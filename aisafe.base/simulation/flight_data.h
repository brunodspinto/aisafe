#ifndef FLIGHT_SIMULATION_FLIGHT_DATA_H
#define FLIGHT_SIMULATION_FLIGHT_DATA_H

#include "types.h"

flight_plan_t *create_flight_plan(int index);
void free_flight_plan(flight_plan_t *plan);

#endif
