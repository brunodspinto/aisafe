/*
 * validation.h - Parameter validation for flight simulation
 */
#ifndef FLIGHT_SIMULATION_VALIDATION_H
#define FLIGHT_SIMULATION_VALIDATION_H

#include "types.h"

int validate_simulation_params(const simulation_params_t *params);
int validate_flight_plan(const flight_plan_t *plan);
int validate_coordinate(const coordinate_t *coord);

#endif /* FLIGHT_SIMULATION_VALIDATION_H */

