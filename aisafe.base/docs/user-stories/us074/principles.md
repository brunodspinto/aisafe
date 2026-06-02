# US074 — Principles and Patterns Applied

## 1. DDD — Domain Driven Design

### 1.1 Aggregate Root

`FlightRoute` is the aggregate root of the `FlightRoute` aggregate. The `deactivate(date)` operation is invoked on it — all state changes to the route pass through the aggregate root.

```java
public class FlightRoute implements AggregateRoot<RouteName> {
    public void deactivate(final LocalDate date) { ... }
}
```

---

### 1.2 Value Object

`RouteName` is a value object that encapsulates and validates the route name format `[A-Z]{2}[0-9]{1,4}`. It is the business identity of `FlightRoute`.

```java
@Embeddable
public class RouteName implements ValueObject, Comparable<RouteName> {
    private String name;

    public RouteName(final String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Route name cannot be null or blank.");
        if (!name.matches("[A-Z]{2}[0-9]{1,4}"))
            throw new IllegalArgumentException(
                    "Route name must follow the format [A-Z]{2}[0-9]{1,4}: " + name);
        this.name = name;
    }
}
```

`AirportIATACode` is also a value object — referenced from the `Airport` aggregate — used to identify origin and destination airports without creating a direct object dependency between aggregates.

---

### 1.3 Business Identity as Value Object

`RouteName` is used as `@EmbeddedId` — the natural business identity of `FlightRoute`. This allows the repository to look up a route by name without loading all routes.

```java
@EmbeddedId
@AttributeOverride(name = "name", column = @Column(name = "route_name", nullable = false))
private RouteName routeName;
```

---

### 1.4 Repository

Two repository interfaces are used in this use case:

`FlightRouteRepository` — domain interface for persisting and querying routes:

```java
public interface FlightRouteRepository extends DomainRepository<RouteName, FlightRoute> {
    Iterable<FlightRoute> findActiveByCompany(IATACode companyIataCode);
}
```

`FlightRepository` — used for the cross-aggregate planned-flight check (AC074.3). It is a small,
focused interface (ISP) that exposes a single dedicated query method, so the controller never
loads all flight plans into memory:

```java
public interface FlightRepository {
    boolean hasFlightsAfter(FlightRoute route, LocalDate deactivationDate);
}
```

Both interfaces are owned by the domain. The controller depends only on these interfaces, not on any JPA or in-memory implementation. `FlightRepository` is backed by the `FlightPlan` aggregate (US080): a planned flight is a `FlightPlan` that references the route and has a `departureDateTime`.

---

### 1.5 Invariants

The `FlightRoute.deactivate()` method enforces the state-transition invariants for this use case:

```java
public void deactivate(final LocalDate date) {
    if (date == null)
        throw new IllegalArgumentException("Deactivation date cannot be null.");
    if (this.status == FlightRouteStatus.INACTIVE)
        throw new IllegalStateException("Route is already inactive.");
    this.activeUntil = date;
    this.status = FlightRouteStatus.INACTIVE;
}
```

The route always starts `ACTIVE` (enforced by the constructor — see US073). Deactivation is a one-way state transition: an already `INACTIVE` route cannot be deactivated again.

---

### 1.6 Low Coupling between Aggregates

`FlightRoute` does not hold a collection of its `Flight` objects. The cross-aggregate constraint (AC074.3 — no planned flights after the deactivation date) is enforced via a repository query in the controller, not inside the aggregate:

```java
// Controller — AC074.3 enforced here, not inside FlightRoute
if (flightRepository.hasFlightsAfter(route, deactivationDate))
    throw new IllegalArgumentException("Cannot deactivate: planned flights exist.");
```

This keeps `FlightRoute` within its own bounded context and avoids a direct dependency on the `Flight` aggregate.

`FlightRoute` references airports and company via value objects (`AirportIATACode`, `IATACode`) — not via full object references — so changes to `Airport` or `AirTransportCompany` do not affect `FlightRoute`.

---

## 2. GRASP — General Responsibility Assignment Software Patterns

### 2.1 Information Expert

`FlightRoute` enforces the state-transition invariants of `deactivate()` because it owns `status` and `activeUntil` — it is the information expert for its own state:

```java
public void deactivate(final LocalDate date) {
    if (this.status == FlightRouteStatus.INACTIVE)
        throw new IllegalStateException("Route is already inactive.");
    this.activeUntil = date;
    this.status = FlightRouteStatus.INACTIVE;
}
```

`FlightRoute` also knows whether it is active:

```java
public boolean isActive() { return status == FlightRouteStatus.ACTIVE; }
```

`RouteName` is the information expert for route name format validation — it owns the `name` field and enforces `[A-Z]{2}[0-9]{1,4}`.

The cross-aggregate constraint (AC074.3) cannot be enforced by `FlightRoute` because it does not hold its flights. The controller acts as the information expert for this orchestration step, using `FlightRepository`.

---

### 2.2 Controller

`DeactivateFlightRouteController` is the application-layer controller. It:
- Verifies authorization (ATCC role)
- Resolves the authenticated company from the session
- Lists active routes for the company
- Verifies that the selected route belongs to the company (AC074.4)
- Queries `FlightRepository` for planned-flight conflicts (AC074.3)
- Delegates the state transition to `FlightRoute.deactivate(date)`
- Persists via `FlightRouteRepository`

```java
@UseCaseController
public class DeactivateFlightRouteController {
    public FlightRoute deactivateFlightRoute(final RouteName routeName,
                                             final LocalDate deactivationDate) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ATCC);
        ...
    }
}
```

---

### 2.3 Low Coupling

- The cross-aggregate constraint (AC074.3) is enforced via `FlightRepository.hasFlightsAfter()` — a single count query — rather than loading all flights into memory or introducing a direct dependency from `FlightRoute` to `Flight`.
- References by identity (`AirportIATACode`, `IATACode`) avoid object-level coupling between `FlightRoute` and other aggregates.
- Both `FlightRouteRepository` and `FlightRepository` are interfaces — the controller does not depend on JPA or in-memory implementations.

---

### 2.4 High Cohesion

- `FlightRoute` — only responsible for holding route data and enforcing route state transitions.
- `DeactivateFlightRouteController` — only responsible for orchestrating the deactivate route use case.
- `DeactivateFlightRouteUI` — only responsible for collecting user input and displaying results.
- `FlightRouteRepository` — only responsible for persisting and retrieving `FlightRoute` aggregates.
- `FlightRepository` — only responsible for querying `Flight` aggregates on behalf of other use cases.

---

### 2.5 Protected Variations

`FlightRouteStatus` enum protects against invalid status values — only `ACTIVE` and `INACTIVE` are possible, enforced at the type level:

```java
Enum FlightRouteStatus { ACTIVE, INACTIVE }
```

`FlightRepository` as an interface protects the controller from changes to how planned-flight queries are implemented. The JPQL count query in `JpaFlightRepository` can be changed or optimized without touching the controller.

`RouteName` encapsulates the format validation — if the route name format changes, only `RouteName` changes.

---

## 3. SOLID

### 3.1 Single Responsibility Principle (SRP)

| Class | Single Responsibility |
|-------|----------------------|
| `RouteName` | Validate and store the route name format |
| `FlightRoute` | Hold route data and enforce route state transitions |
| `FlightRouteStatus` | Represent the possible route statuses |
| `DeactivateFlightRouteController` | Orchestrate the deactivate route use case |
| `DeactivateFlightRouteUI` | Collect user input and display results |
| `FlightRouteRepository` | Define the persistence contract for routes |
| `FlightRepository` | Define the query contract for flights |
| `JpaFlightRouteRepository` | Persist routes in the database |
| `JpaFlightRepository` | Implement `hasFlightsAfter()` via JPQL |

---

### 3.2 Interface Segregation Principle (ISP)

`FlightRepository` only exposes the method needed by this use case — `hasFlightsAfter()` — rather than a general-purpose flight query interface. It does not even extend `DomainRepository`, so the controller is offered exactly one method and nothing it does not use:

```java
public interface FlightRepository {
    boolean hasFlightsAfter(FlightRoute route, LocalDate deactivationDate);
}
```

---

### 3.3 Dependency Inversion Principle (DIP)

`DeactivateFlightRouteController` depends on the `FlightRouteRepository` and `FlightRepository` **interfaces** — not on any concrete implementation:

```java
private final FlightRouteRepository flightRouteRepository =
        PersistenceContext.repositories().flightRoutes();
private final FlightRepository flightRepository =
        PersistenceContext.repositories().flights();
```

