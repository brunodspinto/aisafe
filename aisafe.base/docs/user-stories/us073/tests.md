# US073 — Tests and Coverage

## Scope

US073 covers creating a flight route for an Air Transport Company, with a unique name following the format `[A-Z]{2}[0-9]{1,4}`, between two distinct registered airports.

---

## Automated Tests

### `RouteNameTest`

Location: `src/test/java/aisafe/flightroute/domain/RouteNameTest.java`

> Note: tests the `RouteName` value object in isolation.

**Test:** `ensureValidRouteNameWithOneDigitIsAccepted`

```java
@Test
void ensureValidRouteNameWithOneDigitIsAccepted() {
    final RouteName name = new RouteName("TP1");
    assertEquals("TP1", name.toString());
}
```

**Test:** `ensureValidRouteNameWithFourDigitsIsAccepted`

```java
@Test
void ensureValidRouteNameWithFourDigitsIsAccepted() {
    final RouteName name = new RouteName("TP1234");
    assertEquals("TP1234", name.toString());
}
```

**Test:** `ensureRouteNameWithOnlyOneLetterIsRejected`

```java
@Test
void ensureRouteNameWithOnlyOneLetterIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new RouteName("T123"));
}
```

**Test:** `ensureRouteNameWithMoreThanTwoLettersIsRejected`

```java
@Test
void ensureRouteNameWithMoreThanTwoLettersIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new RouteName("TAP123"));
}
```

**Test:** `ensureRouteNameWithNoDigitsIsRejected`

```java
@Test
void ensureRouteNameWithNoDigitsIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new RouteName("TP"));
}
```

**Test:** `ensureRouteNameWithMoreThanFourDigitsIsRejected`

```java
@Test
void ensureRouteNameWithMoreThanFourDigitsIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new RouteName("TP12345"));
}
```

**Test:** `ensureRouteNameWithLowercaseLettersIsRejected`

```java
@Test
void ensureRouteNameWithLowercaseLettersIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new RouteName("tp123"));
}
```

**Test:** `ensureNullRouteNameIsRejected`

```java
@Test
void ensureNullRouteNameIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new RouteName(null));
}
```

**Test:** `ensureBlankRouteNameIsRejected`

```java
@Test
void ensureBlankRouteNameIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new RouteName("   "));
}
```

**Test:** `ensureTwoRouteNamesWithSameValueAreEqual`

```java
@Test
void ensureTwoRouteNamesWithSameValueAreEqual() {
    final RouteName a = new RouteName("TP123");
    final RouteName b = new RouteName("TP123");
    assertEquals(a, b);
}
```

**Test:** `ensureTwoRouteNamesWithDifferentValuesAreNotEqual`

```java
@Test
void ensureTwoRouteNamesWithDifferentValuesAreNotEqual() {
    final RouteName a = new RouteName("TP123");
    final RouteName b = new RouteName("TP124");
    assertNotEquals(a, b);
}
```

**Test:** `ensureHashCodeIsConsistentWithEquals`

```java
@Test
void ensureHashCodeIsConsistentWithEquals() {
    final RouteName a = new RouteName("TP123");
    final RouteName b = new RouteName("TP123");
    assertEquals(a.hashCode(), b.hashCode());
}
```

**Test:** `ensureToStringReturnsName`

```java
@Test
void ensureToStringReturnsName() {
    final RouteName name = new RouteName("TP123");
    assertEquals("TP123", name.toString());
}
```

---

### `FlightRouteTest`

Location: `src/test/java/aisafe/flightroute/domain/FlightRouteTest.java`

> Note: tests use helper methods `validCompany()`, `validOrigin()`, and `validDestination()` defined in the same class.

```java
private static AirTransportCompany validCompany() {
    return new AirTransportCompany("TAP Air Portugal",
            IATACode.valueOf("TP"), ICAOCode.valueOf("TAP"));
}

private static AirportIATACode validOrigin() {
    return new AirportIATACode("OPO");
}

private static AirportIATACode validDestination() {
    return new AirportIATACode("LIS");
}
```

**Test:** `ensureValidFlightRouteIsCreatedSuccessfully`

```java
@Test
void ensureValidFlightRouteIsCreatedSuccessfully() {
    final FlightRoute route = new FlightRoute(
            new RouteName("TP123"), validOrigin(), validDestination(), validCompany());
    assertEquals("TP123", route.routeName().toString());
    assertEquals("OPO", route.originAirport().code());
    assertEquals("LIS", route.destinationAirport().code());
}
```

**Test:** `ensureNewRouteHasActiveStatus`

