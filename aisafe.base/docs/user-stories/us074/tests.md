# US074 — Tests and Coverage

## Scope

US074 covers deactivating a flight route from a given date onwards (soft delete). The route transitions from `ACTIVE` to `INACTIVE`, its `activeUntil` date is set, and the operation is rejected if planned flights exist on or after the chosen date.

## Automated Tests

### `FlightRouteTest`

Location: `src/test/java/aisafe/flightroute/domain/FlightRouteTest.java`

> Note: tests use a helper method `validFlightRoute()` that builds a minimal `ACTIVE` `FlightRoute` with a sample `RouteName` and two `AirportIATACode` values.

**Test:** `ensureFlightRouteIsCreatedWithActiveStatus`

```java
@Test
void ensureFlightRouteIsCreatedWithActiveStatus() {
    final FlightRoute route = validFlightRoute();
    assertEquals(FlightRouteStatus.ACTIVE, route.status());
}
```

**Test:** `ensureActiveUntilIsNullForNewRoute`

```java
@Test
void ensureActiveUntilIsNullForNewRoute() {
    final FlightRoute route = validFlightRoute();
    assertNull(route.activeUntil());
}
```

**Test:** `ensureIsActiveReturnsTrueForActiveRoute`

```java
@Test
void ensureIsActiveReturnsTrueForActiveRoute() {
    final FlightRoute route = validFlightRoute();
    assertTrue(route.isActive());
}
```

**Test:** `ensureDeactivateSetsStatusToInactive`

```java
@Test
void ensureDeactivateSetsStatusToInactive() {
    final FlightRoute route = validFlightRoute();
    route.deactivate(LocalDate.of(2025, 8, 1));
    assertEquals(FlightRouteStatus.INACTIVE, route.status());
}
```

**Test:** `ensureDeactivateSetsActiveUntilDate`

```java
@Test
void ensureDeactivateSetsActiveUntilDate() {
    final LocalDate date = LocalDate.of(2025, 8, 1);
    final FlightRoute route = validFlightRoute();
    route.deactivate(date);
    assertEquals(date, route.activeUntil());
}
```

**Test:** `ensureIsActiveReturnsFalseAfterDeactivation`

```java
@Test
void ensureIsActiveReturnsFalseAfterDeactivation() {
    final FlightRoute route = validFlightRoute();
    route.deactivate(LocalDate.of(2025, 8, 1));
    assertFalse(route.isActive());
}
```

**Test:** `ensureDeactivateWithNullDateThrows`

```java
@Test
void ensureDeactivateWithNullDateThrows() {
    final FlightRoute route = validFlightRoute();
    assertThrows(IllegalArgumentException.class, () -> route.deactivate(null));
}
```

**Test:** `ensureCannotDeactivateAlreadyInactiveRoute`

```java
@Test
void ensureCannotDeactivateAlreadyInactiveRoute() {
    final FlightRoute route = validFlightRoute();
    route.deactivate(LocalDate.of(2025, 8, 1));
    assertThrows(IllegalStateException.class,
            () -> route.deactivate(LocalDate.of(2025, 9, 1)));
}
```

**Test:** `ensureIdentityReturnsRouteName`

```java
@Test
void ensureIdentityReturnsRouteName() {
    final FlightRoute route = validFlightRoute();
    assertEquals(RouteName.valueOf("LIS-OPO"), route.identity());
}
```

**Test:** `ensureTwoRoutesWithSameNameAreEqual`

```java
@Test
void ensureTwoRoutesWithSameNameAreEqual() {
    final FlightRoute a = validFlightRoute();
    final FlightRoute b = validFlightRoute();
    assertEquals(a, b);
}
```

---

### `RouteNameTest`

Location: `src/test/java/aisafe/flightroute/domain/RouteNameTest.java`

**Test:** `ensureRouteNameCannotBeNull`

```java
@Test
void ensureRouteNameCannotBeNull() {
    assertThrows(IllegalArgumentException.class, () -> RouteName.valueOf(null));
}
```

**Test:** `ensureRouteNameCannotBeBlank`

```java
@Test
void ensureRouteNameCannotBeBlank() {
    assertThrows(IllegalArgumentException.class, () -> RouteName.valueOf("   "));
}
```

**Test:** `ensureValidRouteNameIsAccepted`

