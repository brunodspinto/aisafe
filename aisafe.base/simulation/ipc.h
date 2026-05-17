/*
 * ipc.h - Inter-process communication utilities
 */
#ifndef FLIGHT_SIMULATION_IPC_H
#define FLIGHT_SIMULATION_IPC_H

#include "types.h"

/* Returns the index of an existing entry for flight_id, or creates a new one.
   Returns -1 if the histories array is full. */
int find_or_create_flight(flight_history_t *histories, int n_flights,
                          const char *flight_id);

void print_history(const flight_history_t *histories, int n_flights);

#endif /* FLIGHT_SIMULATION_IPC_H */
