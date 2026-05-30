# US073 — Principles and Patterns Applied

## 1. DDD — Domain Driven Design

### 1.1 Aggregate Root

`FlightRoute` is the aggregate root of the `FlightRoute` aggregate. All access to route data passes through it.

```java
public class FlightRoute implements AggregateRoot<RouteName> {}
```

---

### 1.2 Value Object


`RouteName` is a value object that encapsulates and validates the route name format `[A-Z]{2}[0-9]{1,4}`.

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

`RouteName` is used as `@EmbeddedId` — the natural business identity of `FlightRoute`. This is consistent with the rest of the domain (e.g. `AirportIATACode` in `Airport`, `RegistrationNumber` in `Aircraft`, `MecanographicNumber` in `User`).

```java
@EmbeddedId
@AttributeOverride(name = "name", column = @Column(name = "route_name", nullable = false))
private RouteName routeName;
```

---

### 1.4 Repository

`FlightRouteRepository` is a domain interface — the domain depends only on the interface, not on the implementation.

```java
public interface FlightRouteRepository extends DomainRepository<RouteName, FlightRoute> {
    boolean existsByName(RouteName routeName);
    Iterable<FlightRoute> findByCompany(IATACode companyIataCode);
}
```

Two implementations exist — `InMemoryFlightRouteRepository` (development/testing) and `JpaFlightRouteRepository` (production). The domain does not know which one is used.

---

### 1.5 Invariants

The `FlightRoute` constructor enforces all invariants:

```java
public FlightRoute(final RouteName routeName,
                   final AirportIATACode originAirport,
                   final AirportIATACode destinationAirport,
                   final IATACode companyIataCode) {
    if (routeName == null)
        throw new IllegalArgumentException("Route name cannot be null.");
    if (originAirport == null)
        throw new IllegalArgumentException("Origin airport cannot be null.");
    if (destinationAirport == null)
        throw new IllegalArgumentException("Destination airport cannot be null.");
    if (companyIataCode == null)
        throw new IllegalArgumentException("Company IATA code cannot be null.");
    if (originAirport.equals(destinationAirport))
        throw new IllegalArgumentException("Origin and destination airports must be different.");
    ...
    this.status = FlightRouteStatus.ACTIVE; // always starts ACTIVE
}
```

The `RouteName` constructor also enforces the format invariant `[A-Z]{2}[0-9]{1,4}`.

---

### 1.6 Low Coupling between Aggregates

`FlightRoute` references airports via `AirportIATACode` (a value object) — not via a `@ManyToOne Airport` reference:

```java
@Embedded
@AttributeOverride(name = "iataCode", column = @Column(name = "origin_airport_code"))
private AirportIATACode originAirport;

@Embedded
@AttributeOverride(name = "iataCode", column = @Column(name = "destination_airport_code"))
private AirportIATACode destinationAirport;
```

`FlightRoute` references the company via `IATACode` — not via a `@ManyToOne AirTransportCompany` reference:

```java
@Embedded
@AttributeOverride(name = "code", column = @Column(name = "company_iata_code"))
private IATACode companyIataCode;
```

If `Airport` or `AirTransportCompany` change internally, `FlightRoute` is not affected.

---

## 2. GRASP — General Responsibility Assignment Software Patterns

### 2.1 Information Expert

`RouteName` validates the route name format because it owns the `name` field — it is the information expert for route name validation:

```java
if (!name.matches("[A-Z]{2}[0-9]{1,4}"))
    throw new IllegalArgumentException("Invalid format: " + name);
```

`FlightRoute` validates the invariant "origin and destination must be different" because it owns both `originAirport` and `destinationAirport`:

```java
if (originAirport.equals(destinationAirport))
    throw new IllegalArgumentException("Origin and destination airports must be different.");
```

`FlightRoute` knows its own status via `isActive()` because it owns `status`:

```java
public boolean isActive() { return status == FlightRouteStatus.ACTIVE; }
```

---

### 2.2 Controller

`CreateFlightRouteController` is the application-layer controller. It:
- Verifies authorization
- Resolves the authenticated company from the session
- Validates route name uniqueness via the repository
- Verifies airports exist
- Creates the `FlightRoute` aggregate
- Persists via `FlightRouteRepository`

```java
@UseCaseController
public class CreateFlightRouteController {
    public FlightRoute createFlightRoute(final String routeNameStr,
                                         final String originCodeStr,
                                         final String destCodeStr) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ATCC);
        ...
    }
}
```

---

### 2.3 Creator

`CreateFlightRouteController` creates the `FlightRoute` because it has all the required data — `RouteName`, origin, destination and company identity:

```java
final FlightRoute route = new FlightRoute(
        routeName, originCode, destCode, company.identity());
```

---

### 2.4 Low Coupling

- `cascade = {}` not used (no cascade) — `FlightRoute` does not control the lifecycle of `Airport` or `AirTransportCompany`.
- References by identity (`AirportIATACode`, `IATACode`) instead of full object references.
- `FlightRouteRepository` is an interface — the domain does not depend on JPA or in-memory implementations.

---

### 2.5 High Cohesion

- `RouteName` — only responsible for route name format validation and storage.
- `FlightRoute` — only responsible for holding route data and enforcing route invariants.
- `CreateFlightRouteController` — only responsible for orchestrating the create route use case.
- `CreateFlightRouteUI` — only responsible for collecting user input and displaying results.
- `FlightRouteRepository` — only responsible for persisting and retrieving `FlightRoute` aggregates.

