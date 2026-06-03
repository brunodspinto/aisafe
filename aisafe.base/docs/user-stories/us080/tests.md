# US080 — Tests and Coverage

## Scope

US080 covers the form-based creation of a flight plan by an authenticated pilot, for a route of their company, in `DRAFT` status. Testing is split between automated unit tests for the domain (the `FlightPlan` form-based invariants and the `FuelQuantity` and `FlightPlanDesignator` value objects) and manual acceptance tests for the complete end-to-end use case.

In line with the project's testing convention, the `Application` layer (`CreateFlightPlanController`) is not unit-tested — controllers are thin orchestrators that delegate to the domain and repositories, and are exercised end-to-end through the manual acceptance tests.

> The pre-existing DSL-based tests of `FlightPlan` (`ensureFromDslCreatesFlightPlanWithCorrectData`, etc.) belong to US081 and are not repeated here.

## Automated Tests

### `FuelQuantityTest`

Location: `src/test/java/aisafe/flightplan/domain/FuelQuantityTest.java`

**Test:** `ensureValidFuelQuantityCanBeCreated`

```java
@Test
void ensureValidFuelQuantityCanBeCreated() {
    final FuelQuantity fuel = FuelQuantity.valueOf(1500.0);
    assertEquals(1500.0, fuel.amount());
}
```

**Test:** `ensureZeroFuelIsRejected`

```java
@Test
void ensureZeroFuelIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> FuelQuantity.valueOf(0.0));
}
```

**Test:** `ensureNegativeFuelIsRejected`

```java
@Test
void ensureNegativeFuelIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> FuelQuantity.valueOf(-1.0));
}
```

**Test:** `ensureEqualAmountsAreEqual`

```java
@Test
void ensureEqualAmountsAreEqual() {
    assertEquals(FuelQuantity.valueOf(2000.0), FuelQuantity.valueOf(2000.0));
}
```

**Test:** `ensureDifferentAmountsAreNotEqual`

```java
@Test
void ensureDifferentAmountsAreNotEqual() {
    assertNotEquals(FuelQuantity.valueOf(2000.0), FuelQuantity.valueOf(2500.0));
}
```

**Test:** `ensureHashCodeIsConsistentWithEquals`

```java
@Test
void ensureHashCodeIsConsistentWithEquals() {
    assertEquals(FuelQuantity.valueOf(1800.0).hashCode(), FuelQuantity.valueOf(1800.0).hashCode());
}
```

**Test:** `ensureCompareToOrdersByAmount`

```java
@Test
void ensureCompareToOrdersByAmount() {
    assertTrue(FuelQuantity.valueOf(1000.0).compareTo(FuelQuantity.valueOf(2000.0)) < 0);
}
```

**Test:** `ensureToStringContainsAmount`

```java
@Test
void ensureToStringContainsAmount() {
    assertTrue(FuelQuantity.valueOf(1234.0).toString().contains("1234"));
}
```

**Test:** `ensureEqualsReturnsFalseForNull`

```java
@Test
void ensureEqualsReturnsFalseForNull() {
    assertNotEquals(null, FuelQuantity.valueOf(1000.0));
}
```

---

### `FlightPlanDesignatorTest`

Location: `src/test/java/aisafe/flightplan/domain/FlightPlanDesignatorTest.java`

Verifies the format rule `xxN(N)(N)(N)(a)` (2 uppercase letters, 1 to 4 digits, optional uppercase suffix) and the upper-case normalisation.

**Test:** `ensureValidDesignatorIsAccepted`

```java
@Test
void ensureValidDesignatorIsAccepted() {
    assertEquals("TP1234", FlightPlanDesignator.valueOf("TP1234").toString());
}
```

**Test:** `ensureSingleDigitDesignatorIsAccepted`

```java
@Test
void ensureSingleDigitDesignatorIsAccepted() {
    assertEquals("TP1", FlightPlanDesignator.valueOf("TP1").toString());
}
```

**Test:** `ensureOperationalSuffixIsAccepted`

```java
@Test
void ensureOperationalSuffixIsAccepted() {
    assertEquals("TP1234A", FlightPlanDesignator.valueOf("TP1234A").toString());
}
```