The `PersistenceContext` injects the correct implementation at runtime based on configuration.

---

## 4. GoF — Gang of Four Design Patterns

### 4.1 Facade

`DeactivateFlightRouteController` acts as a Facade for the deactivate route use case — it hides the complexity of authorization checking, company resolution from session, ownership verification, planned-flight conflict check, domain state transition and persistence behind a single `deactivateFlightRoute()` method:

```java
public FlightRoute deactivateFlightRoute(final RouteName routeName,
                                         final LocalDate deactivationDate) {
    // hides: auth check, session lookup, company resolution, ownership check,
    //        planned-flight conflict check, domain deactivation, persistence
}
```

---

### 4.2 Factory Method

`PersistenceContext.repositories().flightRoutes()` and `PersistenceContext.repositories().flights()` are factory methods — they return the correct repository implementation (in-memory or JPA) based on the application configuration, without the controller knowing which one:

```java
private final FlightRouteRepository flightRouteRepository =
        PersistenceContext.repositories().flightRoutes();
private final FlightRepository flightRepository =
        PersistenceContext.repositories().flights();
```

---

### 4.3 Template Method (via EAPLI)

`JpaFlightRouteRepository` and `JpaFlightRepository` both extend the EAPLI base class `JpaAutoTxRepository`, which provides the template for `save()`, `findAll()`, `ofIdentity()`, `match()`, `matchOne()`, etc. Each subclass only supplies the specific query for this use case. `JpaFlightRepository` is parameterized over the `FlightPlan` aggregate, since a planned flight is a `FlightPlan` referencing the route:

```java
public class JpaFlightRepository
        extends JpaAutoTxRepository<FlightPlan, FlightPlanDesignator, FlightPlanDesignator>
        implements FlightRepository {

    @Override
    public boolean hasFlightsAfter(final FlightRoute route, final LocalDate deactivationDate) {
        final Map<String, Object> params = new HashMap<>();
        params.put("route", route.identity().name());
        params.put("date", deactivationDate.atStartOfDay());
        return matchOne(
                "e.routeName.name = :route AND e.departureDateTime >= :date", params).isPresent();
    }
}
```

The in-memory counterpart (`InMemoryFlightRepository`) implements the same contract by delegating to the `FlightPlanRepository`, so both back-ends share a single source of truth.

---

## 5. Summary Table

| Principle / Pattern             | Category  | Where in US074                                                                              |
|---------------------------------|-----------|---------------------------------------------------------------------------------------------|
| Aggregate Root                  | DDD       | `FlightRoute implements AggregateRoot<RouteName>`; exposes `deactivate(date)`               |
| Value Object                    | DDD       | `RouteName`, `AirportIATACode` (by identity reference)                                      |
| Business Identity as VO         | DDD       | `RouteName` as `@EmbeddedId`                                                                |
| Repository                      | DDD       | `FlightRouteRepository` (routes); `FlightRepository` (cross-aggregate query)                |
| Invariants                      | DDD       | `FlightRoute.deactivate()` enforces state-transition rules                                  |
| Low Coupling between Aggregates | DDD       | AC074.3 enforced via repository query, not inside `FlightRoute`                             |
| Information Expert              | GRASP     | `FlightRoute` owns its state; controller orchestrates cross-aggregate constraint            |
| Controller                      | GRASP     | `DeactivateFlightRouteController`                                                           |
| Low Coupling                    | GRASP     | Repository interfaces; identity references; count query instead of loading flights          |
| High Cohesion                   | GRASP     | Each class has a single focused responsibility                                              |
| Protected Variations            | GRASP     | `FlightRouteStatus` enum; `FlightRepository` as interface; `RouteName` encapsulates format  |
| SRP                             | SOLID     | Each class has one reason to change                                                         |
| ISP                             | SOLID     | `FlightRepository` defines only `hasFlightsAfter()`                                         |
| DIP                             | SOLID     | Controller depends on `FlightRouteRepository` and `FlightRepository` interfaces             |
| Facade                          | GoF       | `DeactivateFlightRouteController` hides use case complexity                                 |
| Factory Method                  | GoF       | `PersistenceContext.repositories().flightRoutes()` and `.flights()`                         |
| Template Method                 | GoF       | EAPLI base repositories provide the algorithm skeleton                                      |
