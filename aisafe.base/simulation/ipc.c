/*
 * ipc.c - Inter-process communication utilities
 */
#include <stdio.h>
#include <string.h>
#include "ipc.h"

void store_position(flight_history_t *histories, int n_flights,
                    const aircraft_position_t *pos) {
    for (int i = 0; i < n_flights; i++) {
        if (strcmp(histories[i].flight_id, pos->flight_id) == 0) {
            if (histories[i].count < MAX_POSITIONS)
                histories[i].positions[histories[i].count++] = *pos;
            return;
        }
        if (histories[i].flight_id[0] == '\0') {
            strncpy(histories[i].flight_id, pos->flight_id,
                    sizeof(histories[i].flight_id) - 1);
            histories[i].positions[histories[i].count++] = *pos;
            return;
        }
    }
}

void print_history(const flight_history_t *histories, int n_flights) {
    printf("\n=== Position History ===\n");
    for (int i = 0; i < n_flights; i++) {
        if (histories[i].flight_id[0] == '\0') continue;
        printf("Flight %s: %d positions recorded\n",
               histories[i].flight_id, histories[i].count);
        for (int j = 0; j < histories[i].count; j++) {
            const aircraft_position_t *p = &histories[i].positions[j];
            printf("  [%d] lat=%.4f lon=%.4f alt=%.0fm spd=%.0fkt hdg=%.1f\n",
                   j, p->latitude, p->longitude,
                   p->altitude_meters, p->speed_knots, p->heading_deg);
        }
    }
}
