# US043 - Principles and Patterns Applied

## 1. DDD - Domain Driven Design

### 1.1 Aggregate Root

`WeatherData` is the aggregate root for meteorological readings. US043 does not create or mutate the aggregate; it queries existing `WeatherData` records for a selected day and air control area.

```java
public class WeatherData implements AggregateRoot<Long> {}
```

---

### 1.2 Value Object

`AirControlAreaCode` is a value object used to reference the `AirControlArea` aggregate from `WeatherData` without embedding the full area entity.

```java
@Embeddable
public class AirControlAreaCode implements ValueObject, Comparable<AirControlAreaCode> {
    private String value;
}
```

`WeatherSource` is also a value object. It encapsulates the weather data provider and format and is owned by the `WeatherData` aggregate.

```java
@Embeddable
public class WeatherSource implements ValueObject {
    private String provider;
    private String format;
}
```

---

### 1.3 Repository

`WeatherDataRepository` is the domain repository interface for the `WeatherData` aggregate. US043 extends it with a query focused on the acceptance criteria: selected date and selected air control area.

```java
public interface WeatherDataRepository extends DomainRepository<Long, WeatherData> {
    Iterable<WeatherData> findByDateAndAirControlArea(LocalDate date, AirControlAreaCode areaCode);
}
```

Both in-memory and JPA implementations provide the same query contract, so the application layer is isolated from persistence details.

---

### 1.4 Low Coupling between Aggregates

`WeatherData` references an air control area by `AirControlAreaCode`, not by a direct `AirControlArea` object reference. This avoids coupling the weather aggregate lifecycle to the air control area aggregate.

```java
@Embedded
@AttributeOverride(name = "value", column = @Column(name = "area_code", nullable = false))
private AirControlAreaCode areaCode;
```

The controller validates that the area exists through `AirControlAreaRepository` before querying weather data.

---

### 1.5 Query Boundary

US043 is day-based, while `WeatherData` stores `LocalDateTime`. The JPA repository keeps this rule in the persistence query by using a closed-open interval: selected day at `00:00` inclusive to next day at `00:00` exclusive.

```java
return match("e.areaCode.value = :areaCode AND e.date >= :start AND e.date < :end", params);
```

---

## 2. GRASP - General Responsibility Assignment Software Patterns

### 2.1 Information Expert

`WeatherData` owns the meteorological information that must be displayed: source, timestamp, temperature, wind speed, wind direction, pressure, and visibility.

`AirControlAreaCode` owns normalization of the area code, so the controller and repositories compare normalized values.

---

### 2.2 Controller

`ConsultWeatherDataController` is the application-layer controller for US043. It:

- Verifies authorization for `WEATHER_PERSON`, `PILOT`, and `FLIGHT_CONTROL_OPERATOR`.
- Lists available air control areas.
- Validates the selected area code.
- Delegates the day-and-area query to `WeatherDataRepository`.

```java
@UseCaseController
public class ConsultWeatherDataController {
    public Iterable<WeatherData> consultWeatherData(final LocalDate date, final String areaCode) { ... }
}
```

---

### 2.3 Low Coupling

The UI depends only on `ConsultWeatherDataController`. The controller depends on repository interfaces and not on concrete persistence implementations. `WeatherData` depends on `AirControlAreaCode`, not on the full `AirControlArea` aggregate.

---

### 2.4 High Cohesion

- `WeatherData` - stores and validates meteorological readings.
- `WeatherSource` - stores provider and format information.
- `ConsultWeatherDataController` - orchestrates the consult weather data use case.
- `ConsultWeatherDataUI` - collects query input and displays results.
- `WeatherDataRepository` - defines weather data persistence/query operations.
- `InMemoryWeatherDataRepository` and `JpaWeatherDataRepository` - implement the same query for their own storage mechanisms.

---

### 2.5 Protected Variations

The `WeatherDataRepository` interface protects the application layer from persistence changes. A future database, API-backed weather store, or optimized query can be introduced without changing the controller or UI.

The day-query rule is localized in repository implementations, so changes to date filtering do not spread through presentation code.

---

## 3. SOLID

### 3.1 Single Responsibility Principle (SRP)

| Class | Single Responsibility |
|-------|----------------------|
| `WeatherData` | Hold weather readings and enforce weather data invariants |
| `WeatherSource` | Hold weather data provider and format |
| `AirControlAreaCode` | Normalize and represent the area identity |
| `ConsultWeatherDataController` | Orchestrate the consultation use case |
| `ConsultWeatherDataUI` | Collect user filters and display weather data |
| `WeatherDataRepository` | Define the weather persistence/query contract |
| `InMemoryWeatherDataRepository` | Implement weather queries in memory |
| `JpaWeatherDataRepository` | Implement weather queries in JPA |

---

### 3.2 Interface Segregation Principle (ISP)

`WeatherDataRepository` exposes only the query needed by this use case in addition to inherited repository operations. It does not force unrelated weather reporting or import operations into the same interface.

---

### 3.3 Dependency Inversion Principle (DIP)

`ConsultWeatherDataController` depends on repository interfaces:

```java
private final AirControlAreaRepository areaRepository =
        PersistenceContext.repositories().airControlAreas();

private final WeatherDataRepository weatherDataRepository =
        PersistenceContext.repositories().weatherData();
```

Concrete implementations are supplied by `PersistenceContext` at runtime.

---

## 4. GoF - Gang of Four Design Patterns

### 4.1 Facade

`ConsultWeatherDataController` acts as a Facade for the use case by hiding authorization, area validation, and repository query details behind simple methods for the UI.

```java
public Iterable<AirControlArea> activeAirControlAreas()
public Iterable<WeatherData> consultWeatherData(LocalDate date, String areaCode)
```

---

### 4.2 Factory Method

`PersistenceContext.repositories().weatherData()` and `.airControlAreas()` are factory methods that return the configured repository implementations without exposing construction logic to the controller.

---

### 4.3 Template Method (via EAPLI)

`InMemoryWeatherDataRepository` and `JpaWeatherDataRepository` extend EAPLI repository base classes that provide the algorithm skeleton for common repository operations. Each subclass only implements the US043-specific query.

```java
public class JpaWeatherDataRepository
        extends JpaAutoTxRepository<WeatherData, Long, Long>
        implements WeatherDataRepository {}
```

---

## 5. Summary Table

| Principle / Pattern | Category | Where in US043 |
|---------------------|----------|----------------|
| Aggregate Root | DDD | `WeatherData implements AggregateRoot<Long>` |
| Value Object | DDD | `AirControlAreaCode`, `WeatherSource` |
| Repository | DDD | `WeatherDataRepository.findByDateAndAirControlArea(...)` |
| Low Coupling between Aggregates | DDD | `WeatherData` references area by `AirControlAreaCode` |
| Query Boundary | DDD | Day interval query in repository implementations |
| Information Expert | GRASP | `WeatherData` owns meteorological fields; `AirControlAreaCode` owns normalization |
| Controller | GRASP | `ConsultWeatherDataController` |
| Low Coupling | GRASP | UI -> controller -> repository interfaces |
| High Cohesion | GRASP | Each class has one focused role |
| Protected Variations | GRASP | Repository interface hides persistence changes |
| SRP | SOLID | Each US043 class has one reason to change |
| ISP | SOLID | Weather repository exposes focused query behavior |
| DIP | SOLID | Controller depends on repository interfaces |
| Facade | GoF | Controller hides use case complexity |
| Factory Method | GoF | `PersistenceContext.repositories()` |
| Template Method | GoF | EAPLI repository base classes |
