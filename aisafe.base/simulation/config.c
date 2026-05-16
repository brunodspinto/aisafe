/*
 * config.c - Simulation parameter loading and validation
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include "config.h"

#define NM_TO_METERS 1852.0

static char *trim(char *s) {
    while (isspace((unsigned char)*s)) s++;
    if (*s == '\0') return s;
    char *end = s + strlen(s) - 1;
    while (end > s && isspace((unsigned char)*end)) *end-- = '\0';
    return s;
}

int load_config(const char *path, simulation_params_t *params) {
    params->aca_north_lat     = 43.0;
    params->aca_south_lat     = 36.0;
    params->aca_east_lon      = -6.0;
    params->aca_west_lon      = -10.0;
    params->n_flights         = 3;
    params->safe_dist_horiz_m = 8.0 * NM_TO_METERS;
    params->safe_dist_vert_m  = 600.0;
    params->max_violations    = 5;

    FILE *f = fopen(path, "r");
    if (!f) return -1;

    char line[256];
    int  lineno = 0;

    while (fgets(line, sizeof(line), f)) {
        lineno++;
        char *s = trim(line);
        if (*s == '#' || *s == '\0') continue;

        char *eq = strchr(s, '=');
        if (!eq) {
            fclose(f);
            return lineno;
        }
        *eq = '\0';
        char *key = trim(s);
        char *val = trim(eq + 1);

        if      (strcmp(key, "aca_north")          == 0) params->aca_north_lat     = atof(val);
        else if (strcmp(key, "aca_south")          == 0) params->aca_south_lat     = atof(val);
        else if (strcmp(key, "aca_east")           == 0) params->aca_east_lon      = atof(val);
        else if (strcmp(key, "aca_west")           == 0) params->aca_west_lon      = atof(val);
        else if (strcmp(key, "n_flights")          == 0) params->n_flights         = atoi(val);
        else if (strcmp(key, "safe_dist_horiz_nm") == 0) params->safe_dist_horiz_m = atof(val) * NM_TO_METERS;
        else if (strcmp(key, "safe_dist_vert_m")   == 0) params->safe_dist_vert_m  = atof(val);
        else if (strcmp(key, "max_violations")     == 0) params->max_violations    = atoi(val);
    }

    fclose(f);
    return 0;
}

int validate_config(const simulation_params_t *params) {
    int ok = 1;

    if (params->aca_south_lat < -90.0 || params->aca_south_lat > 90.0) {
        fprintf(stderr, "config error: aca_south must be in [-90, 90] (got %.4f)\n",
                params->aca_south_lat);
        ok = 0;
    }
    if (params->aca_north_lat < -90.0 || params->aca_north_lat > 90.0) {
        fprintf(stderr, "config error: aca_north must be in [-90, 90] (got %.4f)\n",
                params->aca_north_lat);
        ok = 0;
    }
    if (params->aca_south_lat >= params->aca_north_lat) {
        fprintf(stderr, "config error: aca_south (%.4f) must be less than aca_north (%.4f)\n",
                params->aca_south_lat, params->aca_north_lat);
        ok = 0;
    }
    if (params->aca_west_lon < -180.0 || params->aca_west_lon > 180.0) {
        fprintf(stderr, "config error: aca_west must be in [-180, 180] (got %.4f)\n",
                params->aca_west_lon);
        ok = 0;
    }
    if (params->aca_east_lon < -180.0 || params->aca_east_lon > 180.0) {
        fprintf(stderr, "config error: aca_east must be in [-180, 180] (got %.4f)\n",
                params->aca_east_lon);
        ok = 0;
    }
    if (params->aca_west_lon >= params->aca_east_lon) {
        fprintf(stderr, "config error: aca_west (%.4f) must be less than aca_east (%.4f)\n",
                params->aca_west_lon, params->aca_east_lon);
        ok = 0;
    }
    if (params->n_flights < 1 || params->n_flights > MAX_FLIGHTS) {
        fprintf(stderr, "config error: n_flights must be in [1, %d] (got %d)\n",
                MAX_FLIGHTS, params->n_flights);
        ok = 0;
    }
    if (params->safe_dist_horiz_m <= 0.0) {
        fprintf(stderr, "config error: safe_dist_horiz_nm must be positive (got %.2f m)\n",
                params->safe_dist_horiz_m);
        ok = 0;
    }
    if (params->safe_dist_vert_m <= 0.0) {
        fprintf(stderr, "config error: safe_dist_vert_m must be positive (got %.2f)\n",
                params->safe_dist_vert_m);
        ok = 0;
    }
    if (params->max_violations < 1) {
        fprintf(stderr, "config error: max_violations must be >= 1 (got %d)\n",
                params->max_violations);
        ok = 0;
    }

    return ok ? 0 : -1;
}

void print_config(const simulation_params_t *params) {
    printf("--- Simulation Parameters ---\n");
    printf("  ACA: lat [%.2f, %.2f]  lon [%.2f, %.2f]\n",
           params->aca_south_lat, params->aca_north_lat,
           params->aca_west_lon,  params->aca_east_lon);
    printf("  Flights: %d\n", params->n_flights);
    printf("  Safety cylinder: %.0f m horiz (%.2f NM)  /  %.0f m vert\n",
           params->safe_dist_horiz_m, params->safe_dist_horiz_m / 1852.0,
           params->safe_dist_vert_m);
    printf("  Max violations: %d\n", params->max_violations);
    printf("-----------------------------\n");
}
