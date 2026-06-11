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
    /* Validate ACA bounding coordinates */
    if (params->aca_south_lat >= params->aca_north_lat) {
        fprintf(stderr, "Error: ACA south latitude must be less than north latitude\n");
        return 0;
    }

    if (params->aca_west_lon >= params->aca_east_lon) {
        fprintf(stderr, "Error: ACA west longitude must be less than east longitude\n");
        return 0;
    }

    /* number of flights must be positive and not exceed MAX_FLIGHTS */
    if (params->n_flights <= 0 || params->n_flights > MAX_FLIGHTS) {
        fprintf(stderr, "Error: n_flights must be between 1 and %d\n", MAX_FLIGHTS);
        return 0;
    }

    if (params->safe_dist_horiz_m < 0.0) {
        fprintf(stderr, "Error: safe_dist_horiz_m must be non-negative\n");
        return 0;
    }

    if (params->safe_dist_vert_m < 0.0) {
        fprintf(stderr, "Error: safe_dist_vert_m must be non-negative\n");
        return 0;
    }

    if (params->max_violations <= 0) {
        fprintf(stderr, "Error: max_violations must be positive\n");
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

    if (strlen(plan->flight_type) == 0 || (strcmp(plan->flight_type, "REGULAR") != 0 && strcmp(plan->flight_type, "CHARTER") != 0)) {
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

