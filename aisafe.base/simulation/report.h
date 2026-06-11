#ifndef FLIGHT_SIMULATION_REPORT_H
#define FLIGHT_SIMULATION_REPORT_H

#include "types.h"

void reset_live_violation_log(void);
void append_violation_event_to_log(const violation_event_t *event);
void append_violation_drop_notice(int dropped_count);

void generate_final_report(const flight_history_t *histories, int n_flights,
                           const violation_event_t *violation_events,
                           int violation_event_count,
                           int total_violations, int dropped_events,
                           int was_aborted);

#endif /* FLIGHT_SIMULATION_REPORT_H */
