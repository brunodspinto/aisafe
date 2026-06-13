/*
 * weather_service.c - US110 environment data source (see weather_service.h)
 */
#define _POSIX_C_SOURCE 200809L
#include "weather_service.h"
#include <stdio.h>
#include <stdlib.h>

/* The step parameter is accepted so the service can model time-varying weather
 * in the future; the current implementation returns a constant wind read once
 * from the AISAFE_WIND environment variable. */
void weather_service_fetch(environment_t *out, int step) {
    (void)step;
    out->wind_speed     = 0.0;   /* calm by default */
    out->wind_direction = 0.0;

    const char *cfg = getenv("AISAFE_WIND");
    if (cfg != NULL) {
        double spd = 0.0, dir = 0.0;
        /* Expected format: "speed,dir" (e.g. "18,270"). */
        if (sscanf(cfg, "%lf,%lf", &spd, &dir) == 2 && spd > 0.0) {
            out->wind_speed     = spd;
            out->wind_direction = dir;
        }
    }
}
