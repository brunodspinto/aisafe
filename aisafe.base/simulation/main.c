/*
 * main.c - Flight Simulation (US101 & US102)
 *
 * Architecture: one pipe per child + select() multiplexing so the parent
 * knows which flight index sent each position (required for vector tracking).
 *
 * US101: ACA boundary filter, entry/exit detection, position history.
 * US102: per-flight motion vectors checked against a safety cylinder.
 */
#define _POSIX_C_SOURCE 200809L
#include <unistd.h>
#include <sys/types.h>
#include <sys/select.h>
#include <sys/wait.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "types.h"
#include "ipc.h"
#include "aca_filter.h"
#include "flight_data.h"
#include "flight_process.h"
#include "us102.h"

#define N_FLIGHTS 3

/*
 * Lisbon FIR (western Iberian Peninsula).
 * OPO is inside; MAD and the final approach are outside, so flights
 * will enter at departure and exit during cruise — demonstrating AC6.
 */
static const geo_boundary_t ACA = {
    .north_latitude =  43.0,
    .south_latitude =  36.0,
    .west_longitude = -10.0,
    .east_longitude =  -6.0
};

typedef struct {
    int read_fd;
    int write_fd;
} pipe_pair_t;

int main(void) {
    pid_t pids[N_FLIGHTS];
    pipe_pair_t pipes[N_FLIGHTS];
    flight_plan_t *plans[N_FLIGHTS];
    flight_history_t histories[MAX_FLIGHTS];
    int i, status;

    printf("=== Flight Simulation (US101 & US102) ===\n");
    printf("ACA: lat [%.1f, %.1f]  lon [%.1f, %.1f]\n\n",
           ACA.south_latitude, ACA.north_latitude,
           ACA.west_longitude, ACA.east_longitude);

    for (i = 0; i < N_FLIGHTS; i++) {
        plans[i] = create_flight_plan(i);
        if (!plans[i]) {
            fprintf(stderr, "Error: could not create flight plan %d\n", i);
            exit(1);
        }
        printf("Flight loaded: %s\n", plans[i]->identifier);
    }

    fflush(stdout);

    /* One pipe per flight so the parent knows the sender index */
    for (i = 0; i < N_FLIGHTS; i++) {
        int fd[2];
        if (pipe(fd) == -1) { perror("pipe"); exit(1); }
        pipes[i].read_fd  = fd[0];
        pipes[i].write_fd = fd[1];
    }

    for (i = 0; i < N_FLIGHTS; i++) {
        pids[i] = fork();
        if (pids[i] == -1) { perror("fork"); exit(1); }
        if (pids[i] == 0) {
            /* Child: close every read end and every write end that isn't ours */
            for (int j = 0; j < N_FLIGHTS; j++) {
                close(pipes[j].read_fd);
                if (j != i) close(pipes[j].write_fd);
            }
            execute_flight_process(pipes[i].write_fd, plans[i]);
            /* execute_flight_process closes write_fd and calls exit() */
        }
    }

    /* Parent: close all write ends */
    for (i = 0; i < N_FLIGHTS; i++)
        close(pipes[i].write_fd);

    memset(histories, 0, sizeof(histories));

    /* US102: per-flight motion vector state */
    aircraft_position_t prev_positions[N_FLIGHTS];
    aircraft_position_t current_positions[N_FLIGHTS];
    int has_position[N_FLIGHTS];
    memset(has_position, 0, sizeof(has_position));

    int total_violations = 0;
    int pipe_open[N_FLIGHTS];
    int open_pipes = N_FLIGHTS;
    for (i = 0; i < N_FLIGHTS; i++) pipe_open[i] = 1;

    int max_fd = -1;
    for (i = 0; i < N_FLIGHTS; i++)
        if (pipes[i].read_fd > max_fd) max_fd = pipes[i].read_fd;

    printf("\n=== Controlador Aereo Live (US101 & US102) ===\n");

    while (open_pipes > 0) {
        fd_set read_fds;
        FD_ZERO(&read_fds);
        for (i = 0; i < N_FLIGHTS; i++)
            if (pipe_open[i]) FD_SET(pipes[i].read_fd, &read_fds);

        if (select(max_fd + 1, &read_fds, NULL, NULL, NULL) < 0) {
            perror("select");
            break;
        }

        for (i = 0; i < N_FLIGHTS; i++) {
            if (!pipe_open[i] || !FD_ISSET(pipes[i].read_fd, &read_fds)) continue;

            aircraft_position_t pos;
            ssize_t n = read(pipes[i].read_fd, &pos, sizeof(pos));

            if (n == (ssize_t)sizeof(pos)) {
                /* US101: ACA filter, entry/exit detection, history */
                int in_aca = is_in_aca(&pos, &ACA);
                int idx    = find_or_create_flight(histories, MAX_FLIGHTS, pos.flight_id);

                if (idx >= 0) {
                    aca_state_t prev_state = histories[idx].aca_state;

                    if (in_aca) {
                        if (prev_state == ACA_BEFORE)
                            printf("[%s] >>> ENTERING ACA\n", pos.flight_id);
                        histories[idx].aca_state = ACA_INSIDE;

                        if (histories[idx].count < MAX_POSITIONS)
                            histories[idx].positions[histories[idx].count++] = pos;

                        printf("[%s] lat=%.4f lon=%.4f alt=%.0fm spd=%.0fkt hdg=%.1f\n",
                               pos.flight_id, pos.latitude, pos.longitude,
                               pos.altitude_meters, pos.speed_knots, pos.heading_deg);
                    } else {
                        if (prev_state == ACA_INSIDE) {
                            printf("[%s] <<< EXITING ACA\n", pos.flight_id);
                            histories[idx].aca_state = ACA_AFTER;
                        }
                    }
                } else {
                    fprintf(stderr, "Warning: too many flights, dropping [%s]\n",
                            pos.flight_id);
                }

                /* US102: slide the motion vector window forward */
                if (has_position[i] > 0)
                    prev_positions[i] = current_positions[i];
                else
                    prev_positions[i] = pos; /* first step: prev == current */

                current_positions[i] = pos;
                has_position[i]++;

                /* Check for cylinder violations only once a real movement vector exists */
                if (has_position[i] >= 2) {
                    int abort_sim = monitor_safety_violations(
                        i, prev_positions, current_positions,
                        has_position, pipe_open, pids,
                        N_FLIGHTS, &total_violations);

                    if (abort_sim) {
                        open_pipes = 0;
                        break; /* exits the for-loop; while condition handles the rest */
                    }
                }

            } else {
                /* EOF or partial read: this flight has finished */
                close(pipes[i].read_fd);
                pipe_open[i] = 0;
                open_pipes--;
                printf("[FLIGHT_%02d] terminou o voo.\n", i + 1);
            }
        }
    }

    print_history(histories, MAX_FLIGHTS);

    for (i = 0; i < N_FLIGHTS; i++) {
        waitpid(pids[i], &status, 0);
        if (WIFEXITED(status))
            printf("[FLIGHT_%02d] ended with code %d\n", i + 1, WEXITSTATUS(status));
    }

    for (i = 0; i < N_FLIGHTS; i++)
        free_flight_plan(plans[i]);

    return 0;
}
