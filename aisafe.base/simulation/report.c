#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <time.h>
#include "types.h"
#include "report.h"

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

void generate_final_report(const flight_history_t *histories, int n_flights,
                           int total_violations, int dropped_events,
                           int was_aborted) {
    pid_t pid;
    int status;

    pid = fork();

    if (pid == -1) {
        perror("fork error");
        exit(EXIT_FAILURE);
    }

    if (pid > 0) {
        /* Parent: wait for the report child before releasing memory */
        printf("[SYSTEM US109] Parent process (PID: %d) waiting for report process...\n", getpid());
        waitpid(pid, &status, 0);
        if (WIFEXITED(status)) {
            if (WEXITSTATUS(status) == EXIT_SUCCESS)
                printf("[SYSTEM US109] Report process ended successfully with exit value: %d\n", WEXITSTATUS(status));
            else
                printf("[SYSTEM US109] Report process ended with unexpected exit value: %d\n", WEXITSTATUS(status));
        } else {
            printf("[SYSTEM US109] Report process terminated abnormally!\n");
        }
    } else {
        /* Child: write the report file and exit */
        printf("[REPORT US109] Child process (PID: %d) generating report...\n", getpid());

        FILE *file = fopen("simulation_report.txt", "w");
        if (!file) {
            perror("fopen error");
            exit(EXIT_FAILURE);
        }

        time_t now = time(NULL);
        fprintf(file, "==================================================\n");
        fprintf(file, "        FLIGHT SIMULATION - FINAL REPORT\n");
        fprintf(file, "==================================================\n");
        fprintf(file, "Generated on: %s", ctime(&now));
        fprintf(file, "Simulation End Status: %s\n",
                was_aborted ? "ABORTED (Threshold Reached)" : "COMPLETED (Normal)");
        fprintf(file, "Total Safety Violations Detected: %d\n", total_violations);
        fprintf(file, "Dropped Live Violation Events: %d\n", dropped_events);
        fprintf(file, "Live Safety Violation Log: %s\n", LIVE_VIOLATION_LOG);
        fprintf(file, "Total Aircraft Records: %d\n", n_flights);
        fprintf(file, "==================================================\n\n");

        for (int i = 0; i < n_flights; i++) {
            fprintf(file, "--------------------------------------------------\n");
            fprintf(file, "Aircraft Identifier: %s\n", histories[i].flight_id);

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

        exit(EXIT_SUCCESS);
    }
}
