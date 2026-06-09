# US075 — Principles and Patterns Applied

## 1. DDD — Domain Driven Design

### 1.1 Aggregate Root

`Pilot` is the aggregate root of the `Pilot` aggregate. All access to pilot data passes through it.

```java
public class Pilot implements AggregateRoot<Long> {}
```

It was created as a **separate aggregate** from `Collaborator` because a pilot has distinct domain behaviour — certifications for aircraft models and an active/inactive status — keeping each aggregate focused (SRP).

---

### 1.2 Value Object / Identity references

The company and the certified aircraft models are referenced by identity, not by object: the company via the `IATACode` value object, and the certifications as a `Set<Long>` of aircraft model ids.

```java
@Embedded
@AttributeOverride(name = "code", column = @Column(name = "company_iata_code"))
private IATACode companyIataCode;

@ElementCollection(fetch = FetchType.LAZY)
@CollectionTable(name = "T_PILOT_CERTIFICATIONS",
        joinColumns = @JoinColumn(name = "pilot_id"))
@Column(name = "aircraft_model_id")
private Set<Long> certifiedAircraftModelIds = new HashSet<>();
```

---

### 1.3 Business Identity

`Pilot` uses an auto-generated surrogate `Long` identity (`@Id @GeneratedValue`), like `AircraftModel`, `EngineModel` and `Collaborator`. The backing system user (`User`, with the `PILOT` role) is referenced via `@OneToOne`.

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

@OneToOne(fetch = FetchType.LAZY, cascade = {})
@JoinColumn(name = "user_mecanographic_number")
private User user;
```

---

### 1.4 Repository

`PilotRepository` is a domain interface — the domain depends only on the interface, not on the implementation.

```java
public interface PilotRepository extends DomainRepository<Long, Pilot> {
    Iterable<Pilot> findByAirTransportCompany(AirTransportCompany company);
    Optional<Pilot> findBySystemUser(SystemUser systemUser);
}
```

Two implementations exist — `InMemoryPilotRepository` (development/testing) and `JpaPilotRepository` (production). The domain does not know which one is used.

---

### 1.5 Invariants

The `Pilot` constructor enforces all invariants — non-null user and company, and **at least one certification** — and exposes the certifications as unmodifiable.

```java
public Pilot(final User user, final IATACode companyIataCode,
             final Set<Long> certifiedAircraftModelIds) {
    if (user == null)
        throw new IllegalArgumentException("User cannot be null.");
    if (companyIataCode == null)
        throw new IllegalArgumentException("Air Transport Company cannot be null.");
    if (certifiedAircraftModelIds == null || certifiedAircraftModelIds.isEmpty())
        throw new IllegalArgumentException(
                "A pilot must be certified for at least one aircraft model.");
    this.active = true; // a newly added pilot is active
}
```

---

### 1.6 Low Coupling between Aggregates

`Pilot` references `AirTransportCompany` via `IATACode` and `AircraftModel` via their ids — never via `@ManyToOne` object references. If those aggregates change internally, `Pilot` is not affected.

---

## 2. GRASP — General Responsibility Assignment Software Patterns

### 2.1 Information Expert

`Pilot` validates the "at least one certification" rule and answers `isCertifiedFor(...)` because it owns the certification set:

```java
public boolean isCertifiedFor(final Long aircraftModelId) {
    return certifiedAircraftModelIds.contains(aircraftModelId);
}
```

The existence of each aircraft model (a cross-aggregate concern) is checked by the controller, which has access to the `AircraftModelRepository`.

---

### 2.2 Controller

`AddPilotController` is the application-layer controller. It verifies the `ATCC` role, resolves the authenticated collaborator's company from the session, validates that the selected aircraft models exist, creates the backing system user (PILOT role) and the `Pilot` aggregate, and persists — all within a transaction.

```java
@UseCaseController
public class AddPilotController {
    public Pilot addPilot() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ATCC);
    }
}
```

---

### 2.3 Creator

`AddPilotController` creates the `Pilot` because it aggregates all the required data — the user, the resolved company identity and the certified model ids:

```java
final Pilot pilot = pilotRepo.save(
        new Pilot(user, company.identity(), certifiedAircraftModelIds));
