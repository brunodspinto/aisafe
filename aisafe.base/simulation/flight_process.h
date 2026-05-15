/*
 * flight_process.h - Flight process execution
 */
#ifndef FLIGHT_SIMULATION_FLIGHT_PROCESS_H
#define FLIGHT_SIMULATION_FLIGHT_PROCESS_H

#include "types.h"

void execute_flight_process(int pipe_fd, const flight_plan_t *plan);

#endif /* FLIGHT_SIMULATION_FLIGHT_PROCESS_H */

