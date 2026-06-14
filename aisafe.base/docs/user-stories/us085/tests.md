# US085 — Tests and Coverage

## Scope

US085 covers the simulation-based testing of a VALIDATED DSL flight plan by an authenticated
Pilot, transitioning its status to `TESTED`. Testing is split across three layers:

- **`FlightPlanTest`** (domain) — the `markTested()` lifecycle transition and its guards.
- **`FlightPlanJsonSerializerTest`** (DSL utility) — serialization of a `FlightPlanAst` to
  the JSON format consumed by the C binary.
- **`TestFlightPlanControllerTest`** (application) — listing logic and PASS/FAIL execution
  paths; C binary invocation is replaced by a `FlightTesterRunner` stub so tests run without
  a compiled binary or a live JPA context.
- **C unit tests** (`test_flight_tester.c`) — validation logic and full-binary smoke tests
  for the `flight_tester` executable.
- **Manual acceptance tests** — end-to-end validation of the console flow.

---

## Automated Tests

### `FlightPlanTest` (domain — `markTested` lifecycle)

Location: `src/test/java/aisafe/flightplan/domain/FlightPlanTest.java`

> These tests extend the existing `FlightPlanTest` class; the DSL-based and form-based
> construction tests already present belong to US081 and US080 respectively.

---

**Test:** `ensureValidatedDslPlanCanBeMarkedTested`

```java
@Test
void ensureValidatedDslPlanCanBeMarkedTested() {
    final FlightPlan plan = validDslPlan();
    plan.markValidated();
    plan.markTested();
    assertEquals(FlightPlanStatus.TESTED, plan.status());
}
```

---

**Test:** `ensureDraftPlanCannotBeMarkedTested`

```java
@Test
void ensureDraftPlanCannotBeMarkedTested() {
    final FlightPlan plan = validDslPlan();
    // status is DRAFT — markTested() must reject
    assertThrows(IllegalStateException.class, plan::markTested);
}
```

---

**Test:** `ensureAlreadyTestedPlanCannotBeMarkedTestedAgain`

```java
@Test
void ensureAlreadyTestedPlanCannotBeMarkedTestedAgain() {
    final FlightPlan plan = validDslPlan();
    plan.markValidated();
    plan.markTested();
    // second call must reject
    assertThrows(IllegalStateException.class, plan::markTested);
}
```

---

### `FlightPlanJsonSerializerTest`

Location: `src/test/java/aisafe/dsl/FlightPlanJsonSerializerTest.java`

---

**Test:** `ensureSingleLegPlanProducesNonEmptyFile`

```java
@Test
void ensureSingleLegPlanProducesNonEmptyFile() throws Exception {
    final FlightPlanAst ast = singleLegAst("TP001");
    final Path tmp = new FlightPlanJsonSerializer().toTempFile(ast);
    try {
        assertTrue(Files.size(tmp) > 0);
    } finally {
        Files.deleteIfExists(tmp);
    }
}
```

---

**Test:** `ensureOutputIsValidJsonArray`

```java
@Test
void ensureOutputIsValidJsonArray() throws Exception {
    final FlightPlanAst ast = singleLegAst("TP001");
    final Path tmp = new FlightPlanJsonSerializer().toTempFile(ast);
    try {
        final String json = Files.readString(tmp);
        assertTrue(json.trim().startsWith("["), "root must be a JSON array");
        assertTrue(json.trim().endsWith("]"));
    } finally {
        Files.deleteIfExists(tmp);
    }
}
```

---

**Test:** `ensureIdentifierAndFlightTypeArePresent`

```java
@Test
void ensureIdentifierAndFlightTypeArePresent() throws Exception {
    final FlightPlanAst ast = singleLegAst("TP001");
    final Path tmp = new FlightPlanJsonSerializer().toTempFile(ast);
    try {
        final String json = Files.readString(tmp);
        assertTrue(json.contains("\"TP001\""));
        assertTrue(json.contains("\"REGULAR\"") || json.contains("\"CHARTER\""));
    } finally {
        Files.deleteIfExists(tmp);
    }
}
```

---

