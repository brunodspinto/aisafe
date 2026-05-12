#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include "types.h"
#include "flight_data.h"

flight_plan_t *create_sample_flight_plan(void) {
    flight_plan_t *plan = malloc(sizeof(flight_plan_t));
    if (!plan) return NULL;

    strncpy(plan->identifier, "FLIGHT_01", sizeof(plan->identifier) - 1);
    strncpy(plan->flight_type, "REGULAR", sizeof(plan->flight_type) - 1);
    plan->leg_count = 1;
    plan->legs = malloc(sizeof(leg_t));
    if (!plan->legs) { free(plan); return NULL; }

    leg_t *leg = &plan->legs[0];
    strncpy(leg->departure.airport_code, "OPO", 4);
    strncpy(leg->arrival.airport_code, "LIS", 4);
    leg->departure.datetime = time(NULL);
    leg->arrival.datetime   = time(NULL) + 3600;
    leg->fuel_kg = 5000.0;
    leg->segment_count = 2;
    leg->segments = malloc(2 * sizeof(segment_t));
    if (!leg->segments) { free(plan->legs); free(plan); return NULL; }

    /* Segmento 1: Porto → meio caminho */
    leg->segments[0].from.latitude  = 41.2481;
    leg->segments[0].from.longitude = -8.6814;
    leg->segments[0].to.latitude    = 40.6413;
    leg->segments[0].to.longitude   = -8.8970;
    leg->segments[0].altitude_meters = 10000.0;
    leg->segments[0].wind_speed      = 50.0;
    leg->segments[0].wind_direction  = 270.0;

    /* Segmento 2: meio caminho → Lisboa */
    leg->segments[1].from.latitude  = 40.6413;
    leg->segments[1].from.longitude = -8.8970;
    leg->segments[1].to.latitude    = 38.7742;
    leg->segments[1].to.longitude   = -9.1342;
    leg->segments[1].altitude_meters = 10000.0;
    leg->segments[1].wind_speed      = 40.0;
    leg->segments[1].wind_direction  = 260.0;

    return plan;
}

void free_flight_plan(flight_plan_t *plan) {
    if (!plan) return;
    for (int i = 0; i < plan->leg_count; i++) {
        free(plan->legs[i].segments);
    }
    free(plan->legs);
    free(plan);
}
