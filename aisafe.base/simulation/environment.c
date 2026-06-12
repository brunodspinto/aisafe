#define _POSIX_C_SOURCE 200809L
#include "environment.h"
#include <math.h>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

#define DEG_TO_RAD (M_PI / 180.0)

void apply_wind_drift(double *lat, double *lon, const segment_t *seg, double dt) {
    if (seg->wind_speed <= 0.0) return;                           /* no wind → no drift */
    double drift_m = seg->wind_speed * dt;                        /* metres this step   */
    double bearing = (seg->wind_direction + 180.0) * DEG_TO_RAD; /* FROM → TO bearing  */
    double north_m = drift_m * cos(bearing);
    double east_m  = drift_m * sin(bearing);
    *lat += north_m / 110574.0;
    *lon += east_m  / (111320.0 * cos(*lat * DEG_TO_RAD));
}