```java
@Test
void ensureNewRouteHasActiveStatus() {
    final FlightRoute route = new FlightRoute(
            new RouteName("TP123"), validOrigin(), validDestination(), validCompany());
    assertEquals(FlightRouteStatus.ACTIVE, route.status());
}
```

**Test:** `ensureRouteNameCannotBeNull`

```java
@Test
void ensureRouteNameCannotBeNull() {
    assertThrows(IllegalArgumentException.class, () ->
            new FlightRoute(null, validOrigin(), validDestination(), validCompany()));
}
```

**Test:** `ensureOriginAirportCannotBeNull`

```java
@Test
void ensureOriginAirportCannotBeNull() {
    assertThrows(IllegalArgumentException.class, () ->
            new FlightRoute(new RouteName("TP123"), null, validDestination(), validCompany()));
}
```

**Test:** `ensureDestinationAirportCannotBeNull`

```java
@Test
void ensureDestinationAirportCannotBeNull() {
    assertThrows(IllegalArgumentException.class, () ->
            new FlightRoute(new RouteName("TP123"), validOrigin(), null, validCompany()));
}
```

**Test:** `ensureCompanyCannotBeNull`

```java
@Test
void ensureCompanyCannotBeNull() {
    assertThrows(IllegalArgumentException.class, () ->
            new FlightRoute(new RouteName("TP123"), validOrigin(), validDestination(), null));
}
```

**Test:** `ensureOriginAndDestinationCannotBeTheSame`

```java
@Test
void ensureOriginAndDestinationCannotBeTheSame() {
    assertThrows(IllegalArgumentException.class, () ->
            new FlightRoute(new RouteName("TP123"),
                    new AirportIATACode("OPO"),
                    new AirportIATACode("OPO"),
                    validCompany()));
}
```

**Test:** `ensureIdentityIsRouteName`

```java
@Test
void ensureIdentityIsRouteName() {
    final RouteName name = new RouteName("TP123");
    final FlightRoute route = new FlightRoute(name, validOrigin(), validDestination(), validCompany());
    assertEquals(name, route.identity());
}
```

**Test:** `ensureTwoRoutesWithSameNameAreEqual`

```java
@Test
void ensureTwoRoutesWithSameNameAreEqual() {
    final FlightRoute r1 = new FlightRoute(
            new RouteName("TP123"), validOrigin(), validDestination(), validCompany());
    final FlightRoute r2 = new FlightRoute(
            new RouteName("TP123"), new AirportIATACode("FAO"), new AirportIATACode("MAD"), validCompany());
    assertEquals(r1, r2);
}
```

**Test:** `ensureTwoRoutesWithDifferentNamesAreNotEqual`

```java
@Test
void ensureTwoRoutesWithDifferentNamesAreNotEqual() {
    final FlightRoute r1 = new FlightRoute(
            new RouteName("TP123"), validOrigin(), validDestination(), validCompany());
    final FlightRoute r2 = new FlightRoute(
            new RouteName("TP124"), validOrigin(), validDestination(), validCompany());
    assertNotEquals(r1, r2);
}
```

**Test:** `ensureHashCodeIsConsistentWithEquals`

```java
@Test
void ensureHashCodeIsConsistentWithEquals() {
    final FlightRoute r1 = new FlightRoute(
            new RouteName("TP123"), validOrigin(), validDestination(), validCompany());
    final FlightRoute r2 = new FlightRoute(
            new RouteName("TP123"), validOrigin(), validDestination(), validCompany());
    assertEquals(r1.hashCode(), r2.hashCode());
}
```

**Test:** `ensureEqualsReturnsFalseForNull`

```java
@Test
void ensureEqualsReturnsFalseForNull() {
    final FlightRoute route = new FlightRoute(
            new RouteName("TP123"), validOrigin(), validDestination(), validCompany());
    assertNotEquals(null, route);
}
```

**Test:** `ensureSameAsReturnsTrueForEqualRoutes`

```java
@Test
void ensureSameAsReturnsTrueForEqualRoutes() {
    final FlightRoute r1 = new FlightRoute(
            new RouteName("TP123"), validOrigin(), validDestination(), validCompany());
    final FlightRoute r2 = new FlightRoute(
            new RouteName("TP123"), validOrigin(), validDestination(), validCompany());
    assertTrue(r1.sameAs(r2));
}
```

**Test:** `ensureToStringContainsRouteName`

