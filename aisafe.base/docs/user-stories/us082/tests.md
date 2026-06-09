# US082 — Tests and Coverage

## Scope

US082 covers attaching existing weather data to a flight plan owned by the authenticated pilot, voiding a previous flight test when one existed. Testing is split between automated unit tests for the `FlightPlan` aggregate (the weather-data association and the test-voiding rule) and manual acceptance tests for the complete end-to-end use case.

In line with the project's testing convention, the `Application` layer (`InsertWeatherDataController`) is not unit-tested — controllers are thin orchestrators that delegate to the domain and repositories, and are exercised end-to-end through the manual acceptance tests.

## Automated Tests

### `FlightPlanTest` (weather data insertion)

Location: `src/test/java/aisafe/flightplan/domain/FlightPlanTest.java`

> Note: these tests use the helpers `validDslPlan()` (a `DRAFT` plan) and `testedPlan()` (a plan advanced to `TESTED` via `markValidated()` + `markTested()`).

**Test:** `ensureWeatherDataCanBeAdded`

```java
@Test
void ensureWeatherDataCanBeAdded() {
    final FlightPlan plan = validDslPlan();
    plan.addWeatherData(10L);
    assertTrue(plan.weatherDataIds().contains(10L));
}
```

**Test:** `ensureAddingWeatherDataRejectsNull`

```java
@Test
void ensureAddingWeatherDataRejectsNull() {
    final FlightPlan plan = validDslPlan();
    assertThrows(IllegalArgumentException.class, () -> plan.addWeatherData(null));
}
```

**Test:** `ensureWeatherDataIdsAreUnmodifiable`

```java
@Test
void ensureWeatherDataIdsAreUnmodifiable() {
    final FlightPlan plan = validDslPlan();
    plan.addWeatherData(10L);
    assertThrows(UnsupportedOperationException.class, () -> plan.weatherDataIds().add(20L));
}
```

**Test:** `ensureMultipleWeatherDataRecordsCanBeAdded`

```java
@Test
void ensureMultipleWeatherDataRecordsCanBeAdded() {
    final FlightPlan plan = validDslPlan();
    plan.addWeatherData(10L);
    plan.addWeatherData(20L);
    assertEquals(2, plan.weatherDataIds().size());
}
```

**Test:** `ensureAddingNewWeatherDataVoidsTestWhenTested`

```java
@Test
void ensureAddingNewWeatherDataVoidsTestWhenTested() {
    final FlightPlan plan = testedPlan();
    plan.addWeatherData(10L);
    assertEquals(FlightPlanStatus.VALIDATED, plan.status());
}
```

**Test:** `ensureReAddingSameWeatherDataDoesNotVoidTest`

```java
@Test
void ensureReAddingSameWeatherDataDoesNotVoidTest() {
    final FlightPlan plan = testedPlan();
    plan.addWeatherData(10L);   // voids: TESTED -> VALIDATED
    plan.markTested();          // tested again with the weather data in place
    plan.addWeatherData(10L);   // same id -> no-op, must NOT void
    assertEquals(FlightPlanStatus.TESTED, plan.status());
}
```

**Test:** `ensureAddingWeatherDataDoesNotChangeDraftStatus`

```java
@Test
void ensureAddingWeatherDataDoesNotChangeDraftStatus() {
    final FlightPlan plan = validDslPlan(); // DRAFT
    plan.addWeatherData(10L);
    assertEquals(FlightPlanStatus.DRAFT, plan.status());
}
```

**Test:** `ensureAddingWeatherDataDoesNotChangeValidatedStatus`

```java
@Test
void ensureAddingWeatherDataDoesNotChangeValidatedStatus() {
    final FlightPlan plan = validDslPlan();
    plan.markValidated(); // VALIDATED
    plan.addWeatherData(10L);
    assertEquals(FlightPlanStatus.VALIDATED, plan.status());
}
```

---

## Coverage by Acceptance Criterion

