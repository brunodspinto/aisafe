#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include "types.h"
#include "report.h"
#include "thread_io.h"   /* ex1-9.c: generate_final_report runs in the report thread */

static const char *LIVE_VIOLATION_LOG = "simulation_violation_log.txt";

void reset_live_violation_log(void) {
    FILE *file = fopen(LIVE_VIOLATION_LOG, "w");
    if (!file) {
        perror("fopen error");
        exit(EXIT_FAILURE);
    }

    fprintf(file, "================ LIVE SAFETY VIOLATION LOG ================\n");
    fclose(file);
}

void append_violation_event_to_log(const violation_event_t *event) {
    char timestamp_buf[32];
    struct tm *tm_info = localtime(&event->timestamp);
    FILE *file = fopen(LIVE_VIOLATION_LOG, "a");
    if (!file) {
        perror("fopen error");
        exit(EXIT_FAILURE);
    }

    if (tm_info != NULL)
        strftime(timestamp_buf, sizeof(timestamp_buf), "%Y-%m-%d %H:%M:%S", tm_info);
    else
        snprintf(timestamp_buf, sizeof(timestamp_buf), "%ld", (long)event->timestamp);

    fprintf(file, "[%s] %s <-> %s | H=%.2fm | V=%.2fm\n",
            timestamp_buf, event->flight_a, event->flight_b,
            event->horizontal_distance_m, event->vertical_distance_m);
    fprintf(file, "  A: lat=%.4f lon=%.4f alt=%.0fm\n",
            event->position_a.latitude, event->position_a.longitude,
            event->position_a.altitude_meters);
    fprintf(file, "  B: lat=%.4f lon=%.4f alt=%.0fm\n\n",
            event->position_b.latitude, event->position_b.longitude,
            event->position_b.altitude_meters);
    fclose(file);
}

void append_violation_drop_notice(int dropped_count) {
    FILE *file = fopen(LIVE_VIOLATION_LOG, "a");
    if (!file) {
        perror("fopen error");
        exit(EXIT_FAILURE);
    }

    fprintf(file,
            "[WARNING] %d safety violation events were not written to the live queue because the buffer was full.\n\n",
            dropped_count);
    fclose(file);
}

static void write_position(FILE *file, const char *label,
                           const aircraft_position_t *position) {
    fprintf(file,
            "  %s: lat=%.4f | lon=%.4f | alt=%.0fm | speed=%.0fkt"
            " | heading=%.1f | vertical_rate=%.1fm/s\n",
            label,
            position->latitude,
            position->longitude,
            position->altitude_meters,
            position->speed_knots,
            position->heading_deg,
            position->vz_mps);
}

void generate_final_report(const flight_history_t *histories, int n_flights,
                           const violation_event_t *violation_events,
                           int violation_event_count,
                           int total_violations, int dropped_events,
                           int was_aborted) {
    /* ex1-9.c: tprintf() (write-based) — esta função corre na report_thread,
     * em concorrência com as outras threads do processo pai. */
    tprintf("[REPORT US109] Report thread generating final simulation report...\n");

    FILE *file = fopen("simulation_report.txt", "w");
    if (!file) {
        perror("fopen error");
        exit(EXIT_FAILURE);
    }

    time_t now = time(NULL);
    const int validation_passed = !was_aborted && total_violations == 0;

    fprintf(file, "==================================================\n");
    fprintf(file, "        FLIGHT SIMULATION - FINAL REPORT\n");
    fprintf(file, "==================================================\n");
    fprintf(file, "Generated on: %s", ctime(&now));
    fprintf(file, "Simulation End Status: %s\n",
            was_aborted ? "ABORTED (Threshold Reached)" : "COMPLETED (Normal)");
    fprintf(file, "Final Validation Result: %s\n",
            validation_passed ? "PASS" : "FAIL");
    fprintf(file, "Total Flights: %d\n", n_flights);
    fprintf(file, "Total Safety Violations Detected: %d\n", total_violations);
    fprintf(file, "Detailed Safety Violation Events Stored: %d\n", violation_event_count);
    fprintf(file, "Dropped Live Violation Events: %d\n", dropped_events);
    fprintf(file, "Live Safety Violation Log: %s\n", LIVE_VIOLATION_LOG);
    fprintf(file, "==================================================\n\n");

    fprintf(file, "SAFETY VIOLATION EVENTS\n");
    fprintf(file, "--------------------------------------------------\n");
    if (violation_event_count == 0) {
        fprintf(file, "No safety violation events were recorded.\n\n");
    } else {
        for (int i = 0; i < violation_event_count; i++) {
            char timestamp_buf[32];
            const violation_event_t *event = &violation_events[i];
            struct tm *tm_info = localtime(&event->timestamp);
            if (tm_info != NULL)
                strftime(timestamp_buf, sizeof(timestamp_buf), "%Y-%m-%d %H:%M:%S", tm_info);
            else
                snprintf(timestamp_buf, sizeof(timestamp_buf), "%ld", (long)event->timestamp);

            fprintf(file, "[%03d] %s <-> %s | timestamp=%s"
                    " | horizontal=%.2fm | vertical=%.2fm\n",
                    i + 1,
                    event->flight_a,
                    event->flight_b,
                    timestamp_buf,
                    event->horizontal_distance_m,
                    event->vertical_distance_m);
            write_position(file, event->flight_a, &event->position_a);
            write_position(file, event->flight_b, &event->position_b);
            fprintf(file, "\n");
        }
    }

    fprintf(file, "FLIGHT EXECUTION STATUSES\n");
    fprintf(file, "==================================================\n\n");
    for (int i = 0; i < n_flights; i++) {
        fprintf(file, "--------------------------------------------------\n");
        fprintf(file, "Aircraft Identifier: %s\n", histories[i].flight_id);
        fprintf(file, "Execution Status: %s\n",
                was_aborted ? "STOPPED (simulation aborted)" : "COMPLETED");

        const char *aca_label;
        switch (histories[i].aca_state) {
            case ACA_INSIDE: aca_label = "Inside ACA"; break;
            case ACA_AFTER:  aca_label = "Exited ACA"; break;
            default:         aca_label = "Never entered ACA"; break;
        }
        fprintf(file, "ACA Status: %s\n", aca_label);
        fprintf(file, "Logged Snapshots inside ACA: %d\n", histories[i].count);
        fprintf(file, "--------------------------------------------------\n");

        if (histories[i].count == 0) {
            fprintf(file, "  [INFO] Aircraft never entered the Air Control Area.\n");
        } else {
            for (int j = 0; j < histories[i].count; j++) {
                const aircraft_position_t *pos = &histories[i].positions[j];
                fprintf(file, "  [%03d] Lat: %.4f | Lon: %.4f | Alt: %.0fm"
                        " | Spd: %.0fkt | Hdg: %.1f | Vz: %.1fm/s\n",
                        j + 1, pos->latitude, pos->longitude,
                        pos->altitude_meters, pos->speed_knots,
                        pos->heading_deg, pos->vz_mps);
            }
        }
        fprintf(file, "\n");
    }

    fprintf(file, "=================== END OF REPORT ===================\n");
    fclose(file);
    tprintf("[REPORT US109] Final simulation report saved to simulation_report.txt\n");
}