```

---

### 2.4 Low Coupling

- References by identity (`IATACode`, `Long`) instead of full object references.
- `cascade = {}` on the `User` association — the pilot does not own the user's lifecycle in cascade terms.
- `PilotRepository` is an interface — the domain does not depend on JPA or in-memory implementations.

---

### 2.5 High Cohesion

- `Pilot` — holds pilot data and enforces pilot invariants.
- `AddPilotController` — orchestrates the add-pilot use case.
- `AddPilotUI` — collects input and displays the result.
- `PilotRepository` — persists and retrieves pilots.

---

### 2.6 Protected Variations

The authorization check and the company resolution are encapsulated in the controller; the persistence behind the `PilotRepository` interface. Switching the database (in-memory ↔ JPA) requires no change to the domain or the controller logic.

---

## 3. SOLID

### 3.1 Single Responsibility Principle (SRP)

| Class | Single Responsibility |
|-------|----------------------|
| `Pilot` | Hold pilot data and enforce pilot invariants |
| `AddPilotController` | Orchestrate the add-pilot use case |
| `AddPilotUI` | Collect pilot data from the user |
| `PilotRepository` | Define the persistence contract for pilots |
| `InMemoryPilotRepository` / `JpaPilotRepository` | Persist pilots (in memory / in the database) |

---

### 3.2 Interface Segregation Principle (ISP)

`PilotRepository` defines only the methods needed for the `Pilot` aggregate (`findByAirTransportCompany`, `findBySystemUser`) plus the inherited `DomainRepository` methods — no unrelated methods.

---

### 3.3 Dependency Inversion Principle (DIP)

`AddPilotController` depends on repository **interfaces**, resolved at runtime by `PersistenceContext`:

```java
private final PilotRepository pilotRepo = PersistenceContext.repositories().pilots(tx);
private final AircraftModelRepository modelRepo = PersistenceContext.repositories().aircraftModels();
```

---

## 4. GoF — Gang of Four Design Patterns

### 4.1 Facade

`AddPilotController` is a Facade for the use case — it hides authorization, session/company resolution, model-existence validation, system-user creation and persistence behind a single `addPilot(...)` method.

---

### 4.2 Factory Method

`PersistenceContext.repositories().pilots()` / `.aircraftModels()` / `.collaborators()` are factory methods returning the configured repository implementation (in-memory or JPA) without the controller knowing which one. EAPLI's `UserManagementService.registerNewUser(...)` acts as a factory for the backing `SystemUser`.

---

### 4.3 Template Method (via EAPLI)

`InMemoryPilotRepository` and `JpaPilotRepository` extend EAPLI base classes (`InMemoryDomainRepository`, `JpaAutoTxRepository`) which provide the `save()`, `findAll()`, `ofIdentity()` algorithm skeleton; each subclass only implements the specific query methods (`findByAirTransportCompany`, `findBySystemUser`).

---

## 5. Summary Table

| Principle / Pattern | Category | Where in US075 |
|---------------------|----------|----------------|
| Aggregate Root | DDD | `Pilot implements AggregateRoot<Long>` |
| Value Object / identity references | DDD | `IATACode`, certifications as `Set<Long>` |
| Business Identity | DDD | surrogate `Long` id; `User` via `@OneToOne` |
| Repository | DDD | `PilotRepository` interface |
| Invariants | DDD | `Pilot` constructor (≥1 certification, non-null user/company) |
| Low Coupling between Aggregates | DDD | references by `IATACode` and `Long` |
| Information Expert | GRASP | `Pilot.isCertifiedFor`; controller checks model existence |
| Controller | GRASP | `AddPilotController` |
| Creator | GRASP | controller creates `Pilot` with all required data |
| Low Coupling | GRASP | identity references, `cascade = {}`, repository interface |
| High Cohesion | GRASP | each class single focused responsibility |
| Protected Variations | GRASP | repository interface; controller encapsulates auth/company resolution |
| SRP | SOLID | each class one reason to change |
| ISP | SOLID | `PilotRepository` only pilot-specific methods |
| DIP | SOLID | controller depends on repository interfaces |
| Facade | GoF | `AddPilotController` hides use case complexity |
| Factory Method | GoF | `PersistenceContext.repositories().pilots()`; `registerNewUser` |
| Template Method | GoF | EAPLI base repositories provide the algorithm skeleton |
