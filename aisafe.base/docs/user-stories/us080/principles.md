# US080 — Principles and Patterns Applied

## 1. DDD — Domain Driven Design

### 1.1 Aggregate Root

`FlightPlan` is the aggregate root of the `FlightPlan` aggregate. All access to flight plan data passes through it. The same aggregate supports two creation paths — form-based (US080) and DSL-based (US081) — keeping the domain concept unified.

```java
public class FlightPlan implements AggregateRoot<FlightPlanDesignator> {}
```

---

### 1.2 Value Object

`FuelQuantity` is a value object that encapsulates and validates the fuel amount (strictly positive).

```java
@Embeddable
public class FuelQuantity implements ValueObject, Comparable<FuelQuantity> {
    private double amount;

    public FuelQuantity(final double amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("Fuel quantity must be strictly positive.");
        this.amount = amount;
    }
}
```

`RouteName` and `RegistrationNumber` are also value objects — referenced from the `FlightRoute` and `Aircraft` aggregates — used to identify the route and the aircraft without creating a direct object dependency between aggregates.

---

### 1.3 Business Identity as Value Object

`FlightPlanDesignator` is used as `@EmbeddedId` — the natural business identity of `FlightPlan` (the flight designator, e.g. `TP1234`). This is consistent with the rest of the domain (e.g. `RouteName` in `FlightRoute`, `RegistrationNumber` in `Aircraft`, `MecanographicNumber` in `User`).

```java
@EmbeddedId
private FlightPlanDesignator designator;
```

The `FlightPlanDesignator` constructor enforces the format invariant `xxN(N)(N)(N)(a)` (section 3.2):

```java
if (!normalized.matches("[A-Z]{2}[0-9]{1,4}[A-Z]?"))
    throw new IllegalArgumentException(
            "Flight plan designator must follow the format xxN(N)(N)(N)(a): " + value);
```

---

### 1.4 Repository

`FlightPlanRepository` is a domain interface — the domain depends only on the interface, not on the implementation.

```java
public interface FlightPlanRepository extends DomainRepository<FlightPlanDesignator, FlightPlan> {
}
```

Two implementations exist — `InMemoryFlightPlanRepository` (development/testing) and `JpaFlightPlanRepository` (production). The domain does not know which one is used.

---

### 1.5 Invariants

The `FlightPlan` form-based constructor enforces all aggregate-owned invariants:

```java
public FlightPlan(final FlightPlanDesignator designator,
                  final FlightType flightType,
                  final RouteName routeName,
                  final RegistrationNumber aircraftRegistration,
                  final Long assignedPilotId,
                  final LocalDateTime departureDateTime,
                  final FuelQuantity fuelQuantity) {
    if (designator == null) throw new IllegalArgumentException("Flight plan designator cannot be null or empty.");
    if (flightType == null) throw new IllegalArgumentException("Flight type cannot be null.");
    if (routeName == null) throw new IllegalArgumentException("Flight route cannot be null.");
    if (aircraftRegistration == null) throw new IllegalArgumentException("Aircraft cannot be null.");
    if (assignedPilotId == null) throw new IllegalArgumentException("Assigned pilot cannot be null.");
    if (departureDateTime == null) throw new IllegalArgumentException("Departure date/time cannot be null.");
    if (departureDateTime.isBefore(LocalDateTime.now()))
        throw new IllegalArgumentException("Departure date/time must be in the future.");
    if (fuelQuantity == null) throw new IllegalArgumentException("Fuel quantity cannot be null.");
    ...
    this.status = FlightPlanStatus.DRAFT; // always starts DRAFT
}
```

The `FuelQuantity` constructor enforces the positivity invariant, and the `FlightPlanDesignator` constructor enforces the format invariant.

---

### 1.6 Low Coupling between Aggregates

`FlightPlan` references the route via `RouteName`, the aircraft via `RegistrationNumber`, and the pilot via its `Long` identity — never via `@ManyToOne FlightRoute / Aircraft / Pilot` object references:

```java
@Embedded
@AttributeOverride(name = "name", column = @Column(name = "route_name"))
private RouteName routeName;

@Embedded
@AttributeOverride(name = "value", column = @Column(name = "aircraft_registration"))
private RegistrationNumber aircraftRegistration;

@Column(name = "assigned_pilot_id")
private Long assignedPilotId;
```

If `FlightRoute`, `Aircraft` or `Pilot` change internally, `FlightPlan` is not affected. This is consistent with the `Pilot` aggregate (US075), which references `AircraftModel` by id only.

---

## 2. GRASP — General Responsibility Assignment Software Patterns

