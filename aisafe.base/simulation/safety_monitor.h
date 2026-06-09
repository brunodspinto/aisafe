#ifndef FLIGHT_SIMULATION_SAFETY_MONITOR_H
#define FLIGHT_SIMULATION_SAFETY_MONITOR_H

#include <pthread.h>
#include <sys/types.h>
#include "types.h"
#include "shared_memory.h"

/* Checks collisions using vector physics inside a Safety Cylinder.
 * Returns 1 if the critical violation limit has been reached, 0 otherwise. */
int monitor_safety_violations(
    int updated_flight_idx,
    aircraft_position_t *prev_positions,
    aircraft_position_t *current_positions,
    int *has_position,
    int *pipe_open,
    pid_t *pids,
    int n_flights,
    int *total_violations,
    sim_shm_t *shm,
    pthread_mutex_t *notification_mutex,
    pthread_cond_t *notification_cond,
    double safe_dist_horiz_m,
    double safe_dist_vert_m,
    int max_violations
);

/* Advisory: checks all future segment pairs of two flights for cylinder violation.
 * Returns 1 if a future collision is predicted (logs a warning); 0 otherwise. */
int predict_future_collisions(
    flight_plan_t *const *plans,
    int flight_a, int current_seg_a,
    int flight_b, int current_seg_b,
    double safe_dist_horiz_m,
    double safe_dist_vert_m
);

#endif /* FLIGHT_SIMULATION_SAFETY_MONITOR_H */
