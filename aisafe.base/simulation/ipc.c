/*
 * ipc.c - Inter-process communication utilities
 */
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include "ipc.h"

pipe_t create_pipe(void) {
    pipe_t p;
    int fds[2];

    if (pipe(fds) == -1) {
        perror("pipe");
        exit(EXIT_FAILURE);
    }

    p.read_fd = fds[0];
    p.write_fd = fds[1];
    return p;
}

int send_position(int fd, const aircraft_position_t *pos) {
    if (!pos) {
        return -1;
    }

    ssize_t written = write(fd, pos, sizeof(aircraft_position_t));
    if (written < 0) {
        perror("write");
        return -1;
    }

    return (written == sizeof(aircraft_position_t)) ? 0 : -1;
}

int recv_position(int fd, aircraft_position_t *pos) {
    if (!pos) {
        return -1;
    }

    ssize_t read_bytes = read(fd, pos, sizeof(aircraft_position_t));
    if (read_bytes < 0) {
        perror("read");
        return -1;
    }

    return (read_bytes == sizeof(aircraft_position_t)) ? 0 : -1;
}

void close_pipe(pipe_t *p) {
    if (p) {
        if (p->read_fd >= 0) {
            close(p->read_fd);
        }
        if (p->write_fd >= 0) {
            close(p->write_fd);
        }
    }
}