### 2.1 Information Expert

`FuelQuantity` validates the fuel amount because it owns the `amount` field — it is the information expert for fuel validation:

```java
if (amount <= 0)
    throw new IllegalArgumentException("Fuel quantity must be strictly positive.");
```

`FlightPlanDesignator` validates the designator format because it owns the `value` field.

`FlightPlan` validates the "departure must be in the future" invariant because it owns `departureDateTime`:

```java
if (departureDateTime.isBefore(LocalDateTime.now()))
    throw new IllegalArgumentException("Departure date/time must be in the future.");
```

Cross-aggregate rules (assigned pilot of the route's company, aircraft of the route's company, aircraft active) are **not** owned by a single aggregate — they span `FlightRoute`, `Pilot`, `Aircraft` and `AirTransportCompany`. They are therefore coordinated by the controller, which is the expert with access to all the involved aggregates.

---

### 2.2 Controller

`CreateFlightPlanController` is the application-layer controller. It:
- Verifies authorization (`PILOT` role)
- Resolves the authenticated pilot and their company from the session
- Offers the active routes, aircraft and pilots of that company
- Verifies the route, aircraft and pilot exist
- Enforces the cross-aggregate rules (AC080.3, AC080.9, AC080.10) and designator uniqueness (AC080.8)
- Creates the `FlightPlan` aggregate
- Persists via `FlightPlanRepository`

```java
@UseCaseController
public class CreateFlightPlanController {
    public FlightPlan createFlightPlan(final String routeNameStr,
                                       final String aircraftRegistrationStr,
                                       final Long assignedPilotId,
                                       final FlightType flightType,
                                       final String designatorStr,
                                       final LocalDateTime departureDateTime,
                                       final double fuelAmount) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.PILOT);
        ...
    }
}
```

---

### 2.3 Creator

`CreateFlightPlanController` creates the `FlightPlan` because it has aggregated all the required data — designator, flight type, route name, aircraft registration, pilot identity, departure date/time and fuel quantity:

```java
final FlightPlan plan = new FlightPlan(
        designator, flightType, route.routeName(), registration,
        assignedPilot.identity(), departureDateTime, FuelQuantity.valueOf(fuelAmount));
```

---

### 2.4 Low Coupling

- No cascade between aggregates — `FlightPlan` does not control the lifecycle of `FlightRoute`, `Aircraft` or `Pilot`.
- References by identity (`RouteName`, `RegistrationNumber`, `Long`) instead of full object references.
- `FlightPlanRepository` is an interface — the domain does not depend on JPA or in-memory implementations.
- A single aggregate is written per use case, so no transactional context coordinating multiple aggregates is needed.

---

### 2.5 High Cohesion

- `FuelQuantity` — only responsible for fuel amount validation and storage.
- `FlightPlanDesignator` — only responsible for designator format validation and storage.
- `FlightPlan` — only responsible for holding flight plan data and enforcing its own invariants and status lifecycle.
- `CreateFlightPlanController` — only responsible for orchestrating the create flight plan use case.
- `CreateFlightPlanUI` — only responsible for collecting user input and displaying results.
- `FlightPlanRepository` — only responsible for persisting and retrieving `FlightPlan` aggregates.

---

### 2.6 Protected Variations

`FuelQuantity` and `FlightPlanDesignator` encapsulate their validation rules — if the fuel rule or the designator format changes, only those value objects change. Controllers, UI and repositories are not affected:

```java
if (!normalized.matches("[A-Z]{2}[0-9]{1,4}[A-Z]?"))
    throw new IllegalArgumentException(...);
```

`FlightPlanRepository` as an interface protects the domain from persistence changes — switching from in-memory to JPA (or any other database) requires no changes to the domain.

`FlightPlanStatus` enum protects against invalid status values — only `DRAFT`, `VALIDATED` and `TESTED` are possible.

---

## 3. SOLID

### 3.1 Single Responsibility Principle (SRP)

| Class | Single Responsibility |
|-------|----------------------|
| `FuelQuantity` | Validate and store the fuel amount |
| `FlightPlanDesignator` | Validate and store the designator format |
| `FlightPlan` | Hold flight plan data and enforce its invariants and status lifecycle |
| `FlightPlanStatus` | Represent the possible flight plan statuses |
| `CreateFlightPlanController` | Orchestrate the create flight plan use case |
| `CreateFlightPlanUI` | Collect flight plan data from the user |
| `FlightPlanRepository` | Define the persistence contract for flight plans |
| `InMemoryFlightPlanRepository` | Persist flight plans in memory |
| `JpaFlightPlanRepository` | Persist flight plans in the database |

