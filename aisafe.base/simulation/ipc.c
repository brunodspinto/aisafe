/*
 * ipc.c - Inter-process communication utilities
 */
#include <stdio.h>
#include <string.h>
#include "ipc.h"
#include "thread_io.h"   /* ex1-9.c: print_history runs in the coordinator thread */

int find_or_create_flight(flight_history_t *histories, int n_flights,
                          const char *flight_id) {
    int i;
    for (i = 0; i < n_flights; i++) {
        if (strcmp(histories[i].flight_id, flight_id) == 0)
            return i;
        if (histories[i].flight_id[0] == '\0') {
            strncpy(histories[i].flight_id, flight_id,
                    sizeof(histories[i].flight_id) - 1);
            histories[i].flight_id[sizeof(histories[i].flight_id) - 1] = '\0';
            histories[i].aca_state = ACA_BEFORE;
            return i;
        }
    }
    return -1; /* no room */
}

void print_history(const flight_history_t *histories, int n_flights) {
    int i, j;
    const aircraft_position_t *p;

    tprintf("\n=== Position History (ACA only) ===\n");
    for (i = 0; i < n_flights; i++) {
        if (histories[i].flight_id[0] == '\0') continue;

        if (histories[i].count == 0) {
            tprintf("Flight %s: never entered ACA\n", histories[i].flight_id);
            continue;
        }

        tprintf("Flight %s: %d positions inside ACA",
                histories[i].flight_id, histories[i].count);
        if (histories[i].aca_state == ACA_AFTER)
            tprintf(" [exited ACA]");
        else if (histories[i].aca_state == ACA_INSIDE)
            tprintf(" [still inside ACA at end]");
        tprintf("\n");

        for (j = 0; j < histories[i].count; j++) {
            p = &histories[i].positions[j];
            tprintf("  [%d] lat=%.4f lon=%.4f alt=%.0fm spd=%.0fkt hdg=%.1f\n",
                    j, p->latitude, p->longitude,
                    p->altitude_meters, p->speed_knots, p->heading_deg);
        }
    }
}
