#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "validation.h"

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

static flight_plan_t *create_valid_plan(void) {
    flight_plan_t *plan = (flight_plan_t *)calloc(1, sizeof(flight_plan_t));
    if (!plan) {
        return NULL;
    }

    strncpy(plan->identifier, "TP123", sizeof(plan->identifier) - 1);
    strncpy(plan->flight_type, "REGULAR", sizeof(plan->flight_type) - 1);
    plan->leg_count = 1;

    plan->legs = (leg_t *)calloc((size_t)plan->leg_count, sizeof(leg_t));
    if (!plan->legs) {
        free(plan);
        return NULL;
    }

    plan->legs[0].segment_count = 1;
    plan->legs[0].segments = (segment_t *)calloc(1, sizeof(segment_t));
    if (!plan->legs[0].segments) {
        free(plan->legs);
        free(plan);
        return NULL;
    }

    return plan;
}

static void destroy_plan(flight_plan_t *plan) {
    if (!plan) {
        return;
    }

    if (plan->legs) {
        for (int i = 0; i < plan->leg_count; i++) {
            free(plan->legs[i].segments);
        }

        free(plan->legs);
    }

    free(plan);
}

static void test_validate_simulation_params(void) {
    simulation_params_t params = {
        .aca_north_lat = 20.0,
        .aca_south_lat = 10.0,
        .aca_east_lon = 5.0,
        .aca_west_lon = -5.0,
        .n_flights = 3,
        .safe_dist_horiz_m = 25.0,
        .safe_dist_vert_m = 100.0,
        .max_violations = 5,
    };

    ASSERT_TRUE(validate_simulation_params(&params) == 1, "valid simulation parameters should pass");

    params.aca_north_lat = 5.0; /* north less than south */
    ASSERT_TRUE(validate_simulation_params(&params) == 0, "invalid ACA latitude bounds should fail");

    params.aca_north_lat = 20.0;
    params.aca_west_lon = 10.0; /* west greater than east */
    ASSERT_TRUE(validate_simulation_params(&params) == 0, "invalid ACA longitude bounds should fail");

    params.aca_west_lon = -5.0;
    params.n_flights = 0;
    ASSERT_TRUE(validate_simulation_params(&params) == 0, "non-positive n_flights should fail");
}

static void test_validate_coordinate(void) {
    coordinate_t coord = {
        .latitude = 41.15,
        .longitude = -8.61,
    };

    ASSERT_TRUE(validate_coordinate(&coord) == 1, "valid coordinate should pass");

    coord.latitude = -91.0;
    ASSERT_TRUE(validate_coordinate(&coord) == 0, "latitude below range should fail");

    coord.latitude = 41.15;
    coord.longitude = 181.0;
    ASSERT_TRUE(validate_coordinate(&coord) == 0, "longitude above range should fail");
}

static void test_validate_flight_plan(void) {
    flight_plan_t *plan = create_valid_plan();
    ASSERT_TRUE(plan != NULL, "test plan should be created");
    if (!plan) {
        return;
    }

    ASSERT_TRUE(validate_flight_plan(plan) == 1, "valid flight plan should pass");

    plan->identifier[0] = '\0';
    ASSERT_TRUE(validate_flight_plan(plan) == 0, "empty identifier should fail");
    strncpy(plan->identifier, "TP123", sizeof(plan->identifier) - 1);

    strncpy(plan->flight_type, "CARGO", sizeof(plan->flight_type) - 1);
    plan->flight_type[sizeof(plan->flight_type) - 1] = '\0';
    ASSERT_TRUE(validate_flight_plan(plan) == 0, "unsupported flight type should fail");
    strncpy(plan->flight_type, "REGULAR", sizeof(plan->flight_type) - 1);
    plan->flight_type[sizeof(plan->flight_type) - 1] = '\0';

    plan->leg_count = 0;
    ASSERT_TRUE(validate_flight_plan(plan) == 0, "non-positive leg count should fail");
    plan->leg_count = 1;

    free(plan->legs);
    plan->legs = NULL;
    ASSERT_TRUE(validate_flight_plan(plan) == 0, "NULL legs array should fail");

    destroy_plan(plan);
}

int main(void) {
    test_validate_simulation_params();
    test_validate_coordinate();
    test_validate_flight_plan();

    printf("Tests run: %d\n", tests_run);
    printf("Tests failed: %d\n", tests_failed);

    return tests_failed == 0 ? EXIT_SUCCESS : EXIT_FAILURE;
}
