/*
 * test_flight_tester.c - Unit and integration tests for the flight_tester binary (US085).
 *
 * Build:  make test_flight_tester
 * Run:    ./test_flight_tester
 *
 * The integration tests (test_full_tester_*) require the flight_tester binary to be
 * compiled first: make flight_tester
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "validation.h"
#include "flight_parser.h"

/* ---- minimal test framework ---- */

static int tests_run    = 0;
static int tests_failed = 0;

#define ASSERT_TRUE(condition, message) \
    do { \
        tests_run++; \
        if (!(condition)) { \
            fprintf(stderr, "FAIL: %s (line %d)\n", (message), __LINE__); \
            tests_failed++; \
        } \
    } while (0)

/* ---- unit tests ---- */

static void test_parse_valid_single_plan(void) {
    flight_plan_t *plans = NULL;
    int n = 0;
    const int rc = parse_flight_plans_from_json(
            "tests/fixtures/valid_single.json", &plans, &n);
    ASSERT_TRUE(rc == 0,  "parse should succeed for valid_single.json");
    ASSERT_TRUE(n == 1,   "should parse exactly 1 plan");
    if (rc == 0 && n == 1) {
        ASSERT_TRUE(strcmp(plans[0].identifier, "TP001") == 0,
                    "identifier should be TP001");
        ASSERT_TRUE(strcmp(plans[0].flight_type, "REGULAR") == 0,
                    "flight_type should be REGULAR");
        ASSERT_TRUE(plans[0].leg_count == 1,
                    "should have 1 leg");
        ASSERT_TRUE(plans[0].legs != NULL && plans[0].legs[0].segment_count == 1,
                    "leg should have 1 segment");
        /* cleanup */
        if (plans[0].legs) {
            free(plans[0].legs[0].segments);
            free(plans[0].legs);
        }
    }
    free(plans);
}

static void test_validate_coordinate_bounds(void) {
    coordinate_t valid   = { 41.15,  -8.61 };
    coordinate_t bad_lat = { 91.0,    0.0  };
    coordinate_t bad_lon = {  0.0,  181.0  };

    ASSERT_TRUE(validate_coordinate(&valid)   == 1, "valid coord should pass");
    ASSERT_TRUE(validate_coordinate(&bad_lat) == 0, "latitude > 90 should fail");
    ASSERT_TRUE(validate_coordinate(&bad_lon) == 0, "longitude > 180 should fail");
}

static void test_validate_plan_rejects_missing_legs(void) {
    flight_plan_t plan;
    memset(&plan, 0, sizeof(plan));
    strncpy(plan.identifier,  "TP001",   sizeof(plan.identifier)  - 1);
    strncpy(plan.flight_type, "REGULAR", sizeof(plan.flight_type) - 1);
    plan.legs      = NULL;
    plan.leg_count = 0;
    ASSERT_TRUE(validate_flight_plan(&plan) == 0, "plan with no legs should fail validation");
}

/* ---- integration tests (require compiled flight_tester binary) ---- */

static void test_full_tester_pass(void) {
    const int rc = system("./flight_tester tests/fixtures/valid_single.json > /tmp/ft_out.json 2>&1");
    ASSERT_TRUE(rc == 0, "flight_tester should exit 0 on a valid plan");

    FILE *f = fopen("/tmp/ft_out.json", "r");
    ASSERT_TRUE(f != NULL, "output file should exist");
    if (f) {
        char buf[512] = {0};
        fread(buf, 1, sizeof(buf) - 1, f);
        fclose(f);
        ASSERT_TRUE(strstr(buf, "\"PASS\"") != NULL, "output should contain PASS");
    }
}

static void test_full_tester_fail_invalid_plan(void) {
    const int rc = system("./flight_tester tests/fixtures/invalid_no_legs.json > /tmp/ft_fail.json 2>&1");
    ASSERT_TRUE(rc != 0, "flight_tester should exit non-zero on invalid plan");

    FILE *f = fopen("/tmp/ft_fail.json", "r");
    ASSERT_TRUE(f != NULL, "output file should exist");
    if (f) {
        char buf[512] = {0};
        fread(buf, 1, sizeof(buf) - 1, f);
        fclose(f);
        ASSERT_TRUE(strstr(buf, "\"FAIL\"") != NULL, "output should contain FAIL");
    }
}

/* ---- main ---- */

int main(void) {
    printf("Running flight_tester unit tests...\n");

    test_parse_valid_single_plan();
    test_validate_coordinate_bounds();
    test_validate_plan_rejects_missing_legs();
    test_full_tester_pass();
    test_full_tester_fail_invalid_plan();

    printf("Tests run: %d\n", tests_run);
    printf("Tests failed: %d\n", tests_failed);

    return tests_failed == 0 ? EXIT_SUCCESS : EXIT_FAILURE;
}