**Test:** `ensureDesignatorIsNormalisedToUpperCase`

```java
@Test
void ensureDesignatorIsNormalisedToUpperCase() {
    assertEquals("TP1234", FlightPlanDesignator.valueOf("tp1234").toString());
}
```

**Test:** `ensureNullIsRejected`

```java
@Test
void ensureNullIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> FlightPlanDesignator.valueOf(null));
}
```

**Test:** `ensureBlankIsRejected`

```java
@Test
void ensureBlankIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> FlightPlanDesignator.valueOf("   "));
}
```

**Test:** `ensureMissingDigitsIsRejected`

```java
@Test
void ensureMissingDigitsIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> FlightPlanDesignator.valueOf("TP"));
}
```

**Test:** `ensureSingleLetterPrefixIsRejected`

```java
@Test
void ensureSingleLetterPrefixIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> FlightPlanDesignator.valueOf("T1234"));
}
```

**Test:** `ensureTooManyDigitsIsRejected`

```java
@Test
void ensureTooManyDigitsIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> FlightPlanDesignator.valueOf("TP12345"));
}
```

**Test:** `ensureEqualDesignatorsAreEqual`

```java
@Test
void ensureEqualDesignatorsAreEqual() {
    assertEquals(FlightPlanDesignator.valueOf("TP1234"), FlightPlanDesignator.valueOf("tp1234"));
}
```

**Test:** `ensureDifferentDesignatorsAreNotEqual`

```java
@Test
void ensureDifferentDesignatorsAreNotEqual() {
    assertNotEquals(FlightPlanDesignator.valueOf("TP1234"), FlightPlanDesignator.valueOf("TP5678"));
}
```

---

### `FlightPlanTest` (form-based creation)

Location: `src/test/java/aisafe/flightplan/domain/FlightPlanTest.java`

> Note: these tests use the helpers `validFormPlan()`, `futureDeparture()` and the constants `ROUTE` (`new RouteName("TP123")`), `AIRCRAFT` (`RegistrationNumber.valueOf("CS-TUA")`) and `PILOT_ID` (`1L`).

**Test:** `ensureFormBasedFlightPlanCanBeCreated`

```java
@Test
void ensureFormBasedFlightPlanCanBeCreated() {
    final FlightPlan plan = validFormPlan();
    assertEquals("TP1234", plan.designator());
    assertEquals(REGULAR, plan.flightType());
    assertEquals(FlightPlanStatus.DRAFT, plan.status());
}
```

**Test:** `ensureFormBasedPlanStoresAllFields`

```java
@Test
void ensureFormBasedPlanStoresAllFields() {
    final FlightPlan plan = validFormPlan();
    assertEquals(ROUTE, plan.routeName());
    assertEquals(AIRCRAFT, plan.aircraftRegistration());
    assertEquals(PILOT_ID, plan.assignedPilotId());
    assertEquals(FuelQuantity.valueOf(1500.0), plan.fuelQuantity());
    assertNotNull(plan.departureDateTime());
}
```

**Test:** `ensureFormBasedPlanHasNullDslContent`

```java
@Test
void ensureFormBasedPlanHasNullDslContent() {
    assertNull(validFormPlan().dslContent());
}
```

**Test:** `ensureFormPlanRejectsNullRoute`

```java
@Test
void ensureFormPlanRejectsNullRoute() {
    assertThrows(IllegalArgumentException.class, () ->
            new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), REGULAR,
                    null, AIRCRAFT, PILOT_ID, futureDeparture(), FuelQuantity.valueOf(1500.0)));
}
```

**Test:** `ensureFormPlanRejectsNullAircraft`

```java
@Test
void ensureFormPlanRejectsNullAircraft() {
    assertThrows(IllegalArgumentException.class, () ->
            new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), REGULAR,
                    ROUTE, null, PILOT_ID, futureDeparture(), FuelQuantity.valueOf(1500.0)));
}
```

**Test:** `ensureFormPlanRejectsNullPilot`

```java
@Test
void ensureFormPlanRejectsNullPilot() {
    assertThrows(IllegalArgumentException.class, () ->
            new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), REGULAR,
                    ROUTE, AIRCRAFT, null, futureDeparture(), FuelQuantity.valueOf(1500.0)));
}
```

