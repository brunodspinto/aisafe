/*
 * flight_process.c - Flight process execution (second-by-second simulation)
 *
 * Physics: altitude-dependent speed and vertical rate from Flight Profile tables.
 * Synchronisation: after each position write, child blocks until parent sends
 *   'G' (go - safe, continue) or 'S' (stop - collision detected, exit).
 */
#define _POSIX_C_SOURCE 200809L
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <time.h>
#include <math.h>
#include "types.h"
#include "flight_process.h"

#define STEP_SECONDS   1        /* simulation seconds per step (second-by-second) */
#define DEG_TO_RAD     (M_PI / 180.0)
#define KNOTS_TO_MPS   0.51444  /* 1 knot = 0.51444 m/s */

/* Equirectangular distance between two coordinates in meters */
static double horiz_distance_m(double lat1, double lon1, double lat2, double lon2) {
    double lat_mid = (lat1 + lat2) / 2.0 * DEG_TO_RAD;
    double dlat    = (lat2 - lat1) * 110574.0;
    double dlon    = (lon2 - lon1) * 111320.0 * cos(lat_mid);
    return sqrt(dlat * dlat + dlon * dlon);
}

/*
 * Linearly interpolate speed_knots and vertical_rate_mps from a performance
 * table at the given altitude. Clamps to the table boundaries.
 */
static void lookup_perf(const perf_point_t *table, int count, double alt_m,
                        double *speed_kt_out, double *vz_out) {
    if (count <= 0) { *speed_kt_out = 250.0; *vz_out = 0.0; return; }
    if (alt_m <= table[0].altitude_m || count == 1) {
        *speed_kt_out = table[0].speed_knots;
        *vz_out       = table[0].vertical_rate_mps;
        return;
    }
    if (alt_m >= table[count - 1].altitude_m) {
        *speed_kt_out = table[count - 1].speed_knots;
        *vz_out       = table[count - 1].vertical_rate_mps;
        return;
    }
    for (int i = 0; i < count - 1; i++) {
        if (alt_m >= table[i].altitude_m && alt_m < table[i + 1].altitude_m) {
            double t = (alt_m - table[i].altitude_m) /
                       (table[i + 1].altitude_m - table[i].altitude_m);
            *speed_kt_out = table[i].speed_knots +
                            t * (table[i + 1].speed_knots - table[i].speed_knots);
            *vz_out = table[i].vertical_rate_mps +
                      t * (table[i + 1].vertical_rate_mps - table[i].vertical_rate_mps);
            return;
        }
    }
    *speed_kt_out = table[count - 1].speed_knots;
    *vz_out       = table[count - 1].vertical_rate_mps;
}

void execute_flight_process(int pos_write_fd, int ctrl_read_fd,
                            const flight_plan_t *plan) {
    int leg, seg;
    time_t sim_time;
    aircraft_position_t pos;

    if (!plan) {
        fprintf(stderr, "[Flight] Error: flight plan is NULL\n");
        exit(1);
    }

    printf("[Flight %s] Starting simulation\n", plan->identifier);
    sim_time = time(NULL);

    for (leg = 0; leg < plan->leg_count; leg++) {
        const leg_t *leg_data = &plan->legs[leg];
        const flight_profile_t *prof = &leg_data->profile;

        for (seg = 0; seg < leg_data->segment_count; seg++) {
            const segment_t *segment = &leg_data->segments[seg];

            double lat = segment->from.latitude;
            double lon = segment->from.longitude;
            double alt = segment->alt_from_meters;
            double alt_target = segment->alt_to_meters;

            /* Horizontal direction unit vector (in degrees, normalised) */
            double dlat  = segment->to.latitude  - segment->from.latitude;
            double dlon  = segment->to.longitude - segment->from.longitude;
            double dist_total_m = horiz_distance_m(segment->from.latitude,
                                                   segment->from.longitude,
                                                   segment->to.latitude,
                                                   segment->to.longitude);
            if (dist_total_m < 1.0) dist_total_m = 1.0;

            /* Heading: true bearing from segment vector */
            double heading = atan2(dlon, dlat) * 180.0 / M_PI;
            if (heading < 0.0) heading += 360.0;

            int is_climb   = (strcmp(segment->mode, "climb")   == 0);
            int is_cruise  = (strcmp(segment->mode, "cruise")  == 0);
            int is_descend = (strcmp(segment->mode, "descend") == 0);

            double dist_covered_m = 0.0;

            while (1) {
                /* Check segment completion */
                if (is_climb   && alt >= alt_target) break;
                if (is_descend && alt <= alt_target) break;
                if (is_cruise  && dist_covered_m >= dist_total_m) break;

                /* Look up speed and vertical rate at current altitude */
                double speed_kt, vz;
                if (is_cruise) {
                    speed_kt = prof->cruise_speed_knots;
                    vz       = 0.0;
                } else if (is_climb) {
                    lookup_perf(prof->climb, prof->climb_count, alt, &speed_kt, &vz);
                } else { /* descend */
                    lookup_perf(prof->descend, prof->descend_count, alt, &speed_kt, &vz);
                }

                double speed_mps    = speed_kt * KNOTS_TO_MPS;
                double horiz_step_m = speed_mps * STEP_SECONDS;

                /* Advance horizontally along segment direction */
                double lat_mid_rad  = lat * DEG_TO_RAD;
                lat += (dlat / dist_total_m) * (horiz_step_m / 110574.0);
                lon += (dlon / dist_total_m) * (horiz_step_m /
                        (111320.0 * cos(lat_mid_rad)));
                dist_covered_m += horiz_step_m;

                /* Advance vertically */
                alt += vz * STEP_SECONDS;

                /* Clamp to segment altitude bounds */
                if (is_climb   && alt > alt_target) alt = alt_target;
                if (is_descend && alt < alt_target) alt = alt_target;

                /* Build and send position to parent */
                memset(&pos, 0, sizeof(pos));
                pos.latitude        = lat;
                pos.longitude       = lon;
                pos.altitude_meters = alt;
                pos.speed_knots     = speed_kt;
                pos.heading_deg     = heading;
                pos.vz_mps          = vz;
                pos.timestamp       = sim_time;
                strncpy(pos.flight_id, plan->identifier, sizeof(pos.flight_id) - 1);

                write(pos_write_fd, &pos, sizeof(pos));
                sim_time += STEP_SECONDS;

                /* Wait for parent's GO ('G') or STOP ('S') */
                char token = 0;
                if (read(ctrl_read_fd, &token, 1) <= 0 || token == 'S') {
                    close(pos_write_fd);
                    close(ctrl_read_fd);
                    exit(1);
                }
                /* token == 'G': safe to continue */
            }
        }
    }

    close(pos_write_fd);
    close(ctrl_read_fd);
    exit(0);
}