---

### 3.2 Interface Segregation Principle (ISP)

`FlightPlanRepository` exposes only the generic aggregate contract inherited from `DomainRepository` (`save`, `ofIdentity`, `findAll`, …) that this use case needs (`ofIdentity` for the uniqueness check, `save` for persistence). It does not include unrelated methods from other aggregates.

```java
public interface FlightPlanRepository extends DomainRepository<FlightPlanDesignator, FlightPlan> {
}
```

---

### 3.3 Dependency Inversion Principle (DIP)

`CreateFlightPlanController` depends on repository **interfaces** — not on `InMemory*` or `Jpa*` implementations:

```java
private final FlightPlanRepository flightPlanRepo = PersistenceContext.repositories().flightPlans();
private final FlightRouteRepository flightRouteRepo = PersistenceContext.repositories().flightRoutes();
private final AircraftRepository aircraftRepo = PersistenceContext.repositories().aircraft();
private final PilotRepository pilotRepo = PersistenceContext.repositories().pilots();
```

The `PersistenceContext` injects the correct implementations at runtime based on configuration.

---

## 4. GoF — Gang of Four Design Patterns

### 4.1 Facade

`CreateFlightPlanController` acts as a Facade for the create flight plan use case — it hides the complexity of authorization, session/company resolution, multiple repository lookups, cross-aggregate validation and domain object creation behind a single `createFlightPlan()` method.

```java
public FlightPlan createFlightPlan(...) {
    // hides: auth check, pilot/company resolution, route/aircraft/pilot lookup,
    //        company-matching + active checks, uniqueness check, domain creation, persistence
}
```

---

### 4.2 Factory Method

`PersistenceContext.repositories().flightPlans()` (and `.flightRoutes()`, `.aircraft()`, `.pilots()`) are factory methods — they return the correct repository implementation (in-memory or JPA) based on the application configuration, without the controller knowing which one:

```java
private final FlightPlanRepository flightPlanRepo = PersistenceContext.repositories().flightPlans();
```

The static (overloaded) factory methods `FuelQuantity.valueOf(double)` and `FlightPlanDesignator.valueOf(String)` are also examples of the (simple/static) factory idiom for value object creation.

---

### 4.3 Template Method (via EAPLI)

`InMemoryFlightPlanRepository` and `JpaFlightPlanRepository` both extend base classes from the EAPLI framework (`InMemoryDomainRepository` and `JpaAutoTxRepository`) which provide the template for `save()`, `findAll()`, `ofIdentity()` etc. The flight plan repositories reuse the inherited algorithm skeleton without overriding it.

```java
public class InMemoryFlightPlanRepository
        extends InMemoryDomainRepository<FlightPlan, FlightPlanDesignator>
        implements FlightPlanRepository {
}
```

---

## 5. Summary Table

| Principle / Pattern | Category | Where in US080 |
|---------------------|----------|----------------|
| Aggregate Root | DDD | `FlightPlan implements AggregateRoot<FlightPlanDesignator>` |
| Value Object | DDD | `FuelQuantity`, `RouteName` / `RegistrationNumber` (by identity reference) |
| Business Identity as VO | DDD | `FlightPlanDesignator` as `@EmbeddedId` (format `xxN(N)(N)(N)(a)`) |
| Repository | DDD | `FlightPlanRepository` interface |
| Invariants | DDD | `FlightPlan`, `FuelQuantity` and `FlightPlanDesignator` constructors |
| Low Coupling between Aggregates | DDD | References by `RouteName`, `RegistrationNumber` and pilot `Long` id |
| Information Expert | GRASP | VOs validate their own data; `FlightPlan` validates its invariants; controller coordinates cross-aggregate rules |
| Controller | GRASP | `CreateFlightPlanController` |
| Creator | GRASP | Controller creates `FlightPlan` with all required data |
| Low Coupling | GRASP | No cascade, identity references, repository interfaces, single-aggregate write |
| High Cohesion | GRASP | Each class has a single focused responsibility |
| Protected Variations | GRASP | `FuelQuantity` / `FlightPlanDesignator` encapsulate rules; `FlightPlanRepository` as interface |
| SRP | SOLID | Each class has one reason to change |
| ISP | SOLID | `FlightPlanRepository` exposes only the needed aggregate contract |
| DIP | SOLID | Controller depends on repository interfaces |
| Facade | GoF | `CreateFlightPlanController` hides use case complexity |
| Factory Method | GoF | `PersistenceContext.repositories().flightPlans()`; VO `valueOf(...)` |
| Template Method | GoF | EAPLI base repositories provide the algorithm skeleton |