```java
@Test
void ensureValidRouteNameIsAccepted() {
    final RouteName name = RouteName.valueOf("LIS-OPO");
    assertEquals("LIS-OPO", name.toString());
}
```

**Test:** `ensureRouteNamesWithSameValueAreEqual`

```java
@Test
void ensureRouteNamesWithSameValueAreEqual() {
    assertEquals(RouteName.valueOf("LIS-OPO"), RouteName.valueOf("LIS-OPO"));
}
```

**Test:** `ensureRouteNamesWithDifferentValuesAreNotEqual`

```java
@Test
void ensureRouteNamesWithDifferentValuesAreNotEqual() {
    assertNotEquals(RouteName.valueOf("LIS-OPO"), RouteName.valueOf("LIS-CDG"));
}
```

---

## Coverage by Acceptance Criterion

- AC074.1: `ensureDeactivateSetsStatusToInactive`, `ensureDeactivateSetsActiveUntilDate`, `ensureIsActiveReturnsFalseAfterDeactivation`, `ensureActiveUntilIsNullForNewRoute`
- AC074.2: No new flights may be scheduled on an inactive route — enforced in US080 (Create a Flight Plan); validated by manual integration test
- AC074.3: Planned-flight conflict check orchestrated by `DeactivateFlightRouteController` via `FlightRepository.hasFlightsAfter()`; validated by manual test
- AC074.4: Controller checks `ATCC` role via `AuthorizationService` and verifies route ownership against the logged-in user's company; validated by manual test
- Domain invariants (construction and state transitions): `ensureFlightRouteIsCreatedWithActiveStatus`, `ensureActiveUntilIsNullForNewRoute`, `ensureIsActiveReturnsTrueForActiveRoute`, `ensureDeactivateWithNullDateThrows`, `ensureCannotDeactivateAlreadyInactiveRoute`, `ensureIdentityReturnsRouteName`, `ensureTwoRoutesWithSameNameAreEqual`
- `RouteName` value object: `ensureRouteNameCannotBeNull`, `ensureRouteNameCannotBeBlank`, `ensureValidRouteNameIsAccepted`, `ensureRouteNamesWithSameValueAreEqual`, `ensureRouteNamesWithDifferentValuesAreNotEqual`

---

## Acceptance Tests

Role enforcement, company-ownership verification (AC074.4), and the planned-flight conflict check (AC074.3) are infrastructure concerns validated by manual integration testing. Domain state transitions are fully covered by the automated unit tests above.

**Manual test — AC074.1 / AC074.4 (successful deactivation flow):**

1. Run `AiSafeApp` and login as an ATCC user (e.g., `atcc1` / `Password1`).
2. Navigate to `Flight Routes > Deactivate Flight Route`.
3. The system lists the active routes of the ATCC's company, e.g.:
   ```
   [1] LIS → OPO  (TAP Air Portugal)  — ACTIVE
   [2] LIS → CDG  (TAP Air Portugal)  — ACTIVE
   ```
4. Select route `1` and enter deactivation date `2025-08-01`.
5. Expected: the system confirms `Route 'LIS → OPO' deactivated from 2025-08-01 onwards.` and the route is no longer listed as ACTIVE.

**Manual test — AC074.3 (rejection when planned flights exist):**

1. Ensure a planned flight exists on route `LIS → OPO` with departure on or after `2025-08-01`.
2. Attempt to deactivate route `LIS → OPO` from `2025-08-01`.
3. Expected: the system rejects the request with the message `Cannot deactivate: there are planned flights on this route from 2025-08-01 onwards.` and the route remains `ACTIVE`.

**Manual test — AC074.2 (no new flights on deactivated route):**

1. Deactivate route `LIS → OPO` from `2025-08-01`.
2. In US080, attempt to create a flight plan on that route with departure date `2025-09-15`.
3. Expected: US080 rejects the operation because the route is `INACTIVE` from `2025-08-01` onwards.

**Manual test — AC074.4 (role enforcement):**

1. Login as a user without the ATCC role (e.g., Backoffice Operator or Admin).
2. Expected: the Deactivate Flight Route option is not available in the menu.

**Manual test — AC074.4 (cross-company ownership rejected):**

1. Login as an ATCC whose company is `RYR` (Ryanair).
2. Attempt to deactivate a route that belongs to company `TP` (TAP Air Portugal).
3. Expected: the route is not visible in the ATCC's route listing; the system treats it as not found.