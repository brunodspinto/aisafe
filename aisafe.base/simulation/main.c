/*
 * main.c - Flight Simulation
 */

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <sys/wait.h>

#include "types.h"
#include "ipc.h"
#include "flight_process.h"
#include "flight_data.h"

int main(void) {
    printf("=== Flight Simulation (US100) ===\n\n");

    /* Load flight plan */
    flight_plan_t *plan = create_sample_flight_plan();
    if (!plan) {
        fprintf(stderr, "Error: could not create flight plan\n");
        return EXIT_FAILURE;
    }

    printf("Flight loaded: %s (%s)\n", plan->identifier, plan->flight_type);
    printf("Legs: %d\n\n", plan->leg_count);

    /* Create pipe for communication */
    pipe_t p = create_pipe();

    /* Fork flight process */
    pid_t pid = fork();
    if (pid < 0) {
        perror("fork");
        return EXIT_FAILURE;
    }

    if (pid == 0) {
        /* Child process: execute flight */
        close(p.read_fd);
        execute_flight_process(p.write_fd, plan);
        /* never returns */
    } else {
        /* Parent process: receive live position updates */
        close(p.write_fd);

        printf("=== Position Updates ===\n");

        int pos_count = 0;

        while (1) {
            aircraft_position_t pos;
            int result = recv_position(p.read_fd, &pos);

            if (result < 0) {
                break;  /* Pipe closed or error */
            }

            if (result == 0) {
                pos_count++;
                printf("[%s] %.2f, %.2f @ %.0f m\n",
                       pos.flight_id, pos.latitude, pos.longitude, pos.altitude_meters);
            }
        }

        /* Wait for child to complete */
        int status;
        waitpid(pid, &status, 0);

        /* Summary */
        printf("\n=== Summary ===\n");
        printf("Total position updates: %d\n", pos_count);
        printf("Flight: %s completed successfully\n", plan->identifier);

        close_pipe(&p);
        free_flight_plan(plan);
    }

    return EXIT_SUCCESS;
}

