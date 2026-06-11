#include <stdio.h>
#include <stdlib.h>
#include <signal.h>
#include <sys/types.h>
#include <unistd.h>
#include <math.h>
#include <string.h>
#include <time.h>
#include "safety_monitor.h"


int kill(pid_t pid, int sig);

#define SUB_STEPS 10  /* micro-steps for trajectory intersection (prevents position jumps) */

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

// Converts geographic degrees into meters and calculates cylinder component distances
static void calculate_cylinder_distances(const aircraft_position_t *p1, const aircraft_position_t *p2, double *dist_horiz, double *dist_vert) {
    double lat_mid = (p1->latitude + p2->latitude) / 2.0;
    double dx = (p1->longitude - p2->longitude) * 111320.0 * cos(lat_mid * M_PI / 180.0);
    double dy = (p1->latitude - p2->latitude) * 110574.0;

    *dist_horiz = sqrt(dx * dx + dy * dy);
    *dist_vert = fabs(p1->altitude_meters - p2->altitude_meters);
}

static int check_trajectory_intersection(const aircraft_position_t *p1_prev, const aircraft_position_t *p1_curr,
                                         const aircraft_position_t *p2_prev, const aircraft_position_t *p2_curr,
                                         double safe_dist_horiz_m, double safe_dist_vert_m) {
    for (int step = 0; step <= SUB_STEPS; step++) {
        double t = (double)step / SUB_STEPS;

        aircraft_position_t p1_inter, p2_inter;
        p1_inter.latitude        = p1_prev->latitude        + (p1_curr->latitude        - p1_prev->latitude)        * t;
        p1_inter.longitude       = p1_prev->longitude       + (p1_curr->longitude       - p1_prev->longitude)       * t;
        p1_inter.altitude_meters = p1_prev->altitude_meters + (p1_curr->altitude_meters - p1_prev->altitude_meters) * t;

        p2_inter.latitude        = p2_prev->latitude        + (p2_curr->latitude        - p2_prev->latitude)        * t;
        p2_inter.longitude       = p2_prev->longitude       + (p2_curr->longitude       - p2_prev->longitude)       * t;
        p2_inter.altitude_meters = p2_prev->altitude_meters + (p2_curr->altitude_meters - p2_prev->altitude_meters) * t;

        double d_horiz, d_vert;
        calculate_cylinder_distances(&p1_inter, &p2_inter, &d_horiz, &d_vert);

        if (d_horiz < safe_dist_horiz_m && d_vert < safe_dist_vert_m)
            return 1;
    }
    return 0;
}

int predict_future_collisions(flight_plan_t *const *plans,
                              int flight_a, int current_seg_a,
                              int flight_b, int current_seg_b,
                              double safe_dist_horiz_m, double safe_dist_vert_m) {
    const flight_plan_t *pa = plans[flight_a];
    const flight_plan_t *pb = plans[flight_b];
    if (!pa || !pb || pa->leg_count == 0 || pb->leg_count == 0) return 0;

    const leg_t *la = &pa->legs[0];
    const leg_t *lb = &pb->legs[0];

    for (int sa = current_seg_a; sa < la->segment_count; sa++) {
        aircraft_position_t a_prev, a_curr;
        a_prev.latitude        = la->segments[sa].from.latitude;
        a_prev.longitude       = la->segments[sa].from.longitude;
        a_prev.altitude_meters = la->segments[sa].alt_from_meters;
        a_curr.latitude        = la->segments[sa].to.latitude;
        a_curr.longitude       = la->segments[sa].to.longitude;
        a_curr.altitude_meters = la->segments[sa].alt_to_meters;

        for (int sb = current_seg_b; sb < lb->segment_count; sb++) {
            aircraft_position_t b_prev, b_curr;
            b_prev.latitude        = lb->segments[sb].from.latitude;
            b_prev.longitude       = lb->segments[sb].from.longitude;
            b_prev.altitude_meters = lb->segments[sb].alt_from_meters;
            b_curr.latitude        = lb->segments[sb].to.latitude;
            b_curr.longitude       = lb->segments[sb].to.longitude;
            b_curr.altitude_meters = lb->segments[sb].alt_to_meters;

            if (check_trajectory_intersection(&a_prev, &a_curr, &b_prev, &b_curr,
                                              safe_dist_horiz_m, safe_dist_vert_m)) {
                printf("[PREDICTION] Future collision risk: %s seg %d"
                       " vs %s seg %d\n",
                       pa->identifier, sa, pb->identifier, sb);
                return 1;
            }
        }
    }
    return 0;
}

int monitor_safety_violations(int updated_flight_idx, aircraft_position_t *prev_positions,
                               aircraft_position_t *current_positions, int *has_position,
                               int *pipe_open, pid_t *pids, int n_flights, int *total_violations,
                               sim_shm_t *shm, pthread_mutex_t *notification_mutex,
                               pthread_cond_t *notification_cond, double safe_dist_horiz_m,
                               double safe_dist_vert_m, int max_violations) {
    int i = updated_flight_idx;

    for (int j = i + 1; j < n_flights; j++) {
        if (has_position[j] < 2 || !pipe_open[j]) continue;

        double d_horiz, d_vert;
        calculate_cylinder_distances(&current_positions[i], &current_positions[j],
                                     &d_horiz, &d_vert);

        if (check_trajectory_intersection(&prev_positions[i], &current_positions[i],
                                          &prev_positions[j], &current_positions[j],
                                          safe_dist_horiz_m, safe_dist_vert_m)) {
            time_t now = time(NULL);
            printf("\n[CYLINDER ALERT] Intersection risk detected at %s", ctime(&now));
            printf("Flights: %s and %s crossed paths (H < %.0fm and V < %.0fm).\n",
                   current_positions[i].flight_id, current_positions[j].flight_id,
                   safe_dist_horiz_m, safe_dist_vert_m);

            kill(pids[i], SIGUSR1);
            kill(pids[j], SIGUSR1);

            (*total_violations)++;

            pthread_mutex_lock(notification_mutex);
            shm->total_violations = *total_violations;
            if (shm->violation_event_count < MAX_VIOLATION_EVENTS) {
                violation_event_t *event =
                    &shm->violation_events[shm->violation_event_count++];
                event->timestamp = now;
                snprintf(event->flight_a, sizeof(event->flight_a), "%s",
                         current_positions[i].flight_id);
                snprintf(event->flight_b, sizeof(event->flight_b), "%s",
                         current_positions[j].flight_id);
                event->position_a = current_positions[i];
                event->position_b = current_positions[j];
                event->horizontal_distance_m = d_horiz;
                event->vertical_distance_m = d_vert;
            } else {
                shm->dropped_violation_events++;
            }
            pthread_cond_signal(notification_cond);
            pthread_mutex_unlock(notification_mutex);

            if (*total_violations >= max_violations) {
                printf("\n[CRITICAL] Violation limit reached (%d). Aborting...\n",
                       *total_violations);
                for (int k = 0; k < n_flights; k++) {
                    if (pipe_open[k]) kill(pids[k], SIGTERM);
                }
                return 1;
            }
        }
    }
    return 0;
}
