/*
 * main.c - Flight Simulation (US101 & US102)
 *
 * Bidirectional IPC: one position pipe (child→parent) and one control pipe
 * (parent→child) per flight. After receiving positions from ALL active flights,
 * the parent runs US101 + US102 checks, then sends 'G' (go) or 'S' (stop).
 *
 * This guarantees synchronised time steps: no flight advances past step T+1
 * until the parent has verified safety at step T across every active flight.
 */
#define _POSIX_C_SOURCE 200809L
#include <unistd.h>
#include <sys/types.h>
#include <sys/select.h>
#include <sys/wait.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "types.h"
#include "ipc.h"
#include "aca_filter.h"
#include "flight_data.h"
#include "flight_process.h"
#include "us102.h"

#define N_FLIGHTS_NORMAL    3
#define N_FLIGHTS_COLLISION 4   /* adds FLIGHT_04 (~2km from FLIGHT_01) */

/*
 * Lisbon FIR (western Iberian Peninsula).
 * OPO is inside; MAD and the final approach are outside — flights
 * will enter at departure and exit during cruise, demonstrating AC6.
 */
static const geo_boundary_t ACA = {
    .north_latitude =  43.0,
    .south_latitude =  36.0,
    .west_longitude = -10.0,
    .east_longitude =  -6.0
};

typedef struct {
    int pos_read_fd;   /* parent reads positions from child */
    int pos_write_fd;  /* child writes positions to parent  */
    int ctrl_write_fd; /* parent writes 'G'/'S' to child   */
    int ctrl_read_fd;  /* child reads  'G'/'S' from parent */
} flight_pipes_t;

int main(int argc, char *argv[]) {
    int n_flights = N_FLIGHTS_NORMAL;
    for (int a = 1; a < argc; a++) {
        if (strcmp(argv[a], "--collision") == 0)
            n_flights = N_FLIGHTS_COLLISION;
    }

    /* Ignore SIGPIPE: when us102 sends SIGTERM to children before main sends 'S',
     * writing to the dead child's ctrl pipe returns EPIPE instead of killing the parent. */
    signal(SIGPIPE, SIG_IGN);

    pid_t          pids[N_FLIGHTS_COLLISION];
    flight_pipes_t pipes[N_FLIGHTS_COLLISION];
    flight_plan_t *plans[N_FLIGHTS_COLLISION];
    flight_history_t histories[MAX_FLIGHTS];
    int i, status;

    printf("=== Flight Simulation (US101 & US102) ===\n");
    printf("Mode: %s\n", n_flights == N_FLIGHTS_COLLISION ? "COLLISION TEST" : "normal");
    printf("ACA: lat [%.1f, %.1f]  lon [%.1f, %.1f]\n\n",
           ACA.south_latitude, ACA.north_latitude,
           ACA.west_longitude, ACA.east_longitude);

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

    /* US102: per-flight motion vector state */
    aircraft_position_t prev_positions[n_flights];
    aircraft_position_t current_positions[n_flights];
    int has_position[n_flights];
    memset(has_position, 0, sizeof(has_position));

    int total_violations = 0;
    int active[n_flights];
    int n_active = n_flights;
    for (i = 0; i < n_flights; i++) active[i] = 1;

    /* prediction_done[i][j]: future collision advisory already printed for pair (i,j) */
    int prediction_done[N_FLIGHTS_COLLISION][N_FLIGHTS_COLLISION];
    memset(prediction_done, 0, sizeof(prediction_done));

    /* received[i]: has flight i sent its position for the current step? */
    int received[n_flights];
    memset(received, 0, sizeof(received));

    int max_fd = -1;
    for (i = 0; i < n_flights; i++)
        if (pipes[i].pos_read_fd > max_fd) max_fd = pipes[i].pos_read_fd;

    printf("\n=== Controlador Aereo Live (US101 & US102) ===\n");

    /* Main loop — one select() call per iteration (professor's blocking-read pattern,
     * adapted for N file descriptors). Each iteration blocks until at least one
     * active flight writes its position. The GO/STOP decision is only taken once
     * every active flight has sent its position for the current step. */
    while (n_active > 0) {
        fd_set read_fds;
        FD_ZERO(&read_fds);
        for (i = 0; i < n_flights; i++)
            if (active[i] && !received[i])
                FD_SET(pipes[i].pos_read_fd, &read_fds);

        if (select(max_fd + 1, &read_fds, NULL, NULL, NULL) < 0) {
            perror("select");
            goto done;
        }

        /* Process every file descriptor that is ready */
        for (i = 0; i < n_flights; i++) {
            if (!active[i] || received[i]) continue;
            if (!FD_ISSET(pipes[i].pos_read_fd, &read_fds)) continue;

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
                received[i] = 1;

            } else {
                /* EOF: this flight has finished */
                close(pipes[i].pos_read_fd);
                close(pipes[i].ctrl_write_fd);
                active[i] = 0;
                n_active--;
                printf("[%s] terminou o voo.\n", plans[i]->identifier);
            }
        }

        /* Wait until every still-active flight has sent its position */
        int all_received = 1;
        for (i = 0; i < n_flights; i++)
            if (active[i] && !received[i]) { all_received = 0; break; }

        if (!all_received || n_active == 0) continue;

        /* All active flights reported — run US102 and decide GO/STOP */

        /* AC6: future segment prediction advisory (once per pair, log only) */
        for (i = 0; i < n_flights; i++) {
            if (!active[i] || has_position[i] < 1) continue;
            for (int j = i + 1; j < n_flights; j++) {
                if (!active[j] || has_position[j] < 1) continue;
                if (!prediction_done[i][j]) {
                    prediction_done[i][j] = 1;
                    predict_future_collisions(
                        (flight_plan_t *const *)plans, i, 0, j, 0);
                }
            }
        }

        int abort_sim = 0;
        for (i = 0; i < n_flights; i++) {
            if (!active[i] || has_position[i] < 2) continue;
            if (monitor_safety_violations(i, prev_positions, current_positions,
                                          has_position, active, pids,
                                          n_flights, &total_violations)) {
                abort_sim = 1;
                break;
            }
        }

        if (abort_sim) {
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

        /* Reset received flags for next step */
        memset(received, 0, sizeof(received));
    }

done:
    print_history(histories, MAX_FLIGHTS);

    for (i = 0; i < n_flights; i++) {
        waitpid(pids[i], &status, 0);
        if (WIFEXITED(status))
            printf("[FLIGHT_%02d] ended with code %d\n", i + 1, WEXITSTATUS(status));
    }

    for (i = 0; i < n_flights; i++)
        free_flight_plan(plans[i]);

    return 0;
}
