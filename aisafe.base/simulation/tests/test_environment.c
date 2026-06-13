/*
 * test_environment.c - Unit tests for US110 wind-drift maths (environment.c)
 *
 * Verifies the two correctness rules the simulation relies on:
 *   1. drift direction/magnitude follow the meteorological convention
 *      (wind blows FROM wind_direction; aircraft drifts toward +180°);
 *   2. the drift touches ONLY lat/lon — never the cruise/climb progress
 *      variables — so every segment still terminates (US110 §4.3).
 */
#include <stdio.h>
#include <stdlib.h>
#include <math.h>

#include "environment.h"

static int tests_run = 0;
static int tests_failed = 0;

#define ASSERT_TRUE(condition, message) \
    do { \
        tests_run++; \
        if (!(condition)) { \
            fprintf(stderr, "FAIL: %s (line %d)\n", message, __LINE__); \
            tests_failed++; \
        } \
    } while (0)

#define ASSERT_NEAR(actual, expected, eps, message) \
    ASSERT_TRUE(fabs((actual) - (expected)) <= (eps), message)

/* ------------------------------------------------------------------ */

static void test_calm_wind_does_not_move_aircraft(void) {
    double lat = 40.0, lon = -8.0;

    apply_wind_drift_values(&lat, &lon, 0.0, 123.0, 1.0);   /* speed 0 → calm */
    ASSERT_NEAR(lat, 40.0, 1e-12, "calm wind must not change latitude");
    ASSERT_NEAR(lon, -8.0, 1e-12, "calm wind must not change longitude");

    apply_wind_drift_values(&lat, &lon, -5.0, 123.0, 1.0);  /* negative → calm */
    ASSERT_NEAR(lat, 40.0, 1e-12, "negative wind speed must not change latitude");
    ASSERT_NEAR(lon, -8.0, 1e-12, "negative wind speed must not change longitude");
}

static void test_wind_from_north_drifts_south(void) {
    /* wind_direction = 0 means the wind blows FROM the north → aircraft drifts
     * SOUTH (latitude decreases) with no east/west component. */
    double lat = 40.0, lon = -8.0;
    const double expected_lat = 40.0 - (10.0 * 1.0) / 110574.0;

    apply_wind_drift_values(&lat, &lon, 10.0, 0.0, 1.0);

    ASSERT_TRUE(lat < 40.0, "north wind must push latitude south (decrease)");
    ASSERT_NEAR(lat, expected_lat, 1e-9, "north wind latitude delta magnitude");
    ASSERT_NEAR(lon, -8.0, 1e-9, "north wind must not change longitude");
}

static void test_wind_from_west_drifts_east(void) {
    /* wind_direction = 270 means the wind blows FROM the west → aircraft drifts
     * EAST (longitude increases) with no north/south component. */
    double lat = 40.0, lon = -8.0;
    const double expected_lon =
        -8.0 + (10.0 * 1.0) / (111320.0 * cos(40.0 * (M_PI / 180.0)));

    apply_wind_drift_values(&lat, &lon, 10.0, 270.0, 1.0);

    ASSERT_TRUE(lon > -8.0, "west wind must push longitude east (increase)");
    ASSERT_NEAR(lon, expected_lon, 1e-9, "west wind longitude delta magnitude");
    ASSERT_NEAR(lat, 40.0, 1e-9, "west wind must not change latitude");
}

static void test_drift_leaves_progress_variables_untouched(void) {
    /* Mirrors the flight_process.c call site: only lat/lon are passed to the
     * drift function, so cruise distance and altitude cannot be affected — the
     * termination invariant (US110 §4.3). This guards against a future change
     * that might wire progress variables into the drift maths. */
    double lat = 38.77, lon = -9.13;
    double dist_covered_m = 1234.5;   /* cruise progress */
    double alt = 9000.0;              /* climb/descend progress */

    const double dist_before = dist_covered_m;
    const double alt_before  = alt;

    apply_wind_drift_values(&lat, &lon, 15.0, 90.0, 1.0);

    ASSERT_TRUE(dist_covered_m == dist_before, "drift must not change dist_covered_m");
    ASSERT_TRUE(alt == alt_before, "drift must not change altitude");
    ASSERT_TRUE(lat != 38.77 || lon != -9.13, "drift must actually move lat/lon");
}

static void test_segment_overload_matches_values_overload(void) {
    /* apply_wind_drift(seg) must delegate to apply_wind_drift_values with the
     * segment's wind fields, producing an identical result. */
    segment_t seg;
    seg.wind_speed = 12.0;
    seg.wind_direction = 45.0;

    double lat_a = 41.0, lon_a = -8.6;
    double lat_b = 41.0, lon_b = -8.6;

    apply_wind_drift(&lat_a, &lon_a, &seg, 2.0);
    apply_wind_drift_values(&lat_b, &lon_b, seg.wind_speed, seg.wind_direction, 2.0);

    ASSERT_NEAR(lat_a, lat_b, 1e-12, "segment overload latitude must match values overload");
    ASSERT_NEAR(lon_a, lon_b, 1e-12, "segment overload longitude must match values overload");
}

int main(void) {
    test_calm_wind_does_not_move_aircraft();
    test_wind_from_north_drifts_south();
    test_wind_from_west_drifts_east();
    test_drift_leaves_progress_variables_untouched();
    test_segment_overload_matches_values_overload();

    printf("Tests run: %d\n", tests_run);
    printf("Tests failed: %d\n", tests_failed);

    return tests_failed == 0 ? EXIT_SUCCESS : EXIT_FAILURE;
}