**Test:** `ensureSegmentCoordinatesArePresent`

```java
@Test
void ensureSegmentCoordinatesArePresent() throws Exception {
    final FlightPlanAst ast = singleLegAst("TP001");
    final Path tmp = new FlightPlanJsonSerializer().toTempFile(ast);
    try {
        final String json = Files.readString(tmp);
        assertTrue(json.contains("start_coord"));
        assertTrue(json.contains("end_coord"));
        assertTrue(json.contains("altitude_m"));
    } finally {
        Files.deleteIfExists(tmp);
    }
}
```

---

**Test:** `ensureMultiLegPlanSerializesAllLegs`

```java
@Test
void ensureMultiLegPlanSerializesAllLegs() throws Exception {
    final FlightPlanAst ast = twoLegAst("TP002");
    final Path tmp = new FlightPlanJsonSerializer().toTempFile(ast);
    try {
        final String json = Files.readString(tmp);
        // Both legs contribute segments; start_coord should appear at least twice
        int count = 0;
        int idx = 0;
        while ((idx = json.indexOf("start_coord", idx)) >= 0) {
            count++;
            idx++;
        }
        assertTrue(count >= 2, "multi-leg plan must serialise all segments");
    } finally {
        Files.deleteIfExists(tmp);
    }
}
```

---

**Test:** `ensureTempFileCanBeDeletedByCallerAfterUse`

```java
@Test
void ensureTempFileCanBeDeletedByCallerAfterUse() throws Exception {
    final FlightPlanAst ast = singleLegAst("TP003");
    final Path tmp = new FlightPlanJsonSerializer().toTempFile(ast);
    assertTrue(Files.exists(tmp));
    Files.delete(tmp);
    assertFalse(Files.exists(tmp));
}
```

---

### `TestFlightPlanControllerTest`

Location: `src/test/java/aisafe/flightplan/application/TestFlightPlanControllerTest.java`

Uses the package-private `(FlightPlanRepository, FlightTesterRunner)` constructor to inject
an in-memory repository and a stub runner; no JPA context or compiled C binary is required.

---

**Tests: listing logic (5)**

| Method | What it asserts |
|--------|----------------|
| `listValidatedDslPlans_returnsOnlyValidatedDslPlans` | Two VALIDATED DSL plans both appear |
| `listValidatedDslPlans_excludesDraftPlans` | DRAFT plan is absent from the list |
| `listValidatedDslPlans_excludesFormBasedPlans` | VALIDATED plan with `dslContent == null` is absent |
| `listValidatedDslPlans_excludesTestedPlans` | TESTED plan is absent from the list |
| `listValidatedDslPlans_emptyWhenNoPlanExists` | Empty repo returns empty list |

---

**Tests: guard conditions (3)**

| Method | What it asserts |
|--------|----------------|
| `testFlightPlan_throwsIllegalArgumentWhenPlanNotFound` | `IllegalArgumentException` for unknown designator |
| `testFlightPlan_throwsIllegalStateWhenPlanNotValidated` | `IllegalStateException` for DRAFT plan |
| `testFlightPlan_throwsIllegalStateWhenDslContentIsNull` | `IllegalStateException` for form-based VALIDATED plan |

---

**Tests: execution paths — AC085.5 and AC085.6 (3)**

| Method | What it asserts |
|--------|----------------|
| `testFlightPlan_transitionsToTestedOnPass` | Runner returns PASS → `plan.status() == TESTED` and plan is persisted |
| `testFlightPlan_throwsIllegalStateOnFail` | Runner returns FAIL → `IllegalStateException` with the failure reason |
| `testFlightPlan_leavesStatusValidatedOnFail` | After FAIL → `plan.status()` remains `VALIDATED` (not persisted) |

---

### C Unit Tests — `test_flight_tester.c`

Location: `simulation/tests/test_flight_tester.c`
Build & run: `make test_flight_tester && ./test_flight_tester`

---

**Test:** `test_parse_valid_single_plan`

Parse a minimal valid one-segment JSON file.