```java
@Test
void ensureToStringContainsRouteName() {
    final FlightRoute route = new FlightRoute(
            new RouteName("TP123"), validOrigin(), validDestination(), validCompany());
    assertTrue(route.toString().contains("TP123"));
}
```

**Test:** `ensureGettersReturnCorrectValues`

```java
@Test
void ensureGettersReturnCorrectValues() {
    final AirTransportCompany company = validCompany();
    final FlightRoute route = new FlightRoute(
            new RouteName("TP123"), validOrigin(), validDestination(), company);
    assertEquals("OPO", route.originAirport().code());
    assertEquals("LIS", route.destinationAirport().code());
    assertEquals(company, route.company());
    assertEquals(FlightRouteStatus.ACTIVE, route.status());
}
```

---

## Coverage by Acceptance Criterion

- **AC073.1** (both airports must be registered): `ensureOriginAirportCannotBeNull`, `ensureDestinationAirportCannotBeNull`; airport existence validated at controller level via `AirportRepository` (manual test).
- **AC073.2** (origin != destination): `ensureOriginAndDestinationCannotBeTheSame`.
- **AC073.3** (route name format `[A-Z]{2}[0-9]{1,4}`): `ensureValidRouteNameWithOneDigitIsAccepted`, `ensureValidRouteNameWithFourDigitsIsAccepted`, `ensureRouteNameWithOnlyOneLetterIsRejected`, `ensureRouteNameWithMoreThanTwoLettersIsRejected`, `ensureRouteNameWithNoDigitsIsRejected`, `ensureRouteNameWithMoreThanFourDigitsIsRejected`, `ensureRouteNameWithLowercaseLettersIsRejected`, `ensureNullRouteNameIsRejected`, `ensureBlankRouteNameIsRejected`.
- **AC073.4** (route name unique): `ensureTwoRoutesWithSameNameAreEqual`, `ensureTwoRouteNamesWithSameValueAreEqual`; uniqueness enforced by `@EmbeddedId` at persistence level and pre-checked in controller via `existsByName` (manual test).
- **AC073.5** (ATCC role): Controller checks `ATCC` role via `AuthorizationService`; validated by manual test.
- **AC073.6** (company from session): Company resolved from authenticated user in controller; validated by manual test.
- Domain invariants (construction): `ensureValidFlightRouteIsCreatedSuccessfully`, `ensureNewRouteHasActiveStatus`, `ensureRouteNameCannotBeNull`, `ensureCompanyCannotBeNull`, `ensureIdentityIsRouteName`, `ensureGettersReturnCorrectValues`, `ensureToStringContainsRouteName`, `ensureEqualsReturnsFalseForNull`, `ensureSameAsReturnsTrueForEqualRoutes`, `ensureHashCodeIsConsistentWithEquals`.

---

## Acceptance Tests

Role enforcement (AC073.5), airport existence validation (AC073.1), and route name uniqueness at persistence level (AC073.4) are infrastructure concerns validated by manual integration testing. All domain invariants and value object validations are fully covered by the automated unit tests above.

**Manual test — AC073.1 / AC073.2 / AC073.3 (full creation flow):**

1. Run `AiSafeApp` and login as an Air Transport Company Collaborator (ATCC role).
2. Navigate to `Flight Routes > Create Flight Route`.
3. Provide: route name `TP123`, origin `OPO`, destination `LIS`.
4. Expected: confirmation message `Flight route 'TP123' (OPO → LIS) created successfully.` and the route visible in the list.

**Manual test — AC073.1 (unknown airport rejected):**

1. At the origin or destination prompt, enter an IATA code not registered in the system (e.g. `ZZZ`).
2. Expected: the system rejects the operation with an error message indicating the airport does not exist.

**Manual test — AC073.2 (same origin and destination rejected):**

1. Provide the same IATA code for both origin and destination (e.g. `OPO` and `OPO`).
2. Expected: the system rejects the operation with a validation error stating origin and destination must be different.

**Manual test — AC073.3 (invalid route name format rejected):**

1. At the route name prompt, enter `T123` (one letter), `TAP123` (three letters), `TP` (no digits), or `TP12345` (five digits).
2. Expected: the system rejects each input with a format validation error and re-prompts for a valid name.

**Manual test — AC073.4 (duplicate route name rejected):**

1. Create a route `TP123` successfully (steps above).
2. Attempt to create a second route also named `TP123`.
3. Expected: the system rejects the operation with a message indicating the route name is already in use.

**Manual test — AC073.5 (role enforcement):**

1. Login as a user without the ATCC role (e.g. Admin or Backoffice Operator).
2. Expected: the Create Flight Route option is not available in the menu.
