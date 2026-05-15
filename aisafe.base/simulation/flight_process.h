/*
 * flight_process.h - Flight process execution
 */
#ifndef FLIGHT_SIMULATION_FLIGHT_PROCESS_H
#define FLIGHT_SIMULATION_FLIGHT_PROCESS_H

#include "types.h"

/* pos_write_fd: child writes position updates to parent
 * ctrl_read_fd: child reads 'G' (go) or 'S' (stop) from parent each step */
void execute_flight_process(int pos_write_fd, int ctrl_read_fd,
                            const flight_plan_t *plan);

#endif /* FLIGHT_SIMULATION_FLIGHT_PROCESS_H */

