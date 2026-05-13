/*
 * validation.c - Parameter validation for flight simulation
 */
#include <stdio.h>
#include <string.h>
#include "types.h"
#include "validation.h"

int validate_simulation_params(const simulation_params_t *params) {
    if (!params) {
        fprintf(stderr, "Error: simulation parameters are NULL\n");
        return 0;
    }

    if (params->start_time >= params->end_time) {
        fprintf(stderr, "Error: start_time must be before end_time\n");
        return 0;
    }

    if (params->min_latitude >= params->max_latitude) {
        fprintf(stderr, "Error: min_latitude must be less than max_latitude\n");
        return 0;
    }

    if (params->min_longitude >= params->max_longitude) {
        fprintf(stderr, "Error: min_longitude must be less than max_longitude\n");
        return 0;
    }

    if (params->max_flights <= 0) {
        fprintf(stderr, "Error: max_flights must be positive\n");
        return 0;
    }

    if (params->safety_threshold < 0.0 || params->safety_threshold > 100.0) {
        fprintf(stderr, "Error: safety_threshold must be between 0 and 100\n");
        return 0;
    }

    if (params->performance_threshold < 0.0 || params->performance_threshold > 100.0) {
        fprintf(stderr, "Error: performance_threshold must be between 0 and 100\n");
        return 0;
    }

    return 1;
}

int validate_flight_plan(const flight_plan_t *plan) {
    if (!plan) {
        fprintf(stderr, "Error: flight plan is NULL\n");
        return 0;
    }

    if (strlen(plan->identifier) == 0) {
        fprintf(stderr, "Error: flight identifier cannot be empty\n");
        return 0;
    }

    if (!plan->flight_type || (strcmp(plan->flight_type, "REGULAR") != 0 && strcmp(plan->flight_type, "CHARTER") != 0)) {
        fprintf(stderr, "Error: flight_type must be REGULAR or CHARTER\n");
        return 0;
    }

    if (!plan->legs) {
        fprintf(stderr, "Error: flight legs array is NULL\n");
        return 0;
    }

    if (plan->leg_count <= 0) {
        fprintf(stderr, "Error: flight must have at least one leg\n");
        return 0;
    }

    return 1;
}

int validate_coordinate(const coordinate_t *coord) {
    if (!coord) {
        fprintf(stderr, "Error: coordinate is NULL\n");
        return 0;
    }

    if (coord->latitude < -90.0 || coord->latitude > 90.0) {
        fprintf(stderr, "Error: latitude must be between -90 and 90\n");
        return 0;
    }

    if (coord->longitude < -180.0 || coord->longitude > 180.0) {
        fprintf(stderr, "Error: longitude must be between -180 and 180\n");
        return 0;
    }

    return 1;
}

