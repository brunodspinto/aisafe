# US073 — Create a Flight Route

## 1. Context

This US is being implemented for the first time in Sprint 3. It allows an **Air Transport Company Collaborator (ATCC)** to create a new flight route for their company, defining the origin and destination airports and assigning a unique route name that follows the required naming convention.

`FlightRoute` is a new aggregate in the domain. It depends on `Airport` (US052) and `AirTransportCompany` (US060) being already registered in the system. Flight routes are a foundational prerequisite for US080 (Create a Flight Plan), since every flight plan is instantiated from a route.

### 1.1 List of issues

- **Analysis:** Define the domain rules for `FlightRoute`, including route name format and uniqueness constraints.
- **Design:** Define the architecture for flight route creation — domain model, persistence, and layers.
- **Implement:** Implement the `FlightRoute` aggregate, `RouteName` value object, repository, controller, and UI.
- **Test:** Unit tests for `FlightRoute` and `RouteName` (domain package coverage above 90%).

---

## 2. Requirements

**US073** As an Air Transport Company Collaborator, I want to add a flight route for my company, so that pilots can use it as the basis for creating flight plans.

**Acceptance Criteria:**

- **AC073.1** A route must be defined between exactly two airports (origin and destination), both already registered in the system.
- **AC073.2** Origin and destination airports must be different.
- **AC073.3** The route name must follow the format: 2 uppercase letters (company initials) followed by 1 to 4 digits (e.g. `TP123`). The format is validated by the `RouteName` value object.
- **AC073.4** The route name must be unique within the system.
- **AC073.5** Only an authenticated Air Transport Company Collaborator (`ATCC` role) may perform this action.
- **AC073.6** The collaborator can only create routes for their own company — the company is derived from the authenticated user's session, not provided as input.

**Dependencies/References:**

- **US030** — Authentication and authorization must be in place (role `ATCC`).
- **US052** — Create an Airport. Both airports referenced by the route must already be registered.
- **US060** — Register an Air Transport Company. The company must exist before a route can be created.

---

## 3. Analysis

A flight route represents a named connection between two airports operated by a specific air transport company.

The main design decisions taken were:

**`RouteName` as a Value Object** — The project document specifies that a route name consists of the company's 2-letter initials followed by up to 4 numeric digits (e.g. `TP123`). A `RouteName` Value Object was created to encapsulate and enforce this format via regex validation (`[A-Z]{2}[0-9]{1,4}`). The `RouteName` is the natural business identity of the `FlightRoute` aggregate — consistent with how other business identities are modelled in the domain (e.g. `AirportIATACode`, `MecanographicNumber`, `RegistrationNumber`). This satisfies the DDD principle "business identity as Value Objects" (CO3).

**Route status** — A route can be `ACTIVE` or `INACTIVE`. A route is always created as `ACTIVE`. Deactivation is handled by US074. The `FlightRouteStatus` enum enables soft deactivation without deleting the aggregate — past references from flight plans remain valid.

**Airport references by identity** — The route references airports via `AirportIATACode` value objects rather than full `Airport` object references. This keeps the coupling between `FlightRoute` and `Airport` aggregates low — a DDD Low Coupling principle.

**Uniqueness** — Route name uniqueness is enforced at the controller level via a pre-check through the repository, and also at the database level with a unique constraint, to prevent race conditions.

**Company binding** — The company is not an input: it is resolved from the currently authenticated user's `ATCC` session. This guarantees AC073.6 structurally.

The main classes identified are:

| Class | Type | Responsibility |
|-------|------|----------------|
| `FlightRoute` | Entity / Aggregate Root | Holds route data and enforces invariants |
| `RouteName` | Value Object / Identity | Route name format validation (`[A-Z]{2}[0-9]{1,4}`) |
| `FlightRouteStatus` | Enum | `ACTIVE` / `INACTIVE` |
| `FlightRouteRepository` | Repository Interface | Persistence contract |
| `CreateFlightRouteController` | Application Controller | Orchestrates the use case; enforces `ATCC` role |
| `CreateFlightRouteUI` | UI | Collects route name, origin and destination from the user |

The following diagram shows the domain model excerpt relevant to this US:

![Domain Model](svg/US073-domain-model.svg)

---


## 4. Design

### 4.1. Realization

The use case follows the standard layered flow: `CreateFlightRouteUI` collects the route name, origin and destination airport IATA codes from the authenticated ATCC, then delegates to `CreateFlightRouteController`. The controller:

