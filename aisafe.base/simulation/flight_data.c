#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include "types.h"
#include "flight_data.h"

/*
 * 3-segment OPO->MAD profile from Flight_Plan_v0c.json.
 * lat_offset shifts departure latitude for each flight variant.
 */
static flight_plan_t *build_opo_mad(const char *id, double lat_offset) {
    flight_plan_t *plan = malloc(sizeof(flight_plan_t));
    if (!plan) return NULL;

    strncpy(plan->identifier, id, sizeof(plan->identifier) - 1);
    plan->identifier[sizeof(plan->identifier) - 1] = '\0';
    strncpy(plan->flight_type, "REGULAR", sizeof(plan->flight_type) - 1);
    plan->leg_count = 1;
    plan->legs = malloc(sizeof(leg_t));
    if (!plan->legs) { free(plan); return NULL; }

    leg_t *leg = &plan->legs[0];
    strncpy(leg->departure.airport_code, "OPO", 4);
    strncpy(leg->arrival.airport_code, "MAD", 4);
    leg->departure.datetime = time(NULL);
    leg->arrival.datetime   = time(NULL) + 7200;
    leg->fuel_kg = 4200.0;
    /* Flight Profile tables from Flight_Plan_v0c.json */
    leg->profile.climb_count = 13;
    leg->profile.climb[0]  = (perf_point_t){ 0,     210, 12.0 };
    leg->profile.climb[1]  = (perf_point_t){ 1000,  210, 11.5 };
    leg->profile.climb[2]  = (perf_point_t){ 2000,  220, 11.0 };
    leg->profile.climb[3]  = (perf_point_t){ 3000,  230, 10.5 };
    leg->profile.climb[4]  = (perf_point_t){ 4000,  250, 10.0 };
    leg->profile.climb[5]  = (perf_point_t){ 5000,  260,  9.5 };
    leg->profile.climb[6]  = (perf_point_t){ 6000,  290,  8.5 };
    leg->profile.climb[7]  = (perf_point_t){ 7000,  290,  8.0 };
    leg->profile.climb[8]  = (perf_point_t){ 8000,  290,  7.5 };
    leg->profile.climb[9]  = (perf_point_t){ 9000,  290,  7.0 };
    leg->profile.climb[10] = (perf_point_t){ 10000, 300,  6.5 };
    leg->profile.climb[11] = (perf_point_t){ 11000, 300,  6.0 };
    leg->profile.climb[12] = (perf_point_t){ 12000, 300,  5.5 };

    leg->profile.descend_count = 13;
    leg->profile.descend[0]  = (perf_point_t){ 0,      140, -5.0  };
    leg->profile.descend[1]  = (perf_point_t){ 1000,   180, -7.0  };
    leg->profile.descend[2]  = (perf_point_t){ 2000,   250, -7.0  };
    leg->profile.descend[3]  = (perf_point_t){ 3000,   250, -8.0  };
    leg->profile.descend[4]  = (perf_point_t){ 4000,   270, -8.0  };
    leg->profile.descend[5]  = (perf_point_t){ 5000,   290, -10.0 };
    leg->profile.descend[6]  = (perf_point_t){ 6000,   300, -10.0 };
    leg->profile.descend[7]  = (perf_point_t){ 7000,   300, -10.0 };
    leg->profile.descend[8]  = (perf_point_t){ 8000,   300, -10.0 };
    leg->profile.descend[9]  = (perf_point_t){ 9000,   300, -10.0 };
    leg->profile.descend[10] = (perf_point_t){ 10000,  300, -10.0 };
    leg->profile.descend[11] = (perf_point_t){ 11000,  300, -10.0 };
    leg->profile.descend[12] = (perf_point_t){ 12000,  300, -10.0 };

    leg->profile.cruise_speed_knots = 460.0;

    leg->segment_count = 3;
    leg->segments = malloc(3 * sizeof(segment_t));
    if (!leg->segments) { free(plan->legs); free(plan); return NULL; }

    /* Segment 0: climb  OPO -> (42.0, -8.01)  69m -> 9249m */
    strncpy(leg->segments[0].mode, "climb", sizeof(leg->segments[0].mode) - 1);
    leg->segments[0].from.latitude  = 41.262891 + lat_offset;
    leg->segments[0].from.longitude = -8.68522;
    leg->segments[0].to.latitude    = 42.0 + lat_offset;
    leg->segments[0].to.longitude   = -8.01;
    leg->segments[0].alt_from_meters = 69.0;
    leg->segments[0].alt_to_meters   = 9249.0;
    leg->segments[0].width_meters    = 5000.0;
    leg->segments[0].wind_speed      = 30.0;
    leg->segments[0].wind_direction  = 270.0;

    /* Segment 1: cruise (42.0, -8.01) -> (41.0, -4.3)  9249m -> 9249m */
    strncpy(leg->segments[1].mode, "cruise", sizeof(leg->segments[1].mode) - 1);
    leg->segments[1].from.latitude  = 42.0 + lat_offset;
    leg->segments[1].from.longitude = -8.01;
    leg->segments[1].to.latitude    = 41.0 + lat_offset;
    leg->segments[1].to.longitude   = -4.3;
    leg->segments[1].alt_from_meters = 9249.0;
    leg->segments[1].alt_to_meters   = 9249.0;
    leg->segments[1].width_meters    = 5000.0;
    leg->segments[1].wind_speed      = 50.0;
    leg->segments[1].wind_direction  = 260.0;

    /* Segment 2: descend (41.0, -4.3) -> MAD (40.4895, -3.5643)  9249m -> 610m */
    strncpy(leg->segments[2].mode, "descend", sizeof(leg->segments[2].mode) - 1);
    leg->segments[2].from.latitude  = 41.0 + lat_offset;
    leg->segments[2].from.longitude = -4.3;
    leg->segments[2].to.latitude    = 40.4895 + lat_offset;
    leg->segments[2].to.longitude   = -3.5643;
    leg->segments[2].alt_from_meters = 9249.0;
    leg->segments[2].alt_to_meters   = 610.0;
    leg->segments[2].width_meters    = 5000.0;
    leg->segments[2].wind_speed      = 20.0;
    leg->segments[2].wind_direction  = 250.0;

    return plan;
}

flight_plan_t *create_flight_plan(int index) {
    switch (index) {
        case 0: return build_opo_mad("FLIGHT_01",  0.0);
        case 1: return build_opo_mad("FLIGHT_02",  0.3);
        case 2: return build_opo_mad("FLIGHT_03", -0.3);
        /* FLIGHT_04: 0.02° north of FLIGHT_01 (~2.2 km) — triggers collision */
        case 3: return build_opo_mad("FLIGHT_04",  0.02);
        default: return NULL;
    }
}

void free_flight_plan(flight_plan_t *plan) {
    int i;
    if (!plan) return;
    for (i = 0; i < plan->leg_count; i++) {
        free(plan->legs[i].segments);
    }
    free(plan->legs);
    free(plan);
}
