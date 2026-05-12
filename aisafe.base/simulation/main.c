/*
 * main.c - Flight Simulation
 */

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <sys/wait.h>
#include <sys/select.h>
#include "types.h"
#include "ipc.h"
#include "flight_process.h"
#include "flight_data.h"

#define N_FLIGHTS 3

int main(void) {
    printf("=== Flight Simulation (US101) ===\n\n");

    /* 1. Criar planos de voo */
    flight_plan_t *plans[N_FLIGHTS];
    for (int i = 0; i < N_FLIGHTS; i++) {
        plans[i] = create_sample_flight_plan();
        if (!plans[i]) {
            fprintf(stderr, "Error: could not create flight plan %d\n", i);
            return EXIT_FAILURE;
        }
        snprintf(plans[i]->identifier, sizeof(plans[i]->identifier),
                 "FLIGHT_%02d", i + 1);
        printf("Flight loaded: %s\n", plans[i]->identifier);
    }

    /* 2. Criar um pipe por voo */
    pipe_t pipes[N_FLIGHTS];
    pid_t pids[N_FLIGHTS];
    for (int i = 0; i < N_FLIGHTS; i++)
        pipes[i] = create_pipe();

    /* 3. Fork um processo por voo */
    for (int i = 0; i < N_FLIGHTS; i++) {
        pids[i] = fork();
        if (pids[i] < 0) {
            perror("fork");
            return EXIT_FAILURE;
        }
        if (pids[i] == 0) {
            /* FILHO - fecha tudo o que não usa */
            for (int j = 0; j < N_FLIGHTS; j++) {
                close(pipes[j].read_fd);
                if (j != i) close(pipes[j].write_fd);
            }
            execute_flight_process(pipes[i].write_fd, plans[i]);
        }
    }

    /* 4. PAI - fecha todos os write_fd */
    for (int i = 0; i < N_FLIGHTS; i++)
        close(pipes[i].write_fd);

    /* 5. Histórico de posições */
    flight_history_t histories[MAX_FLIGHTS];
    memset(histories, 0, sizeof(histories));

    int pipe_open[N_FLIGHTS];
    int open_pipes = N_FLIGHTS;
    for (int i = 0; i < N_FLIGHTS; i++) pipe_open[i] = 1;

    printf("\n=== Live Position Updates ===\n");
    while (open_pipes > 0) {

        /* select() - espera pelo pipe que tiver dados primeiro */
        fd_set read_fds;
        FD_ZERO(&read_fds);
        int max_fd = -1;
        for (int i = 0; i < N_FLIGHTS; i++) {
            if (!pipe_open[i]) continue;
            FD_SET(pipes[i].read_fd, &read_fds);
            if (pipes[i].read_fd > max_fd)
                max_fd = pipes[i].read_fd;
        }

        if (select(max_fd + 1, &read_fds, NULL, NULL, NULL) < 0) {
            perror("select");
            break;
        }

        /* lê apenas os pipes que têm dados */
        for (int i = 0; i < N_FLIGHTS; i++) {
            if (!pipe_open[i]) continue;
            if (!FD_ISSET(pipes[i].read_fd, &read_fds)) continue;

            aircraft_position_t pos;
            int result = recv_position(pipes[i].read_fd, &pos);

            if (result == 0) {
                printf("[%s] lat=%.4f lon=%.4f alt=%.0fm\n",
                       pos.flight_id, pos.latitude,
                       pos.longitude, pos.altitude_meters);
                store_position(histories, MAX_FLIGHTS, &pos);
            } else {
                close(pipes[i].read_fd);
                pipe_open[i] = 0;
                open_pipes--;
                printf("[FLIGHT_%02d] finished\n", i + 1);
            }
        }
    }

    /* 6. Histórico completo */
    print_history(histories, MAX_FLIGHTS);

    /* 7. Esperar filhos + libertar memória */
    for (int i = 0; i < N_FLIGHTS; i++)
        waitpid(pids[i], NULL, 0);
    for (int i = 0; i < N_FLIGHTS; i++)
        free_flight_plan(plans[i]);

    return EXIT_SUCCESS;
}