1. Verifies the authenticated user has the `ATCC` role
2. Resolves the authenticated collaborator's company from the session
3. Creates and validates the `RouteName` value object
4. Verifies route name uniqueness via the repository
5. Verifies both airports exist in the repository
6. Verifies origin and destination are different
7. Creates the `FlightRoute` aggregate in `ACTIVE` status
8. Persists via `FlightRouteRepository`

The following sequence diagram illustrates this flow:

![Sequence Diagram](svg/US073-SD.svg)

The following class diagram shows the classes involved:

![Class Diagram](svg/US073-class-diagram.svg)

### 4.2. Acceptance Tests

All tests are automated with JUnit 5 and located in `src/test/java/aisafe/flightroute/domain/`.

---

**AC073.1 — Route must reference two valid registered airports**

**Test:** `ensureOriginAirportCannotBeNull`

```java
@Test
void ensureOriginAirportCannotBeNull() {
    assertThrows(IllegalArgumentException.class, () ->
            new FlightRoute(new RouteName("TP123"), null,
                    AirportIATACode.valueOf("LIS"), IATACode.valueOf("TP")));
}
```

**Test:** `ensureDestinationAirportCannotBeNull`

```java
@Test
void ensureDestinationAirportCannotBeNull() {
    assertThrows(IllegalArgumentException.class, () ->
            new FlightRoute(new RouteName("TP123"),
                    AirportIATACode.valueOf("OPO"), null, IATACode.valueOf("TP")));
}
```

---

**AC073.2 — Origin and destination airports must be different**

**Test:** `ensureOriginAndDestinationCannotBeTheSame`

```java
@Test
void ensureOriginAndDestinationCannotBeTheSame() {
    assertThrows(IllegalArgumentException.class, () ->
            new FlightRoute(new RouteName("TP123"),
                    AirportIATACode.valueOf("OPO"),
                    AirportIATACode.valueOf("OPO"),
                    IATACode.valueOf("TP")));
}
```

---

**AC073.3 — Route name must follow the format `[A-Z]{2}[0-9]{1,4}`**

**Test:** `ensureValidRouteNameIsAccepted`

```java
@Test
void ensureValidRouteNameIsAccepted() {
    final RouteName name = new RouteName("TP123");
    assertEquals("TP123", name.toString());
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

---

**AC073.4 — Route name must be unique**

Route name uniqueness is enforced at the controller level via repository pre-check and at the database level via unique constraint. Validated through manual integration testing:

1. Register a route with name `TP123`.
2. Attempt to register a second route with the same name `TP123`.
3. Expected: the system rejects the operation with a uniqueness violation message.

---

**AC073.5 / AC073.6 — Authorization and company binding**

Authorization is enforced by the controller via EAPLI's authorization framework. The company is resolved automatically from the authenticated session. Validated through manual integration testing:

1. Login as an ATCC of company `TP`.
2. Navigate to **Flight Routes > Create Flight Route**.
3. Expected: the route is created and associated with company `TP` automatically.

---

**FlightRoute domain invariants**

**Test:** `ensureValidFlightRouteCanBeCreated`

```java
@Test
void ensureValidFlightRouteCanBeCreated() {
    final FlightRoute route = new FlightRoute(
            new RouteName("TP123"),
            AirportIATACode.valueOf("OPO"),
            AirportIATACode.valueOf("LIS"),
            IATACode.valueOf("TP"));
    assertEquals("TP123", route.identity().toString());
    assertEquals(FlightRouteStatus.ACTIVE, route.status());
}
```

**Test:** `ensureStatusStartsAsActive`

```java
@Test
void ensureStatusStartsAsActive() {
    final FlightRoute route = new FlightRoute(
            new RouteName("TP123"),
            AirportIATACode.valueOf("OPO"),
            AirportIATACode.valueOf("LIS"),
            IATACode.valueOf("TP"));
    assertEquals(FlightRouteStatus.ACTIVE, route.status());
}
```

**Test:** `ensureRouteNameCannotBeNull`

```java
@Test
void ensureRouteNameCannotBeNull() {
    assertThrows(IllegalArgumentException.class, () ->
            new FlightRoute(null,
                    AirportIATACode.valueOf("OPO"),
                    AirportIATACode.valueOf("LIS"),
                    IATACode.valueOf("TP")));
}
```

---

