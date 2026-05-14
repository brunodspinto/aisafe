/*
 * ipc.h - Inter-process communication utilities
 */
#ifndef FLIGHT_SIMULATION_IPC_H
#define FLIGHT_SIMULATION_IPC_H

#include "types.h"

void store_position(flight_history_t *histories, int n_flights,
                    const aircraft_position_t *pos);
void print_history(const flight_history_t *histories, int n_flights);

#endif /* FLIGHT_SIMULATION_IPC_H */