**Test:** `ensureFormPlanRejectsNullDeparture`

```java
@Test
void ensureFormPlanRejectsNullDeparture() {
    assertThrows(IllegalArgumentException.class, () ->
            new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), REGULAR,
                    ROUTE, AIRCRAFT, PILOT_ID, null, FuelQuantity.valueOf(1500.0)));
}
```

**Test:** `ensureFormPlanRejectsPastDeparture`

```java
@Test
void ensureFormPlanRejectsPastDeparture() {
    assertThrows(IllegalArgumentException.class, () ->
            new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), REGULAR,
                    ROUTE, AIRCRAFT, PILOT_ID, LocalDateTime.now().minusDays(1), FuelQuantity.valueOf(1500.0)));
}
```

**Test:** `ensureFormPlanRejectsNullFuel`

```java
@Test
void ensureFormPlanRejectsNullFuel() {
    assertThrows(IllegalArgumentException.class, () ->
            new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), REGULAR,
                    ROUTE, AIRCRAFT, PILOT_ID, futureDeparture(), null));
}
```

**Test:** `ensureFormPlanRejectsNullFlightType`

```java
@Test
void ensureFormPlanRejectsNullFlightType() {
    assertThrows(IllegalArgumentException.class, () ->
            new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), (FlightType) null,
                    ROUTE, AIRCRAFT, PILOT_ID, futureDeparture(), FuelQuantity.valueOf(1500.0)));
}
```

---

## Coverage by Acceptance Criterion

