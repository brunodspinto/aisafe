# US082 — Principles and Patterns Applied

## 1. DDD — Domain Driven Design

### 1.1 Aggregate Root

`FlightPlan` is the aggregate root. The weather-data association and the test-voiding rule are applied through it — no other object reaches into the plan's status or its weather set.

```java
public class FlightPlan implements AggregateRoot<FlightPlanDesignator> {}
```

---

### 1.2 Value Object / Identity references

The weather records and the assigned pilot are referenced by identity, not by object — the plan holds a `Set<Long>` of `WeatherData` ids and a `Long` assigned pilot id.

```java
@ElementCollection(fetch = FetchType.LAZY)
@CollectionTable(name = "T_FLIGHT_PLAN_WEATHER_DATA",
        joinColumns = @JoinColumn(name = "flight_plan_designator"))
@Column(name = "weather_data_id")
private Set<Long> weatherDataIds = new HashSet<>();
```

`FlightPlanDesignator` (the aggregate identity) remains a value object used as `@EmbeddedId`.

---

### 1.3 Repository

`FlightPlanRepository`, `WeatherDataRepository` and `PilotRepository` are domain interfaces — the use case depends only on the interfaces, not on the JPA or in-memory implementations.

```java
public interface WeatherDataRepository extends DomainRepository<Long, WeatherData> {
    Iterable<WeatherData> findByDateAndAirControlArea(LocalDate date, AirControlAreaCode areaCode);
}
```

---

### 1.4 Invariants

The test-voiding invariant is enforced inside the aggregate: a previously `TESTED` plan that receives genuinely new weather data is reverted to `VALIDATED`; re-adding an already-attached record is a no-op.

```java
public void addWeatherData(final Long weatherDataId) {
    if (weatherDataId == null)
        throw new IllegalArgumentException("Weather data id cannot be null.");
    final boolean added = this.weatherDataIds.add(weatherDataId);
    if (added && this.status == FlightPlanStatus.TESTED) {
        this.status = FlightPlanStatus.VALIDATED;
    }
}
```

The weather set is exposed as unmodifiable, so the invariant cannot be bypassed from outside the aggregate:

```java
public Set<Long> weatherDataIds() {
    return Collections.unmodifiableSet(weatherDataIds);
}
```

---

### 1.5 Low Coupling between Aggregates

`FlightPlan` references `WeatherData` and `Pilot` only by identity (`Long`), never by a `@ManyToOne` object reference. If `WeatherData` or `Pilot` change internally, `FlightPlan` is not affected. This is consistent with the rest of the domain (US075 pilot certifications, US080 form references).

---

## 2. GRASP — General Responsibility Assignment Software Patterns

### 2.1 Information Expert

`FlightPlan` owns both its weather set and its `status`, so it is the expert that decides whether adding weather voids the test:

```java
final boolean added = this.weatherDataIds.add(weatherDataId);
if (added && this.status == FlightPlanStatus.TESTED) {
    this.status = FlightPlanStatus.VALIDATED;
}
```

The cross-aggregate rules (ownership, existence of the plan and the weather data) are **not** owned by a single aggregate — they are coordinated by the controller, which has access to all the involved aggregates and the session.

---

### 2.2 Controller

`InsertWeatherDataController` is the application-layer controller. It verifies the `PILOT` role, resolves the authenticated pilot, loads and validates ownership of the flight plan, confirms the weather data exists, invokes the domain operation, and persists.

```java
@UseCaseController
public class InsertWeatherDataController {
    public FlightPlan insertWeatherData(final String designatorStr, final Long weatherDataId) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.PILOT);
        ...
    }
}
```

---

### 2.3 Low Coupling

- References by identity (`Long`) between `FlightPlan`, `WeatherData` and `Pilot`.
- No cascade — `FlightPlan` does not control the lifecycle of `WeatherData` or `Pilot`.
- A single aggregate is written per use case, so no transactional context coordinating multiple aggregates is needed.

---

### 2.4 High Cohesion

- `FlightPlan.addWeatherData` — one cohesive operation: attach weather and, as a consequence, void a stale test.
- `InsertWeatherDataController` — only orchestrates the insert-weather use case.
- `InsertWeatherDataUI` — only collects the selection and shows the result.

