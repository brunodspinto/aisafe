/*
 * main.c - Flight Simulation (US101)
 * Architecture aligned with professor's ex1-6.c (one shared pipe, N children).
 *
 * AC5: only positions inside the ACA boundary are stored/displayed.
 * AC6: aircraft entry into and exit from the ACA are detected and logged.
 */
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "types.h"
#include "ipc.h"
#include "aca_filter.h"
#include "flight_data.h"
#include "flight_process.h"

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

int main() {
    pid_t pids[N_FLIGHTS];
    int fd[2];
    int i, status, in_aca, idx;
    flight_plan_t *plans[N_FLIGHTS];
    flight_history_t histories[MAX_FLIGHTS];
    aircraft_position_t pos;
    aca_state_t prev;

    printf("=== Flight Simulation (US101) ===\n");
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

    fflush(stdout); /* prevent children from inheriting unflushed buffer */

    if (pipe(fd) == -1) { perror("pipe"); exit(1); }

    for (i = 0; i < N_FLIGHTS; i++) {
        pids[i] = fork();
        if (pids[i] == -1) { perror("fork"); exit(1); }
        if (pids[i] == 0) {
            close(fd[0]);
            execute_flight_process(fd[1], plans[i]);
            /* execute_flight_process closes fd[1] and calls exit() */
        }
    }

    /* parent: close write end */
    close(fd[1]);

    memset(histories, 0, sizeof(histories));
    printf("\n=== Live Position Updates (ACA only) ===\n");

    while (read(fd[0], &pos, sizeof(aircraft_position_t)) > 0) {
        in_aca = is_in_aca(&pos, &ACA);
        idx    = find_or_create_flight(histories, MAX_FLIGHTS, pos.flight_id);

        if (idx < 0) {
            fprintf(stderr, "Warning: too many flights, dropping [%s]\n",
                    pos.flight_id);
            continue;
        }

        prev = histories[idx].aca_state;

        if (in_aca) {
            /* AC6: detect entry */
            if (prev == ACA_BEFORE)
                printf("[%s] >>> ENTERING ACA\n", pos.flight_id);
            histories[idx].aca_state = ACA_INSIDE;

            /* AC5: only store and display positions inside the ACA */
            if (histories[idx].count < MAX_POSITIONS)
                histories[idx].positions[histories[idx].count++] = pos;

            printf("[%s] lat=%.4f lon=%.4f alt=%.0fm spd=%.0fkt hdg=%.1f\n",
                   pos.flight_id, pos.latitude, pos.longitude,
                   pos.altitude_meters, pos.speed_knots, pos.heading_deg);
        } else {
            /* AC6: detect exit */
            if (prev == ACA_INSIDE) {
                printf("[%s] <<< EXITING ACA\n", pos.flight_id);
                histories[idx].aca_state = ACA_AFTER;
            }
            /* if prev == ACA_BEFORE or ACA_AFTER: no state change needed */
        }
    }
    close(fd[0]);

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
