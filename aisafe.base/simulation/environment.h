#ifndef ENVIRONMENT_H
#define ENVIRONMENT_H

#include "types.h"

/* US110: apply lateral wind drift to (lat, lon) for one simulation step dt (s).
 * wind_dir_deg is meteorological (direction wind blows FROM); aircraft drifts
 * toward (wind_dir + 180°).  Affects lat/lon only — never dist_covered_m or
 * alt — so every segment remains finite (termination invariant). */
void apply_wind_drift(double *lat, double *lon, const segment_t *seg, double dt);

/* US110: same drift maths from explicit wind values, used when the wind comes
 * from the environment thread (shared memory) rather than the segment. */
void apply_wind_drift_values(double *lat, double *lon,
                             double wind_speed, double wind_direction, double dt);

#endif /* ENVIRONMENT_H */
