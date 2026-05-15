#ifndef FLIGHT_SIMULATION_US102_H
#define FLIGHT_SIMULATION_US102_H

#include <sys/types.h>
#include "types.h"

// Checks collisions using vector physics inside a Safety Cylinder.
// Returns 1 if the critical violation limit has been reached, otherwise returns 0.
int monitor_safety_violations(
    int updated_flight_idx,
    aircraft_position_t *prev_positions,
    aircraft_position_t *current_positions,
    int *has_position,
    int *pipe_open,
    pid_t *pids,
    int n_flights,
    int *total_violations
);

#endif /* FLIGHT_SIMULATION_US102_H */