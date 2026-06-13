/*
 * weather_service.h - US110 environment data source
 *
 * Simulates the "weather service" the environment thread reads from. There is no
 * real network weather service in this project, so the wind is taken from the
 * AISAFE_WIND="speed,dir" environment variable (speed in m/s, dir in degrees,
 * meteorological FROM convention). If unset/invalid the conditions are calm
 * (0,0), in which case flights fall back to the per-segment wind in the plan.
 */
#ifndef WEATHER_SERVICE_H
#define WEATHER_SERVICE_H

#include "types.h"

/* Fetch the current environment for the given simulation step. */
void weather_service_fetch(environment_t *out, int step);

#endif /* WEATHER_SERVICE_H */
