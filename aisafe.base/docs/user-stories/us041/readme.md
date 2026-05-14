# US041 — Register Weather Data

## 1. Context

This US is implemented in Sprint 2 and allows the Weather Person to register weather data in the AISafe system for a specific air control area. It depends on US030 (Authentication and Authorization), which must be in place so that only an authenticated Weather Person can invoke this feature.

The `WeatherData` is an independent aggregate whose creation is bound to the prior existence of an `AirControlArea` (US050). This use case acts as a foundational dependency for flight safety assessments that require up-to-date meteorological conditions.

---

## 2. Requirements

**US041** As a Weather Person, I want to register weather data in the system for a specific air control area.

**Acceptance Criteria:**

- **AC041.1** The system must allow the Weather Person to register weather information (date, temperature, wind speed, wind direction, pressure, visibility, and source).
- **AC041.2** The weather data must be explicitly linked to a pre-existing Air Control Area in the system.
- **AC041.3** Only an authenticated Weather Person (`WEATHER_PERSON` role) may perform this action.

**Dependencies/References:**

- US030 — Authentication and Authorization must be implemented first.
- US050 — Register an Air Control Area (weather data is geographically bound to an existing area).
- Acts as a prerequisite for:
  - Flight safety assessments that consume meteorological conditions per area.

---

## 3. Analysis

The `WeatherData` aggregate was designed following DDD principles. Its identity is a surrogate `Long` key generated at persistence time. To maintain low coupling and avoid loading heavy spatial data unnecessarily, `WeatherData` does not encapsulate the `AirControlArea` entity — instead it holds an external reference (`areaCode`) as a primitive `String`, normalised to uppercase on construction.

The main classes involved are:

| Class | Type | Responsibility |
|-------|------|----------------|
| `WeatherData` | Entity / Aggregate Root | Holds area code reference, date/time, wind speed, wind direction, pressure, and visibility |
| `WeatherSource` | Value Object | Encapsulates the origin and format of the weather data provider |
| `WeatherDataRepository` | Repository Interface | Persistence contract for the WeatherData aggregate |
| `AirControlAreaRepository` | Repository Interface | Used to list available areas for user selection |
| `RegisterWeatherDataController` | Application Controller | Orchestrates the use case; enforces `WEATHER_PERSON` role |
| `RegisterWeatherDataUI` | UI | Collects weather fields and target area selection from the operator |

The following domain model excerpt shows the aggregate structure:

![Domain Model](svg/US041-domain-model.svg)

---

## 4. Design

### 4.1. Realization

1. The UI (`RegisterWeatherDataUI`) lists available Air Control Areas for the operator to select.
2. The controller calls `authz.ensureAuthenticatedUserHasAnyOf(WEATHER_PERSON)`.
3. The controller fetches the list of active Air Control Areas via `AirControlAreaRepository`.
4. The operator selects the target area and provides the weather fields: date, temperature, wind speed, wind direction, pressure, visibility, and source.
5. Input is validated inline before calling the controller (non-null date, non-negative numeric values).
6. The controller instantiates `WeatherData` with the validated data — domain invariants are enforced inside the constructor.
7. The record is persisted via `WeatherDataRepository.save()`.
8. The UI confirms success: `Weather data registered successfully for area '...'.`

The following sequence diagram illustrates the flow:

![Sequence Diagram](svg/US041-SD.svg)

The following class diagram shows the classes involved:

![Class Diagram](svg/US041-class-diagram.svg)

---

### 4.2. Acceptance Tests

All tests are automated with JUnit 5 and located in `src/test/java/aisafe/weatherdata/domain/WeatherDataTest.java`.

---

**AC041.1 — Valid weather data creation**

**Test:** `ensureValidWeatherDataIsCreated`

```java
@Test
void ensureValidWeatherDataIsCreated() {
    final WeatherData data = new WeatherData(
            "LPPT", LocalDateTime.now(), 22.5, 15.0, "N", 1013.0, 9000.0,
            new WeatherSource("IPMA", "METAR"));
    assertNotNull(data);
}
```

**Test:** `ensureWindSpeedOfZeroIsAccepted`

```java
@Test
void ensureWindSpeedOfZeroIsAccepted() {
    final WeatherData data = new WeatherData(
            "LPPT", LocalDateTime.now(), 22.5, 0.0, "N", 1013.0, 9000.0,
            new WeatherSource("IPMA", "METAR"));
    assertEquals(0.0, data.windSpeed());
}
```

**Test:** `ensureVisibilityOfZeroIsAccepted`

```java
@Test
void ensureVisibilityOfZeroIsAccepted() {
    final WeatherData data = new WeatherData(
            "LPPT", LocalDateTime.now(), 22.5, 15.0, "N", 1013.0, 0.0,
            new WeatherSource("IPMA", "METAR"));
    assertEquals(0.0, data.visibility());
}
```

**Test:** `ensureNegativeWindSpeedIsRejected`

```java
@Test
void ensureNegativeWindSpeedIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new WeatherData(
            "LPPT", LocalDateTime.now(), 22.5, -1.0, "N", 1013.0, 9000.0,
            new WeatherSource("IPMA", "METAR")));
}
```

**Test:** `ensureNegativeVisibilityIsRejected`

