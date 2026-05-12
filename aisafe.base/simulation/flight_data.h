#ifndef FLIGHT_SIMULATION_FLIGHT_DATA_H
#define FLIGHT_SIMULATION_FLIGHT_DATA_H

#include "types.h"

flight_plan_t *create_sample_flight_plan(void);
void free_flight_plan(flight_plan_t *plan);

#endif