```c
static void test_parse_valid_single_plan(void) {
    flight_plan_t *plans = NULL;
    int n = 0;
    int rc = parse_flight_plans_from_json("tests/fixtures/valid_single.json", &plans, &n);
    ASSERT_TRUE(rc == 0,  "parse should succeed");
    ASSERT_TRUE(n == 1,   "should parse exactly 1 plan");
    ASSERT_TRUE(strcmp(plans[0].identifier, "TP001") == 0, "identifier should be TP001");
    /* cleanup */
    free(plans[0].legs[0].segments);
    free(plans[0].legs);
    free(plans);
}
```

---

**Test:** `test_validate_coordinate_bounds`

```c
static void test_validate_coordinate_bounds(void) {
    coordinate_t valid   = { .latitude =  41.15, .longitude =  -8.61 };
    coordinate_t bad_lat = { .latitude =  91.0,  .longitude =   0.0  };
    coordinate_t bad_lon = { .latitude =   0.0,  .longitude = 181.0  };

    ASSERT_TRUE(validate_coordinate(&valid)   == 1, "valid coord should pass");
    ASSERT_TRUE(validate_coordinate(&bad_lat) == 0, "latitude > 90 should fail");
    ASSERT_TRUE(validate_coordinate(&bad_lon) == 0, "longitude > 180 should fail");
}
```

---

**Test:** `test_validate_plan_rejects_missing_legs`

```c
static void test_validate_plan_rejects_missing_legs(void) {
    flight_plan_t plan;
    memset(&plan, 0, sizeof(plan));
    strncpy(plan.identifier,  "TP001",   sizeof(plan.identifier)  - 1);
    strncpy(plan.flight_type, "REGULAR", sizeof(plan.flight_type) - 1);
    plan.legs      = NULL;
    plan.leg_count = 0;
    ASSERT_TRUE(validate_flight_plan(&plan) == 0, "plan with no legs should fail");
}
```

---

**Test:** `test_full_tester_pass` *(integration)*

Invoke the compiled binary against a known-good fixture and verify exit code and JSON output.

```c
static void test_full_tester_pass(void) {
    int rc = system("./flight_tester tests/fixtures/valid_single.json > /tmp/ft_out.json 2>&1");
    ASSERT_TRUE(rc == 0, "flight_tester should exit 0 on a valid plan");

    FILE *f = fopen("/tmp/ft_out.json", "r");
    ASSERT_TRUE(f != NULL, "output file should exist");
    char buf[512] = {0};
    fread(buf, 1, sizeof(buf) - 1, f);
    fclose(f);
    ASSERT_TRUE(strstr(buf, "\"PASS\"") != NULL, "output should contain PASS");
}
```

---

**Test:** `test_full_tester_fail_invalid_plan` *(integration)*

```c
static void test_full_tester_fail_invalid_plan(void) {
    int rc = system("./flight_tester tests/fixtures/invalid_no_legs.json > /tmp/ft_fail.json 2>&1");
    ASSERT_TRUE(rc != 0, "flight_tester should exit non-zero on invalid plan");

    FILE *f = fopen("/tmp/ft_fail.json", "r");
    ASSERT_TRUE(f != NULL, "output file should exist");
    char buf[512] = {0};
    fread(buf, 1, sizeof(buf) - 1, f);
    fclose(f);
    ASSERT_TRUE(strstr(buf, "\"FAIL\"") != NULL, "output should contain FAIL");
}
```

> **Fixture files** required in `simulation/tests/fixtures/`:
> - `valid_single.json` — one plan, one leg, one segment, valid coordinates
> - `invalid_no_legs.json` — one plan with an empty `legs` array

---

## Coverage by Acceptance Criterion

