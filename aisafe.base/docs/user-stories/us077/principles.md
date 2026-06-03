# US077 — Principles and Patterns Applied

## 1. DDD — Domain Driven Design

### 1.1 Aggregate Root

`Pilot` is the aggregate root of the `Pilot` aggregate. All access to pilot state — including deactivation — passes through it.

```java
public class Pilot implements AggregateRoot<Long> {
    private boolean active;

    public void deactivate() {
        if (!active)
            throw new IllegalStateException("Pilot is already inactive.");
        active = false;
    }
}
```

---

### 1.2 Domain Behaviour on the Aggregate Root

`deactivate()` is a domain method on `Pilot` — not a setter. It encapsulates the state transition and enforces the invariant that an already-inactive pilot cannot be deactivated again. Placing this behaviour inside the aggregate (rather than in the controller or a service) respects the DDD principle that aggregates protect their own invariants.

---

### 1.3 Repository

`PilotRepository` and `FlightPlanRepository` are domain interfaces — the domain depends only on the contract, not on the implementation:

```java
public interface PilotRepository extends DomainRepository<Long, Pilot> {
    Iterable<Pilot> findByAirTransportCompany(AirTransportCompany company);
    Optional<Pilot> findBySystemUser(SystemUser systemUser);
}

public interface FlightPlanRepository extends DomainRepository<FlightPlanDesignator, FlightPlan> {
    boolean hasFlightPlanAssignedTo(Long pilotId);
}
```

Two implementations exist for each — `InMemory*` (development/testing) and `Jpa*` (production). The domain does not know which one is used.

---

### 1.4 Invariants

`Pilot.deactivate()` enforces the aggregate invariant "a pilot that is already inactive cannot be deactivated again":

```java
public void deactivate() {
    if (!active)
        throw new IllegalStateException("Pilot is already inactive.");
    active = false;
}
```

The flight-plan guard ("a pilot with flight plans assigned cannot be deactivated") is enforced at the application layer — not inside the domain method — because it requires querying the `FlightPlan` aggregate. Placing a cross-aggregate query inside `Pilot.deactivate()` would violate aggregate isolation.

---

### 1.5 Low Coupling between Aggregates

`Pilot` references its company via `IATACode` (a value object) — not via a `@ManyToOne AirTransportCompany` reference:

```java
@Embedded
@AttributeOverride(name = "code", column = @Column(name = "company_iata_code"))
private IATACode companyIataCode;
```

`FlightPlan` references its assigned pilot via `Long` identity — not via a `@ManyToOne Pilot` reference:

```java
@Column(name = "assigned_pilot_id")
private Long assignedPilotId;
```

If `AirTransportCompany` or `FlightPlan` change internally, `Pilot` is not affected.

---

## 2. GRASP — General Responsibility Assignment Software Patterns

### 2.1 Information Expert

`Pilot` owns the `active` field, so it is the Information Expert for deactivation behaviour — `deactivate()` belongs in `Pilot`:

```java
public void deactivate() {
    if (!active) throw new IllegalStateException("Pilot is already inactive.");
    active = false;
}
```

Conversely, `Pilot` does **not** own `FlightPlan` data, so the flight-plan guard is **not** placed inside `Pilot`. The `FlightPlanRepository` is the expert for querying flight plan data, and that check is delegated to the application layer.

---

### 2.2 Controller

`RemovePilotController` is the application-layer controller for this use case. It:
- Verifies authorization (`ATCC` role)
- Resolves the authenticated company from the session
- Lists only active pilots of that company
- Verifies the selected pilot belongs to the company
- Checks for assigned flight plans
- Delegates deactivation to the domain
- Persists via `PilotRepository`

```java
@UseCaseController
public class RemovePilotController {
    public Pilot deactivatePilot(final Long pilotId) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ATCC);
        ...
    }
}
```

---

### 2.3 Low Coupling

- `Pilot` references company by `IATACode` (value object), not `@ManyToOne AirTransportCompany`.
- `FlightPlan` references pilot by `Long` identity, not `@ManyToOne Pilot`.
- `PilotRepository` and `FlightPlanRepository` are interfaces — the controller depends on the contract, not the implementation.
- No cascade operations — `Pilot` does not control the lifecycle of `FlightPlan`.

---

### 2.4 High Cohesion

| Class | Single Focused Responsibility |
|-------|------------------------------|
| `Pilot` | Hold pilot state and enforce deactivation invariant |
| `RemovePilotController` | Orchestrate the remove pilot use case |
| `RemovePilotUI` | Collect pilot selection and confirmation from the user |
| `PilotRepository` | Persist and retrieve `Pilot` aggregates |
| `FlightPlanRepository` | Persist and retrieve `FlightPlan` aggregates; guard AC077.3 |

---

### 2.5 Protected Variations

`PilotRepository` and `FlightPlanRepository` as interfaces protect the application layer from persistence changes — switching from in-memory to JPA requires no changes to the controller or domain:

```java
private final PilotRepository pilotRepo =
        PersistenceContext.repositories().pilots();
private final FlightPlanRepository flightPlanRepo =
        PersistenceContext.repositories().flightPlans();
```

---

## 3. SOLID

### 3.1 Single Responsibility Principle (SRP)

| Class | Single Responsibility |
|-------|----------------------|
| `Pilot` | Hold pilot state and enforce deactivation invariant |
| `RemovePilotController` | Orchestrate the remove pilot use case |
| `RemovePilotUI` | Collect input from the user |
| `PilotRepository` | Define the persistence contract for pilots |
| `FlightPlanRepository` | Define the persistence contract for flight plans |
| `InMemoryPilotRepository` | Persist pilots in memory |
| `JpaPilotRepository` | Persist pilots in the database |
| `InMemoryFlightPlanRepository` | Persist flight plans in memory |
| `JpaFlightPlanRepository` | Persist flight plans in the database |

---

### 3.2 Interface Segregation Principle (ISP)

`FlightPlanRepository` defines only the method added for this use case — `hasFlightPlanAssignedTo(Long pilotId)` — in addition to the inherited `DomainRepository` methods. It does not include unrelated methods:

```java
public interface FlightPlanRepository extends DomainRepository<FlightPlanDesignator, FlightPlan> {
    boolean hasFlightPlanAssignedTo(Long pilotId);
}
```

`PilotRepository` similarly only defines methods relevant to the `Pilot` aggregate (`findByAirTransportCompany`, `findBySystemUser`).

---

### 3.3 Dependency Inversion Principle (DIP)

`RemovePilotController` depends on `PilotRepository` and `FlightPlanRepository` **interfaces** — not on their concrete implementations:

```java
private final PilotRepository pilotRepo =
        PersistenceContext.repositories().pilots();
private final FlightPlanRepository flightPlanRepo =
        PersistenceContext.repositories().flightPlans();
```

The `PersistenceContext` injects the correct implementation at runtime based on configuration.

---

## 4. GoF — Gang of Four Design Patterns

### 4.1 Facade

`RemovePilotController` acts as a Facade for the remove pilot use case — it hides the complexity of authorization checking, session resolution, company scoping, flight-plan guard and domain deactivation behind a single `deactivatePilot()` method:

```java
public Pilot deactivatePilot(final Long pilotId) {
    // hides: auth check, session lookup, company resolution,
    //        pilot lookup, company ownership check,
    //        flight-plan guard, domain deactivate(), persistence
}
```

---

### 4.2 Factory Method

`PersistenceContext.repositories().pilots()` and `PersistenceContext.repositories().flightPlans()` are factory methods — they return the correct repository implementation (in-memory or JPA) based on application configuration, without the controller knowing which one:

```java
private final PilotRepository pilotRepo =
        PersistenceContext.repositories().pilots();
```

---

### 4.3 Template Method (via EAPLI)

`InMemoryPilotRepository` and `JpaPilotRepository` extend EAPLI base classes (`InMemoryDomainRepository` and `JpaAutoTxRepository`) which provide the algorithm skeleton for `save()`, `findAll()`, `ofIdentity()` etc. Each subclass only implements the specific query methods:

```java
public class InMemoryPilotRepository
        extends InMemoryDomainRepository<Pilot, Long>
        implements PilotRepository {

    @Override
    public Iterable<Pilot> findByAirTransportCompany(final AirTransportCompany company) { ... }
}
```

The same pattern applies to `InMemoryFlightPlanRepository` / `JpaFlightPlanRepository`.

---

## 5. Summary Table

| Principle / Pattern | Category | Where in US077 |
|---------------------|----------|----------------|
| Aggregate Root | DDD | `Pilot implements AggregateRoot<Long>` |
| Domain Behaviour on AR | DDD | `Pilot.deactivate()` enforces state transition and invariant |
| Repository | DDD | `PilotRepository`, `FlightPlanRepository` interfaces |
| Invariants | DDD | `Pilot.deactivate()` — guards already-inactive state |
| Low Coupling between Aggregates | DDD | `IATACode` (company ref), `Long` (pilot ref from FlightPlan) |
| Information Expert | GRASP | `Pilot` owns `active` → owns `deactivate()`; flight-plan guard delegated to repo |
| Controller | GRASP | `RemovePilotController` |
| Low Coupling | GRASP | Identity references, repository interfaces, no cascade |
| High Cohesion | GRASP | Each class has a single focused responsibility |
| Protected Variations | GRASP | Repository interfaces shield controller from persistence changes |
| SRP | SOLID | Each class has one reason to change |
| ISP | SOLID | Repositories define only methods for their aggregate |
| DIP | SOLID | Controller depends on repository interfaces |
| Facade | GoF | `RemovePilotController` hides use case complexity |
| Factory Method | GoF | `PersistenceContext.repositories().pilots()` / `.flightPlans()` |
| Template Method | GoF | EAPLI base repositories provide the algorithm skeleton |