---

### 2.6 Protected Variations

`RouteName` encapsulates the format validation — if the format changes (e.g. 3 letters instead of 2), only `RouteName` changes. Controllers, UI and repositories are not affected:

```java
if (!name.matches("[A-Z]{2}[0-9]{1,4}"))
    throw new IllegalArgumentException(...);
```

`FlightRouteRepository` as an interface protects the domain from persistence changes — switching from in-memory to JPA (or any other database) requires no changes to the domain.

`FlightRouteStatus` enum protects against invalid status values — only `ACTIVE` and `INACTIVE` are possible.

---

## 3. SOLID

### 3.1 Single Responsibility Principle (SRP)

| Class | Single Responsibility |
|-------|----------------------|
| `RouteName` | Validate and store the route name format |
| `FlightRoute` | Hold route data and enforce route invariants |
| `FlightRouteStatus` | Represent the possible route statuses |
| `CreateFlightRouteController` | Orchestrate the create route use case |
| `CreateFlightRouteUI` | Collect route data from the user |
| `FlightRouteRepository` | Define the persistence contract for routes |
| `InMemoryFlightRouteRepository` | Persist routes in memory |
| `JpaFlightRouteRepository` | Persist routes in the database |

---

### 3.2 Interface Segregation Principle (ISP)

`FlightRouteRepository` only defines the methods needed for the `FlightRoute` aggregate — `existsByName()` and `findByCompany()` — in addition to the inherited `DomainRepository` methods. It does not include unrelated methods from other aggregates:

```java
public interface FlightRouteRepository extends DomainRepository<RouteName, FlightRoute> {
    boolean existsByName(RouteName routeName);
    Iterable<FlightRoute> findByCompany(IATACode companyIataCode);
}
```

---

### 3.3 Dependency Inversion Principle (DIP)

`CreateFlightRouteController` depends on the `FlightRouteRepository` **interface** — not on `InMemoryFlightRouteRepository` or `JpaFlightRouteRepository`:

```java
private final FlightRouteRepository flightRouteRepository =
        PersistenceContext.repositories().flightRoutes();
```

The `PersistenceContext` injects the correct implementation at runtime based on configuration.

---

## 4. GoF — Gang of Four Design Patterns

### 4.1 Facade

`CreateFlightRouteController` acts as a Facade for the create route use case — it hides the complexity of authorization checking, company resolution from session, repository lookups and domain object creation behind a single `createFlightRoute()` method:

```java
public FlightRoute createFlightRoute(final String routeNameStr,
                                     final String originCodeStr,
                                     final String destCodeStr) {
    // hides: auth check, session lookup, company resolution,
    //        uniqueness check, airport lookup, domain creation, persistence
}
```

---

### 4.2 Factory Method

`PersistenceContext.repositories().flightRoutes()` is a factory method — it returns the correct `FlightRouteRepository` implementation (in-memory or JPA) based on the application configuration, without the controller knowing which one:

```java
private final FlightRouteRepository flightRouteRepository =
        PersistenceContext.repositories().flightRoutes();
```

---

### 4.3 Template Method (via EAPLI)

`InMemoryFlightRouteRepository` and `JpaFlightRouteRepository` both extend base classes from the EAPLI framework (`InMemoryDomainRepository` and `JpaAutoTxRepository`) which provide the template for `save()`, `findAll()`, `ofIdentity()` etc. Each subclass only implements the specific query methods (`existsByName`, `findByCompany`):

```java
public class InMemoryFlightRouteRepository
        extends InMemoryDomainRepository<FlightRoute, RouteName>
        implements FlightRouteRepository {

    @Override
    public boolean existsByName(final RouteName routeName) {
        return matchOne(r -> r.routeName().equals(routeName)).isPresent();
    }
}
```

---

## 5. Summary Table

| Principle / Pattern | Category | Where in US073 |
|---------------------|----------|----------------|
| Aggregate Root | DDD | `FlightRoute implements AggregateRoot<RouteName>` |
| Value Object | DDD | `RouteName`, `AirportIATACode` (by identity reference) |
| Business Identity as VO | DDD | `RouteName` as `@EmbeddedId` |
| Repository | DDD | `FlightRouteRepository` interface |
| Invariants | DDD | `FlightRoute` and `RouteName` constructors |
| Low Coupling between Aggregates | DDD | References by `AirportIATACode` and `IATACode` |
| Information Expert | GRASP | `RouteName` validates format; `FlightRoute` validates invariants |
| Controller | GRASP | `CreateFlightRouteController` |
| Creator | GRASP | Controller creates `FlightRoute` with all required data |
| Low Coupling | GRASP | `cascade = {}`, identity references, repository interface |
| High Cohesion | GRASP | Each class has a single focused responsibility |
| Protected Variations | GRASP | `RouteName` encapsulates format; `FlightRouteRepository` as interface |
| SRP | SOLID | Each class has one reason to change |
| ISP | SOLID | `FlightRouteRepository` defines only route-specific methods |
| DIP | SOLID | Controller depends on `FlightRouteRepository` interface |
| Facade | GoF | `CreateFlightRouteController` hides use case complexity |
| Factory Method | GoF | `PersistenceContext.repositories().flightRoutes()` |
| Template Method | GoF | EAPLI base repositories provide the algorithm skeleton |