- **AC082.1** (only an authenticated PILOT may insert weather data): enforced by the controller via `authz.ensureAuthenticatedUserHasAnyOf(PILOT)`; validated by the manual acceptance test "Authorization".
- **AC082.2** (the flight plan must be the pilot's own): enforced by the controller (`pilotId.equals(plan.assignedPilotId())`); validated by the manual acceptance test for AC082.2.
- **AC082.3** (flight plan must exist): validated by the controller (`FlightPlanRepository.ofIdentity` orElseThrow); covered by the manual acceptance test for AC082.3.
- **AC082.4** (weather data must exist): validated by the controller (`WeatherDataRepository.ofIdentity` orElseThrow); covered by the manual acceptance test for AC082.4.
- **AC082.5** (weather data becomes associated): `ensureWeatherDataCanBeAdded`, `ensureMultipleWeatherDataRecordsCanBeAdded`, `ensureWeatherDataIdsAreUnmodifiable`.
- **AC082.6** (a previously tested plan has its test voided to `VALIDATED`): `ensureAddingNewWeatherDataVoidsTestWhenTested`; the "new data only" nuance by `ensureReAddingSameWeatherDataDoesNotVoidTest`.
- **AC082.7** (non-tested plans keep their status): `ensureAddingWeatherDataDoesNotChangeDraftStatus`, `ensureAddingWeatherDataDoesNotChangeValidatedStatus`.

Total: **8 automated unit tests** added for US082 on the `FlightPlan` aggregate. The full `aisafe.base` suite passes (690 tests at the time of writing). The JPA mapping of the new `@ElementCollection` was additionally validated by booting Hibernate against H2.

---

## Acceptance Tests

The following manual acceptance tests validate the user story end-to-end against the running system.

### Prerequisites

1. The H2 database server must be running (`start-h2.bat`).
2. The bootstrap must have been executed once (`run-bootstrap.bat`) so that the system has: a pilot (US075), at least one flight plan assigned to that pilot (US080), and at least one weather data record (US041/US042).
3. Run the application with `run-jpa.bat`.

---

### AC082.1 — Only an authenticated PILOT may insert weather data

**Steps:**

1. Login as a user without the `PILOT` role.
2. Inspect the main menu.

**Expected:** the `Flight Plans >` submenu is not shown — it is rendered based on the authenticated user's roles. A direct invocation by a non-pilot is rejected by the authorization service.

---

### AC082.2 — The flight plan must belong to the authenticated pilot

**Steps:**

1. Login as pilot `pilot1`.
2. Navigate to `Flight Plans > Insert Weather Data in a Flight`.
3. The list shows only `pilot1`'s flight plans. Attempt to enter the designator of a plan assigned to a different pilot.

**Expected:** `The flight plan does not belong to the authenticated pilot.` No weather data is attached.

---

### AC082.3 — The flight plan must exist

**Steps:**

1. Enter a designator that does not exist (e.g. `ZZ9999`).

**Expected:** `Flight plan not found: ZZ9999.`

---

### AC082.4 — The weather data must exist

**Steps:**

1. Select one of your flight plans.
2. Enter a weather data id that does not exist (e.g. `999999`).

**Expected:** `Weather data not found: 999999.` No change is made to the plan.

---

### AC082.5 — Weather data is associated with the flight plan

**Steps:**

1. Select one of your flight plans and a valid weather data id.

**Expected:** confirmation message; the summary shows the number of weather records attached increased by one. The association is persisted in `T_FLIGHT_PLAN_WEATHER_DATA`.

---

### AC082.6 — A previously tested plan has its test voided

**Steps:**

1. Use a flight plan currently in `TESTED` status.
2. Attach a new weather data record to it.

**Expected:** the operation succeeds and the plan's status is now `VALIDATED` (the test was voided). The summary shows `Status : VALIDATED`.

---

### AC082.6 (nuance) — Re-adding the same weather data does not void a passed test

**Steps:**

1. Attach weather data id `X` to a plan, then have it validated and tested again (so `X` is already attached and the plan is `TESTED`).
2. Attach the same weather data id `X` again.

**Expected:** the status remains `TESTED` — re-adding an already-attached record is a no-op and does not void the test.

---

### AC082.7 — Non-tested plans keep their status

**Steps:**

1. Use a flight plan in `DRAFT` (or `VALIDATED`) status.
2. Attach a weather data record.

**Expected:** the weather data is attached and the status is unchanged (`DRAFT` stays `DRAFT`; `VALIDATED` stays `VALIDATED`).

---

## Coverage matrix

| AC | Automated | Manual |
|----|-----------|--------|
| AC082.1 | — | Authorization |
| AC082.2 | — | "must belong to the authenticated pilot" |
| AC082.3 | — | "flight plan must exist" |
| AC082.4 | — | "weather data must exist" |
| AC082.5 | `FlightPlanTest` (add / multiple / unmodifiable) | "weather data is associated" |
| AC082.6 | `FlightPlanTest` (voids when tested; not on re-add) | "test voided" + "re-adding nuance" |
| AC082.7 | `FlightPlanTest` (draft / validated unchanged) | "non-tested plans keep status" |