```java
@Test
void ensureNegativeVisibilityIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new WeatherData(
            "LPPT", LocalDateTime.now(), 22.5, 15.0, "N", 1013.0, -1.0,
            new WeatherSource("IPMA", "METAR")));
}
```

---

**AC041.2 — Air Control Area reference**

**Test:** `ensureAreaCodeCannotBeNull`

```java
@Test
void ensureAreaCodeCannotBeNull() {
    assertThrows(IllegalArgumentException.class, () -> new WeatherData(
            null, LocalDateTime.now(), 22.5, 15.0, "N", 1013.0, 9000.0,
            new WeatherSource("IPMA", "METAR")));
}
```

**Test:** `ensureAreaCodeCannotBeBlank`

```java
@Test
void ensureAreaCodeCannotBeBlank() {
    assertThrows(IllegalArgumentException.class, () -> new WeatherData(
            "   ", LocalDateTime.now(), 22.5, 15.0, "N", 1013.0, 9000.0,
            new WeatherSource("IPMA", "METAR")));
}
```

**Test:** `ensureAreaCodeIsNormalisedToUppercase`

```java
@Test
void ensureAreaCodeIsNormalisedToUppercase() {
    final WeatherData data = new WeatherData(
            "lppt", LocalDateTime.now(), 22.5, 15.0, "N", 1013.0, 9000.0,
            new WeatherSource("IPMA", "METAR"));
    assertEquals("LPPT", data.areaCode());
}
```

**Test:** `ensureDateCannotBeNull`

```java
@Test
void ensureDateCannotBeNull() {
    assertThrows(IllegalArgumentException.class, () -> new WeatherData(
            "LPPT", null, 22.5, 15.0, "N", 1013.0, 9000.0,
            new WeatherSource("IPMA", "METAR")));
}
```

**Test:** `ensureSourceCannotBeNull`

```java
@Test
void ensureSourceCannotBeNull() {
    assertThrows(IllegalArgumentException.class, () -> new WeatherData(
            "LPPT", LocalDateTime.now(), 22.5, 15.0, "N", 1013.0, 9000.0, null));
}
```

---

**AC041.3 — Authorization**

**Test:** `ensureOnlyWeatherPersonCanRegisterWeatherData`

```java
@Test
void ensureOnlyWeatherPersonCanRegisterWeatherData() {
    when(authz.hasRole(WEATHER_PERSON)).thenReturn(false);

    final RegisterWeatherDataController controller = new RegisterWeatherDataController();
    assertThrows(UnauthorizedException.class,
            () -> controller.registerWeatherData("LPPT", LocalDateTime.now(),
                    22.5, 15.0, "N", 1013.0, 9000.0, "IPMA", "METAR"));
}
```

---

## 5. Implementation

The implementation is distributed across the following packages in `aisafe.base`:

| Package | Class | Role |
|---------|-------|------|
| `aisafe.weatherdata.domain` | `WeatherData` | Aggregate root, table `T_WEATHER_DATA` |
| `aisafe.weatherdata.domain` | `WeatherSource` | Value object (`@Embeddable`) — weather data provider origin and format |
| `aisafe.weatherdata.repositories` | `WeatherDataRepository` | Repository interface |
| `aisafe.weatherdata.application` | `RegisterWeatherDataController` | Use case orchestrator |
| `aisafe.infrastructure.persistence.inmemory` | `InMemoryWeatherDataRepository` | In-memory persistence |
| `aisafe.infrastructure.persistence.jpa` | `JpaWeatherDataRepository` | JPA persistence |
| `aisafe.app.console.presentation.weatherdata` | `RegisterWeatherDataUI` | Console UI |

`RepositoryFactory` must expose a `weatherData()` method, and both `InMemoryRepositoryFactory` and `JpaRepositoryFactory` must provide implementations.

---

## 6. Integration/Demonstration

**Prerequisites:** Run from the `aisafe.base` directory with Maven 3.9+ and Java 21. The bootstrap must have been executed first so that at least one Air Control Area and the `weather_person` user exist in the database.

**To register weather data:**

1. Login with Weather Person credentials (username: `weather_person`, password: `Password1`).
2. Select **Weather > Register Weather Data** from the main menu.
3. Select an existing Air Control Area from the presented list (e.g., `LPPT`).
4. Enter the date and time of the observation (e.g., `2025-05-14 10:00`).
5. Enter the weather fields: temperature, wind speed, wind direction, pressure, and visibility.
6. Enter the data source name and format (e.g., `IPMA` / `METAR`).
7. The system confirms: `Weather data registered successfully for area 'LPPT'.`

---

## 7. Observations

- `WeatherData` uses a surrogate `Long` identity (`@GeneratedValue`) because no natural business key uniquely identifies a weather record — the same area can have multiple records at different times.
- `areaCode` is stored as a simple `@Column` string rather than a `@ManyToOne` join, avoiding loading heavy spatial data when working with weather records and preserving Low Coupling between aggregates.
- `WeatherSource` is mapped with `@Embeddable` / `@Embedded` inside `WeatherData`, keeping the provider metadata co-located without requiring an extra table.
- The controller queries `AirControlAreaRepository` to present a validated list of areas; this prevents the operator from entering a free-text code that does not correspond to any existing area.
