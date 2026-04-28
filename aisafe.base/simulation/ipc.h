/*
 * ipc.h - Inter-process communication utilities
 */
#ifndef FLIGHT_SIMULATION_IPC_H
#define FLIGHT_SIMULATION_IPC_H

#include "types.h"

typedef struct {
    int read_fd;
    int write_fd;
} pipe_t;

pipe_t create_pipe(void);
int send_position(int fd, const aircraft_position_t *pos);
int recv_position(int fd, aircraft_position_t *pos);
void close_pipe(pipe_t *p);

#endif /* FLIGHT_SIMULATION_IPC_H */

