/*
 * ipc.c - Inter-process communication utilities
 */
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <errno.h>
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
    if (!pos) return -1;
    ssize_t written = write(fd, pos, sizeof(aircraft_position_t));
    if (written < 0) {
        perror("write");
        return -1;
    }
    return (written == sizeof(aircraft_position_t)) ? 0 : -1;
}

int recv_position(int fd, aircraft_position_t *pos) {
    if (!pos) return -1;
    size_t total = 0;
    char *buf = (char *)pos;
    while (total < sizeof(aircraft_position_t)) {
        ssize_t n = read(fd, buf + total, sizeof(aircraft_position_t) - total);
        if (n == 0) return -1;        /* EOF - pipe fechado */
        if (n < 0) {
            if (errno == EINTR) continue;  /* interrompido por sinal, tenta de novo */
            perror("read");
            return -1;
        }
        total += (size_t)n;
    }
    return 0;
}

void close_pipe(pipe_t *p) {
    if (p) {
        if (p->read_fd >= 0) close(p->read_fd);
        if (p->write_fd >= 0) close(p->write_fd);
    }
}

void store_position(flight_history_t *histories, int n_flights,
                    const aircraft_position_t *pos) {
    for (int i = 0; i < n_flights; i++) {
        if (strcmp(histories[i].flight_id, pos->flight_id) == 0) {
            if (histories[i].count < MAX_POSITIONS)
                histories[i].positions[histories[i].count++] = *pos;
            return;
        }
        if (histories[i].flight_id[0] == '\0') {
            strncpy(histories[i].flight_id, pos->flight_id,
                    sizeof(histories[i].flight_id) - 1);
            histories[i].positions[histories[i].count++] = *pos;
            return;
        }
    }
}

void print_history(const flight_history_t *histories, int n_flights) {
    printf("\n=== Position History ===\n");
    for (int i = 0; i < n_flights; i++) {
        if (histories[i].flight_id[0] == '\0') continue;
        printf("Flight %s: %d positions recorded\n",
               histories[i].flight_id, histories[i].count);
        for (int j = 0; j < histories[i].count; j++) {
            const aircraft_position_t *p = &histories[i].positions[j];
            printf("  [%d] lat=%.4f lon=%.4f alt=%.0fm\n",
                   j, p->latitude, p->longitude, p->altitude_meters);
        }
    }
}