---

### 2.5 Protected Variations

The status lifecycle is encapsulated in the `FlightPlan` aggregate and the `FlightPlanStatus` enum — if the voiding policy changes (e.g. revert to `DRAFT` instead of `VALIDATED`), only `addWeatherData` changes; the controller, UI and repositories are unaffected.

The repository interfaces protect the domain from persistence changes (in-memory vs JPA).

---

## 3. SOLID

### 3.1 Single Responsibility Principle (SRP)

| Class | Single Responsibility |
|-------|----------------------|
| `FlightPlan` | Hold flight plan data; own the weather set and the status transitions |
| `InsertWeatherDataController` | Orchestrate the insert-weather-data use case |
| `InsertWeatherDataUI` | Collect the pilot's selection and display the result |
| `FlightPlanRepository` | Persist and retrieve flight plans |
| `WeatherDataRepository` | Retrieve weather data |
| `PilotRepository` | Retrieve pilots |

---

### 3.2 Open/Closed Principle (OCP)

The `FlightPlan` aggregate was **extended** with the weather-data behaviour (new field + `addWeatherData`) without modifying the existing constructors or the `markValidated()` / `markTested()` transitions — existing behaviour (US080/US081/US085) is closed to modification, open to extension.

---

### 3.3 Dependency Inversion Principle (DIP)

`InsertWeatherDataController` depends on repository **interfaces**, not on concrete implementations:

```java
private final FlightPlanRepository flightPlanRepo = PersistenceContext.repositories().flightPlans();
private final WeatherDataRepository weatherDataRepo = PersistenceContext.repositories().weatherData();
private final PilotRepository pilotRepo = PersistenceContext.repositories().pilots();
```

The `PersistenceContext` injects the correct implementations at runtime.

---

## 4. GoF — Gang of Four Design Patterns

### 4.1 Facade

`InsertWeatherDataController` is a Facade for the use case — it hides authorization, pilot resolution, ownership and existence checks, the domain operation and persistence behind a single `insertWeatherData(...)` method.

---

### 4.2 Factory Method

`PersistenceContext.repositories().flightPlans()` / `.weatherData()` / `.pilots()` are factory methods returning the configured repository implementation (in-memory or JPA) without the controller knowing which one.

---

### 4.3 Template Method (via EAPLI)

`InMemoryFlightPlanRepository` and `JpaFlightPlanRepository` extend EAPLI base classes (`InMemoryDomainRepository`, `JpaAutoTxRepository`) that provide the `save()`, `findAll()`, `ofIdentity()` algorithm skeleton; the flight plan repositories reuse it.

---

## 5. Summary Table

| Principle / Pattern | Category | Where in US082 |
|---------------------|----------|----------------|
| Aggregate Root | DDD | `FlightPlan implements AggregateRoot<FlightPlanDesignator>` |
| Identity references | DDD | `Set<Long>` weather ids, `Long` assigned pilot id |
| Repository | DDD | `FlightPlanRepository`, `WeatherDataRepository`, `PilotRepository` interfaces |
| Invariants | DDD | `addWeatherData` voids the test only on genuinely new data |
| Low Coupling between Aggregates | DDD | References by id; no object references |
| Information Expert | GRASP | `FlightPlan` owns set + status; controller coordinates cross-aggregate rules |
| Controller | GRASP | `InsertWeatherDataController` |
| Low Coupling | GRASP | No cascade, identity references, single-aggregate write |
| High Cohesion | GRASP | `addWeatherData` is one cohesive operation |
| Protected Variations | GRASP | Status lifecycle encapsulated in the aggregate/enum |
| SRP | SOLID | Each class has one reason to change |
| OCP | SOLID | `FlightPlan` extended without modifying existing behaviour |
| DIP | SOLID | Controller depends on repository interfaces |
| Facade | GoF | `InsertWeatherDataController` hides use case complexity |
| Factory Method | GoF | `PersistenceContext.repositories().*` |
| Template Method | GoF | EAPLI base repositories provide the algorithm skeleton |
