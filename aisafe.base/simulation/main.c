/*
 * main.c - Flight Simulation (US101)
 * Architecture aligned with professor's ex1-6.c (one shared pipe, N children).
 */
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "types.h"
#include "ipc.h"
#include "flight_data.h"
#include "flight_process.h"

#define N_FLIGHTS 3

int main(void) {
    pid_t pids[N_FLIGHTS];
    int fd[2];
    int i, status;
    flight_plan_t *plans[N_FLIGHTS];
    flight_history_t histories[MAX_FLIGHTS];
    aircraft_position_t pos;

    printf("=== Flight Simulation (US101) ===\n\n");

    /* create flight plans */
    for (i = 0; i < N_FLIGHTS; i++) {
        plans[i] = create_flight_plan(i);
        if (!plans[i]) {
            fprintf(stderr, "Error: could not create flight plan %d\n", i);
            exit(1);
        }
        printf("Flight loaded: %s\n", plans[i]->identifier);
    }

    /* create ONE shared pipe */
    if (pipe(fd) == -1) { perror("pipe"); exit(1); }

    /* fork one process per flight */
    for (i = 0; i < N_FLIGHTS; i++) {
        pids[i] = fork();
        if (pids[i] == -1) { perror("fork"); exit(1); }
        if (pids[i] == 0) {
            /* child: close unused read end */
            close(fd[0]);
            execute_flight_process(fd[1], plans[i]);
            /* execute_flight_process closes fd[1] and calls exit() */
        }
    }

    /* parent: close write end */
    close(fd[1]);

    memset(histories, 0, sizeof(histories));
    printf("\n=== Live Position Updates ===\n");

    /* read positions until all children close the pipe */
    while (read(fd[0], &pos, sizeof(aircraft_position_t)) > 0) {
        printf("[%s] lat=%.4f lon=%.4f alt=%.0fm spd=%.0fkt hdg=%.1f\n",
               pos.flight_id, pos.latitude, pos.longitude,
               pos.altitude_meters, pos.speed_knots, pos.heading_deg);
        store_position(histories, MAX_FLIGHTS, &pos);
    }
    close(fd[0]);

    print_history(histories, MAX_FLIGHTS);

    /* wait for all children */
    for (i = 0; i < N_FLIGHTS; i++) {
        waitpid(pids[i], &status, 0);
        if (WIFEXITED(status))
            printf("[FLIGHT_%02d] ended with code %d\n", i + 1, WEXITSTATUS(status));
    }

    for (i = 0; i < N_FLIGHTS; i++)
        free_flight_plan(plans[i]);

    return 0;
}