- **AC080.1** (only an authenticated PILOT may register): enforced by the controller via `authz.ensureAuthenticatedUserHasAnyOf(PILOT)`; validated by the manual acceptance test "Authorization".
- **AC080.2** (route must exist): validated by the controller (`FlightRouteRepository.ofIdentity` orElseThrow); covered by the manual acceptance test for AC080.2.
- **AC080.3** (assigned pilot of the route's company): validated by the controller; covered by the manual acceptance test for AC080.3.
- **AC080.4** (aircraft must exist): validated by the controller (`AircraftRepository.ofIdentity` orElseThrow); covered by the manual acceptance test for AC080.4.
- **AC080.5** (fuel strictly positive): `ensureZeroFuelIsRejected`, `ensureNegativeFuelIsRejected`, `ensureValidFuelQuantityCanBeCreated`.
- **AC080.6** (departure in the future): `ensureFormPlanRejectsPastDeparture`, `ensureFormPlanRejectsNullDeparture`.
- **AC080.7** (status starts as DRAFT): `ensureFormBasedFlightPlanCanBeCreated`.
- **AC080.8** (unique identifier + format): uniqueness validated by the controller and the manual acceptance test; format covered by `FlightPlanDesignatorTest` (valid forms accepted, malformed rejected).
- **AC080.9** (aircraft of the route's company): validated by the controller; covered by the manual acceptance test for AC080.9.
- **AC080.10** (aircraft ACTIVE): validated by the controller; covered by the manual acceptance test for AC080.10.
- Aggregate invariants / field storage: `ensureFormBasedPlanStoresAllFields`, `ensureFormBasedPlanHasNullDslContent`, `ensureFormPlanRejectsNullRoute`, `ensureFormPlanRejectsNullAircraft`, `ensureFormPlanRejectsNullPilot`, `ensureFormPlanRejectsNullFlightType`, `ensureFormPlanRejectsNullFuel`.

Total: **9 (`FuelQuantity`) + 11 (`FlightPlanDesignator`) + 10 (form-based `FlightPlan`) = 30 automated unit tests** added for US080. The full `aisafe.base` suite (548 tests) passes. The JPA mapping of the extended `FlightPlan` was additionally validated by booting Hibernate against H2.

---

## Acceptance Tests

The following manual acceptance tests validate the user story end-to-end against the running system.

### Prerequisites

1. The H2 database server must be running (`start-h2.bat`).
2. The bootstrap must have been executed once (`run-bootstrap.bat`) so that the system has: an Air Transport Company (e.g. `TP`), at least one active aircraft in its fleet (e.g. `CS-TUA`), at least one active pilot of that company (US075), and at least one active flight route of that company (e.g. `TP123`, US073).
3. Run the application with `run-jpa.bat`.

---

### AC080.1 — Only an authenticated PILOT may register a flight plan

**Steps:**

1. Login as a user without the `PILOT` role (e.g. a Backoffice Operator).
2. Inspect the main menu.

**Expected:** the `Flight Plans >` submenu is not shown — the menu is rendered based on the authenticated user's roles. A direct invocation by a non-pilot is rejected by the authorization service.

---

### AC080.2 / AC080.7 / AC080.8 — Create a valid flight plan (happy path)

**Steps:**

1. Login as a pilot (e.g. `pilot1`).
2. Navigate to `Flight Plans > Create Flight Plan`.
3. Enter an existing route of your company (e.g. `TP123`).
4. Enter an aircraft of your company's fleet (e.g. `CS-TUA`).
5. Enter an assigned pilot id from the listed pilots.
6. Choose the flight type (e.g. `REGULAR`).
7. Enter a new designator (e.g. `TP1234`), a future departure (e.g. `2026-07-01T14:30`), and a positive fuel quantity (e.g. `15000`).

**Expected:** confirmation message; the plan is persisted with `status = DRAFT` (AC080.7), associated with the route (AC080.2), under the unique designator (AC080.8).

---

### AC080.3 — Assigned pilot must belong to the route's company

**Steps:**

1. Attempt to create a flight plan assigning a pilot that does not belong to the route's company.

**Expected:** `The assigned pilot does not belong to the route's company.` No plan is created.

---

### AC080.4 — Aircraft must exist

**Steps:**

1. Enter an aircraft registration that is not registered in the system.

**Expected:** `Aircraft not found: <registration>.` No plan is created.

(Likewise, an unknown route produces `Flight route not found: ...` and an unknown pilot produces `Pilot not found: ...`.)

---

### AC080.5 — Fuel quantity must be strictly positive

**Steps:**

1. Enter a fuel quantity of `0` or a negative value.

**Expected:** `Fuel quantity must be strictly positive.` No plan is created.

---

### AC080.6 — Departure date/time must be in the future

**Steps:**

1. Enter a departure date/time in the past.

**Expected:** `Departure date/time must be in the future.` No plan is created.

---

### AC080.8 — Designator must be unique and well-formed

**Steps (uniqueness):**

1. Create a flight plan with designator `TP1234`.
2. Attempt to create another plan with the same designator `TP1234`.

**Expected:** `A flight plan with designator 'TP1234' already exists.`

**Steps (format):**

1. Enter a malformed designator (e.g. `T1234` or `TP12345`).

**Expected:** a format validation error from `FlightPlanDesignator` (`xxN(N)(N)(N)(a)`).

---

### AC080.9 — Aircraft must belong to the route's company

**Steps:**

1. Attempt to assign an aircraft that is not in the route company's fleet.

**Expected:** `The aircraft does not belong to the route's company.` No plan is created.

---

### AC080.10 — Aircraft must be ACTIVE

**Steps:**

1. Decommission an aircraft (US071) and then attempt to assign it to a flight plan.

**Expected:** `The aircraft is not active and cannot be assigned to a flight plan.` No plan is created.

---

## Coverage matrix

| AC | Automated | Manual |
|----|-----------|--------|
| AC080.1 | — | Authorization |
| AC080.2 | — | "Create a valid flight plan" |
| AC080.3 | — | "Assigned pilot must belong to the route's company" |
| AC080.4 | — | "Aircraft must exist" |
| AC080.5 | `FuelQuantityTest` | "Fuel quantity must be strictly positive" |
| AC080.6 | `FlightPlanTest` (past/null departure) | "Departure date/time must be in the future" |
| AC080.7 | `FlightPlanTest` (DRAFT) | "Create a valid flight plan" |
| AC080.8 | `FlightPlanDesignatorTest` (format) | "Designator must be unique and well-formed" |
| AC080.9 | — | "Aircraft must belong to the route's company" |
| AC080.10 | — | "Aircraft must be ACTIVE" |
