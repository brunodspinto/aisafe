/*
 * flight_process.c - Flight process execution (AC7: second-by-second simulation)
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

/*
 * Each simulation step represents STEP_SECONDS of flight time.
 * Steps are derived from real physics (distance / speed), so longer
 * segments at higher speeds produce proportionally more steps.
 * The 10 ms real-time sleep keeps the demo fast while preserving
 * the second-by-second model in simulation timestamps.
 */
#define STEP_SECONDS   60       /* simulation seconds per step */
#define STEP_DELAY_MS  10       /* real-time delay per step (ms) */

#define DEG_TO_RAD  (M_PI / 180.0)
#define KNOTS_TO_MPS 0.51444    /* 1 knot = 0.51444 m/s */

/* Equirectangular distance approximation (sufficient for short segments). */
static double segment_distance_m(const segment_t *seg) {
    double lat_mid, dlat, dlon;
    lat_mid = (seg->from.latitude + seg->to.latitude) / 2.0 * DEG_TO_RAD;
    dlat    = (seg->to.latitude  - seg->from.latitude)  * 111320.0;
    dlon    = (seg->to.longitude - seg->from.longitude) * 111320.0 * cos(lat_mid);
    return sqrt(dlat * dlat + dlon * dlon);
}

/* Number of simulation steps for a segment given the aircraft speed. */
static int compute_steps(const segment_t *seg, double speed_knots) {
    double dist_m, speed_mps;
    int steps;
    dist_m    = segment_distance_m(seg);
    speed_mps = speed_knots * KNOTS_TO_MPS;
    steps     = (int)(dist_m / (speed_mps * STEP_SECONDS));
    return (steps < 2) ? 2 : steps;
}

void execute_flight_process(int pipe_fd, const flight_plan_t *plan) {
    int leg, seg, step, steps;
    double dlat, dlon, heading, speed, progress;
    time_t sim_time;
    aircraft_position_t pos;
    const leg_t *leg_data;
    const segment_t *segment;
    struct timespec delay;

    if (!plan) {
        fprintf(stderr, "[Flight] Error: flight plan is NULL\n");
        exit(1);
    }

    printf("[Flight %s] Executing %d leg(s)\n", plan->identifier, plan->leg_count);

    sim_time = time(NULL); /* simulation clock starts at fork time */

    for (leg = 0; leg < plan->leg_count; leg++) {
        leg_data = &plan->legs[leg];

        for (seg = 0; seg < leg_data->segment_count; seg++) {
            segment = &leg_data->segments[seg];

            /* Heading: true bearing derived from segment vector */
            dlat    = segment->to.latitude  - segment->from.latitude;
            dlon    = segment->to.longitude - segment->from.longitude;
            heading = atan2(dlon, dlat) * 180.0 / M_PI;
            if (heading < 0.0) heading += 360.0;

            /* Speed constant within each phase */
            if      (strcmp(segment->mode, "climb")  == 0) speed = 250.0;
            else if (strcmp(segment->mode, "cruise") == 0) speed = 460.0;
            else                                             speed = 220.0;

            /* AC7: step count derived from real distance and speed */
            steps = compute_steps(segment, speed);

            for (step = 0; step < steps; step++) {
                progress = (double)step / steps;

                pos.latitude  = segment->from.latitude  +
                                (segment->to.latitude  - segment->from.latitude)  * progress;
                pos.longitude = segment->from.longitude +
                                (segment->to.longitude - segment->from.longitude) * progress;
                pos.altitude_meters = segment->alt_from_meters +
                                      (segment->alt_to_meters - segment->alt_from_meters) * progress;
                pos.speed_knots = speed;
                pos.heading_deg = heading;
                pos.timestamp   = sim_time;
                strncpy(pos.flight_id, plan->identifier, sizeof(pos.flight_id) - 1);
                pos.flight_id[sizeof(pos.flight_id) - 1] = '\0';

                write(pipe_fd, &pos, sizeof(aircraft_position_t));

                sim_time += STEP_SECONDS;

                delay.tv_sec  = 0;
                delay.tv_nsec = STEP_DELAY_MS * 1000000L;
                nanosleep(&delay, NULL);
            }
        }
    }

    /* close write end and exit — parent will detect EOF on its read loop */
    close(pipe_fd);
    exit(0);
}
