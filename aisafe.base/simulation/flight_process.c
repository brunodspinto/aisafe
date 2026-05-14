/*
 * flight_process.c - Flight process execution
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

#define STEP_COUNT 5

void execute_flight_process(int pipe_fd, const flight_plan_t *plan) {
    if (!plan) {
        fprintf(stderr, "[Flight] Error: flight plan is NULL\n");
        exit(EXIT_FAILURE);
    }

    printf("[Flight %s] Executing %d legs\n", plan->identifier, plan->leg_count);

    time_t start_time = time(NULL);

    for (int leg = 0; leg < plan->leg_count; leg++) {
        const leg_t *leg_data = &plan->legs[leg];

        for (int seg = 0; seg < leg_data->segment_count; seg++) {
            const segment_t *segment = &leg_data->segments[seg];

            /* heading: atan2(dlon, dlat) aligned with professor's math */
            double dlat = segment->to.latitude  - segment->from.latitude;
            double dlon = segment->to.longitude - segment->from.longitude;
            double heading = atan2(dlon, dlat) * 180.0 / M_PI;
            if (heading < 0) heading += 360.0;

            /* speed by phase */
            double speed;
            if      (strcmp(segment->mode, "climb")  == 0) speed = 250.0;
            else if (strcmp(segment->mode, "cruise") == 0) speed = 460.0;
            else                                            speed = 220.0;

            /* Interpolate positions along segment */
            for (int step = 0; step < STEP_COUNT; step++) {
                double progress = (double)step / STEP_COUNT;

                aircraft_position_t pos;
                pos.latitude  = segment->from.latitude +
                                (segment->to.latitude  - segment->from.latitude)  * progress;
                pos.longitude = segment->from.longitude +
                                (segment->to.longitude - segment->from.longitude) * progress;
                pos.altitude_meters = segment->alt_from_meters +
                                      (segment->alt_to_meters - segment->alt_from_meters) * progress;
                pos.speed_knots = speed;
                pos.heading_deg = heading;
                pos.timestamp   = start_time + (time_t)(leg * 1000 + seg * 100 + step * 10);
                strncpy(pos.flight_id, plan->identifier, sizeof(pos.flight_id) - 1);
                pos.flight_id[sizeof(pos.flight_id) - 1] = '\0';

                write(pipe_fd, &pos, sizeof(aircraft_position_t));

                struct timespec delay = {0, 50000000L};
                nanosleep(&delay, NULL);
            }
        }
    }

    close(pipe_fd);
    exit(EXIT_SUCCESS);
}
