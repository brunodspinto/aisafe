/*
 * aca_filter.h - Air Control Area boundary checks (US101)
 */
#ifndef ACA_FILTER_H
#define ACA_FILTER_H

#include "types.h"

/* Returns 1 if pos is inside the rectangular ACA boundary, 0 otherwise. */
int is_in_aca(const aircraft_position_t *pos, const geo_boundary_t *aca);

#endif /* ACA_FILTER_H */