| AC | Automated | Manual |
|----|-----------|--------|
| AC085.1 — only PILOT role | — | MAC085.3 |
| AC085.2 — only VALIDATED plans | `ensureDraftPlanCannotBeMarkedTested`, `ensureAlreadyTestedPlanCannotBeMarkedTestedAgain`, `testFlightPlan_throwsIllegalStateWhenPlanNotValidated` | MAC085.1, MAC085.4 |
| AC085.3 — only DSL-based plans | `testFlightPlan_throwsIllegalStateWhenDslContentIsNull`, `listValidatedDslPlans_excludesFormBasedPlans` | MAC085.5 |
| AC085.4 — C uses all POSIX APIs | `test_full_tester_pass` (exercises full binary) | MAC085.1 |
| AC085.5 — PASS → status TESTED | `testFlightPlan_transitionsToTestedOnPass` | MAC085.1 |
| AC085.6 — FAIL → status unchanged | `testFlightPlan_throwsIllegalStateOnFail`, `testFlightPlan_leavesStatusValidatedOnFail` | MAC085.6 |
| AC085.7 — Java never simulates | *(structural — enforced by design)* | — |
| Serializer correctness | `FlightPlanJsonSerializerTest` (6 tests) | — |
| C parsing / validation | `test_parse_valid_single_plan`, `test_validate_coordinate_bounds`, `test_validate_plan_rejects_missing_legs` | — |
| C binary smoke | `test_full_tester_pass`, `test_full_tester_fail_invalid_plan` | — |

---

## Manual Acceptance Tests

### Prerequisites

1. `flight_tester` binary compiled: `cd aisafe.base/simulation && make flight_tester`.
2. At least one DSL-based flight plan in `VALIDATED` status exists (run US081 to create it,
   then confirm it is in `VALIDATED` state via US083 or by bootstrapping a pre-validated plan).
3. Application running: `mvn -f aisafe.base/pom.xml exec:java` (or equivalent run script).

---

### MAC085.1 — Happy path: VALIDATED DSL plan is tested successfully

**Steps:**

1. Login as Pilot (`pilot1` / `Password1`).
2. Navigate to **Flight Plans > Test Flight Plan**.
3. The system lists available VALIDATED DSL plans.
4. Enter the designator of one (e.g. `TP123`).
5. Wait for the simulation to complete.

**Expected:**

```
Testing flight plan TP123...
Flight plan TP123 tested successfully.
Status: TESTED   Steps simulated: N
```

The plan's status is now `TESTED` and is persisted (verified by reloading the list).

---

### MAC085.2 — No testable plans available

**Precondition:** No VALIDATED DSL plans exist (or all have already been tested).

**Steps:**

1. Navigate to **Flight Plans > Test Flight Plan**.

**Expected:** `No validated DSL flight plans available for testing.`
No crash; user is returned to the menu.

---

### MAC085.3 — Non-PILOT user cannot access the option

**Steps:**

1. Login as a BACKOFFICE_OPERATOR or ATCC user.
2. Navigate to the Flight Plans submenu.

**Expected:** The **Test Flight Plan** option is not present in the menu.

---

### MAC085.4 — DRAFT plan does not appear in the list

**Precondition:** A flight plan exists in `DRAFT` status.

**Steps:**

1. Navigate to **Flight Plans > Test Flight Plan**.
2. Inspect the list.

**Expected:** The DRAFT plan does not appear. Only `VALIDATED` DSL plans are shown.

---

### MAC085.5 — Form-based plan does not appear in the list

**Precondition:** A VALIDATED form-based flight plan exists (created via US080).

**Steps:**

1. Navigate to **Flight Plans > Test Flight Plan**.

**Expected:** The form-based plan does not appear. Only plans with DSL content are listed.

---

### MAC085.6 — C binary reports FAIL: status remains VALIDATED

**Precondition:** A VALIDATED DSL plan exists whose DSL content will cause the C tester to
return FAIL (e.g. a plan with coordinates that fail the simulation validation).

**Steps:**

1. Navigate to **Flight Plans > Test Flight Plan**.
2. Select the plan that is expected to fail.

**Expected:**

```
Simulation failed: <reason reported by flight_tester>
```

The plan's status remains `VALIDATED` (verified by reloading the list).

---

### MAC085.7 — C binary not found / misconfigured

**Steps:**

1. Set `flight.tester.binary` in `application.properties` to a non-existent path.
2. Restart the application. Login as Pilot.
3. Navigate to **Flight Plans > Test Flight Plan** and select a plan.

**Expected:**

```
Error: Flight tester binary not found. Check flight.tester.binary in application.properties.
```

Plan status is unchanged.
