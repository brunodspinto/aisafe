#ifndef FLIGHT_SIMULATION_REPORT_H
#define FLIGHT_SIMULATION_REPORT_H

#include "types.h"

void generate_final_report(const flight_history_t *histories, int n_flights,
                           int total_violations, int was_aborted);

#endif /* FLIGHT_SIMULATION_REPORT_H */
