/*
 * main.c - Flight Simulation entry point (US100)
 *
 * Bidirectional IPC: one position pipe (child->parent) and one control pipe
 * (parent->child) per flight. The parent reads from each active flight in
 * sequence (blocking read, TP5 pattern), then runs US101 + US102 checks
 * and sends 'G' (go) or 'S' (stop) to every active flight.
 *
 * This guarantees synchronised time steps: no flight advances past step T+1
 * until the parent has verified safety at step T across every active flight.
 */
#define _POSIX_C_SOURCE 200809L
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "types.h"
#include "config.h"
#include "ipc.h"
#include "aca_filter.h"
#include "flight_parser.h"
#include "flight_process.h"
#include "safety_monitor.h"
#include "report.h"

#define FLIGHT_PLANS_FILE "flight_plans.json"
#define CONFIG_FILE "simulation.conf"

typedef struct {
    int pos_read_fd;   /* parent reads positions from child */
    int pos_write_fd;  /* child writes positions to parent  */
    int ctrl_write_fd; /* parent writes 'G'/'S' to child   */
    int ctrl_read_fd;  /* child reads  'G'/'S' from parent */
} flight_pipes_t;

int main(int argc, char *argv[]) {
    /* Load and validate simulation parameters from config file */
    simulation_params_t params;
    int cfg_result = load_config(CONFIG_FILE, &params);
    if (cfg_result == -1) {
        fprintf(stderr, "Error: cannot open config file '%s'\n", CONFIG_FILE);
        return 1;
    }
    if (cfg_result > 0) {
        fprintf(stderr, "Error: parse error in '%s' at line %d\n", CONFIG_FILE, cfg_result);
        return 1;
    }
    if (validate_config(&params) != 0)
        return 1;

    int n_flights = params.n_flights;
    for (int a = 1; a < argc; a++) {
        if (strcmp(argv[a], "--collision") == 0)
            n_flights = N_FLIGHTS_COLLISION;
    }

    signal(SIGPIPE, SIG_IGN);

    pid_t          pids[MAX_FLIGHTS];
    flight_pipes_t pipes[MAX_FLIGHTS];
    flight_plan_t *plans[MAX_FLIGHTS];
    flight_history_t histories[MAX_FLIGHTS];
    int i, status;

    geo_boundary_t ACA = {
        .north_latitude = params.aca_north_lat,
        .south_latitude = params.aca_south_lat,
        .west_longitude = params.aca_west_lon,
        .east_longitude = params.aca_east_lon
    };

    printf("=== AISafe Flight Simulation ===\n");
    printf("Mode: %s\n", n_flights == N_FLIGHTS_COLLISION ? "COLLISION TEST" : "normal");
    print_config(&params);

    for (i = 0; i < n_flights; i++) {
        plans[i] = create_flight_plan(i);
        if (!plans[i]) {
            fprintf(stderr, "Error: could not create flight plan %d\n", i);
            exit(1);
        }
        printf("Flight loaded: %s\n", plans[i]->identifier);
    }
    fflush(stdout);

    /* Two pipes per flight: one for position (child→parent), one for control (parent→child) */
    for (i = 0; i < n_flights; i++) {
        int fd[2];
        if (pipe(fd) == -1) { perror("pipe pos"); exit(1); }
        pipes[i].pos_read_fd  = fd[0];
        pipes[i].pos_write_fd = fd[1];

        if (pipe(fd) == -1) { perror("pipe ctrl"); exit(1); }
        pipes[i].ctrl_write_fd = fd[1];
        pipes[i].ctrl_read_fd  = fd[0];
    }

    for (i = 0; i < n_flights; i++) {
        pids[i] = fork();
        if (pids[i] == -1) { perror("fork"); exit(1); }
        if (pids[i] == 0) {
            /* Child: keep only pos_write_fd[i] and ctrl_read_fd[i] */
            for (int j = 0; j < n_flights; j++) {
                close(pipes[j].pos_read_fd);
                close(pipes[j].ctrl_write_fd);
                if (j != i) {
                    close(pipes[j].pos_write_fd);
                    close(pipes[j].ctrl_read_fd);
                }
            }
            execute_flight_process(pipes[i].pos_write_fd,
                                   pipes[i].ctrl_read_fd,
                                   plans[i]);
            /* never reached */
        }
    }

    /* Parent: close child-side ends */
    for (i = 0; i < n_flights; i++) {
        close(pipes[i].pos_write_fd);
        close(pipes[i].ctrl_read_fd);
    }

    memset(histories, 0, sizeof(histories));
    for (i = 0; i < MAX_FLIGHTS; i++)
        histories[i].aca_state = ACA_BEFORE;

    /* US102: per-flight motion vector state */
    aircraft_position_t prev_positions[n_flights];
    aircraft_position_t current_positions[n_flights];
    int has_position[n_flights];
    memset(has_position, 0, sizeof(has_position));

    int total_violations = 0;
    int sim_aborted = 0;
    int active[n_flights];
    int n_active = n_flights;
    for (i = 0; i < n_flights; i++) active[i] = 1;

    /* prediction_done[i][j]: future collision advisory already printed for pair (i,j) */
    int prediction_done[MAX_FLIGHTS][MAX_FLIGHTS];
    memset(prediction_done, 0, sizeof(prediction_done));

    printf("\n=== Air Traffic Control Live Feed ===\n");

    /* Each iteration reads one position from every active flight in order,
     * then runs safety checks and sends 'G' or 'S' to all active flights. */
    while (n_active > 0) {
        /* Read one position from each active flight (blocks until data arrives) */
        for (i = 0; i < n_flights; i++) {
            if (!active[i]) continue;

            aircraft_position_t pos;
            ssize_t n = read(pipes[i].pos_read_fd, &pos, sizeof(pos));

            if (n == (ssize_t)sizeof(pos)) {
                /* US101: ACA filter, entry/exit detection, history */
                int in_aca = is_in_aca(&pos, &ACA);
                int idx    = find_or_create_flight(histories, MAX_FLIGHTS,
                                                   pos.flight_id);
                if (idx >= 0) {
                    aca_state_t prev_state = histories[idx].aca_state;

                    if (in_aca) {
                        if (prev_state == ACA_BEFORE)
                            printf("[%s] >>> ENTERING ACA\n", pos.flight_id);
                        histories[idx].aca_state = ACA_INSIDE;

                        if (histories[idx].count < MAX_POSITIONS)
                            histories[idx].positions[histories[idx].count++] = pos;

                        printf("[%s] lat=%.4f lon=%.4f alt=%.0fm spd=%.0fkt"
                               " hdg=%.1f vz=%.1fm/s\n",
                               pos.flight_id, pos.latitude, pos.longitude,
                               pos.altitude_meters, pos.speed_knots,
                               pos.heading_deg, pos.vz_mps);
                    } else {
                        if (prev_state == ACA_INSIDE) {
                            printf("[%s] <<< EXITING ACA\n", pos.flight_id);
                            histories[idx].aca_state = ACA_AFTER;
                        }
                    }
                }

                /* US102: slide motion vector window */
                if (has_position[i] > 0)
                    prev_positions[i] = current_positions[i];
                else
                    prev_positions[i] = pos;

                current_positions[i] = pos;
                has_position[i]++;

            } else {
                /* EOF: this flight has finished */
                close(pipes[i].pos_read_fd);
                close(pipes[i].ctrl_write_fd);
                active[i] = 0;
                n_active--;
                printf("[%s] flight completed.\n", plans[i]->identifier);
            }
        }

        if (n_active == 0) break;

        /* All active flights reported — run US102 and decide GO/STOP */

        /* Future segment prediction advisory (once per pair, log only) */
        for (i = 0; i < n_flights; i++) {
            if (!active[i] || has_position[i] < 1) continue;
            for (int j = i + 1; j < n_flights; j++) {
                if (!active[j] || has_position[j] < 1) continue;
                if (!prediction_done[i][j]) {
                    prediction_done[i][j] = 1;
                    predict_future_collisions(
                        (flight_plan_t *const *)plans, i, 0, j, 0,
                        params.safe_dist_horiz_m, params.safe_dist_vert_m);
                }
            }
        }

        int abort_sim = 0;
        for (i = 0; i < n_flights; i++) {
            if (!active[i] || has_position[i] < 2) continue;
            if (monitor_safety_violations(i, prev_positions, current_positions,
                                          has_position, active, pids,
                                          n_flights, &total_violations,
                                          params.safe_dist_horiz_m,
                                          params.safe_dist_vert_m,
                                          params.max_violations)) {
                abort_sim = 1;
                break;
            }
        }

        if (abort_sim) {
            sim_aborted = 1;
            /* Send STOP to all active flights */
            for (i = 0; i < n_flights; i++) {
                if (active[i]) {
                    char s = 'S';
                    write(pipes[i].ctrl_write_fd, &s, 1);
                    close(pipes[i].ctrl_write_fd);
                    active[i] = 0;
                }
            }
            n_active = 0;
            break;
        }

        /* Safe: send GO to all active flights */
        for (i = 0; i < n_flights; i++) {
            if (active[i]) {
                char g = 'G';
                write(pipes[i].ctrl_write_fd, &g, 1);
            }
        }
    }

    print_history(histories, MAX_FLIGHTS);

    for (i = 0; i < n_flights; i++) {
        waitpid(pids[i], &status, 0);
        if (WIFEXITED(status))
            printf("[FLIGHT_%02d] ended with code %d\n", i + 1, WEXITSTATUS(status));
    }

    /* US109: spawn dedicated child process to write the final report */
    printf("\n[SYSTEM] Simulation concluded. Spawning report generation process...\n");
    generate_final_report(histories, n_flights, total_violations, sim_aborted);

    for (i = 0; i < n_flights; i++)
        free_flight_plan(plans[i]);

    return 0;
}
