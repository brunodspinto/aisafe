# US041 — Register Weather Data

## 1. Context

This US allows the Weather Person to register weather data in the AISafe system for a specific air control area.

### 1.1 List of Issues

- **Analysis:** Define the domain model for the `WeatherData` aggregate and its external relationship with the `AirControlArea`.
- **Design:** Design the standard "Register X" sequence diagram and class diagram for the domain and persistence layers, including the querying of existing areas.
- **Implement:** Create the `WeatherData` entity, JPA repositories, Controller, UI, and enforce authorization.
- **Test:** Unit tests for `WeatherData` instantiation and business validations.

---

## 2. Requirements

### US041
As a Weather Person, I want to register weather data in the system for a specific air control area.

### Acceptance Criteria

- **US041.1:** The system must allow the Weather Person to register weather information (e.g., date, temperature, wind speed, wind direction, pressure, visibility).
- **US041.2:** The weather data must be explicitly linked to a pre-existing Air Control Area in the system.
- **US041.3:** The user performing this action must be authenticated and have the `WEATHER_PERSON` role.

### Dependencies / References

- Requires **US050** (Register an Air Control Area), as weather data is geographically bound to an area.
- Requires **US030 / US031** (Authentication and Authorization) to validate the Weather Person role.

---

## 3. Analysis

The `WeatherData` aggregate was designed following Domain-Driven Design (DDD) principles to ensure low coupling and high cohesion.

### Main Components

- **WeatherData** — Aggregate root representing the meteorological conditions at a specific time.
- **WeatherSource** — Value object encapsulating the origin and format of the weather data provider.

### Architectural Decision

To maintain low coupling and avoid loading heavy spatial data unnecessarily, `WeatherData` does not encapsulate the `AirControlArea` entity. Instead, it holds an external reference (`areaCode`) as a primitive `String`.

### Business Rules

The `WeatherData` ensures that:

- Wind speed cannot be negative.
- Visibility cannot be negative.
- The Air Control Area reference cannot be null or empty.
- The date and time of the record are valid.

### Persistence (JPA)

- `WeatherSource` → `@Embeddable`
- Used inside `WeatherData` with `@Embedded`
- Avoids complex ORM mapping (`@ManyToOne`) by keeping `areaCode` as a simple column.

### Domain Model

![Domain Model](svg/US041-domain-model.svg)

---

## 4. Design

### 4.1 Realization

This use case follows the standard "Register X" architectural pattern.

The controller must query the `AirControlAreaRepository` first, allowing the user to select the target area.

---
### Sequence Diagram

![Sequence Diagram](svg/US041-SD.svg)


### Class Diagram

![Class Diagram](svg/US041-class-diagram.svg)

---

### 4.2 Acceptance Tests

Detailed coverage is documented in `tests.md`.

---

## 5. Implementation

### Key Implementation Details

- `WeatherData` → Annotated with `@Entity` and implements `AggregateRoot<Long>`
- `areaCode` → Simple `@Column` (external reference)
- `WeatherSource` → Implements `ValueObject` and uses `@Embeddable`
- Mapped using `@Embedded` inside `WeatherData`

### Repositories

- `InMemoryWeatherDataRepository` (testing)
- `JpaWeatherDataRepository` (production)

### Security

- `AuthorizationService` ensures only users with `WEATHER_PERSON` role can execute the operation.

#### 6. Integration / Demonstration

##### Run Instructions

To test this functionality, ensure the system has been bootstrapped first so that at least one Air Control Area exists in the database.

```bash
# Run bootstrap (creates initial data, including Air Control Areas and default users)
./run-bootstrap.sh

# Run backoffice
./run-backoffice.sh

# Login with:
# Username: weather_person (or the specific username created in your bootstrap)
# Password: Password1

# Navigate to: 
# Weather Management -> Register Weather Data

````
---

## 7. Observations

- The system uses a `WeatherDataRepository` (with a JPA implementation `JpaWeatherDataRepository`).
- It also uses an `AirControlAreaRepository` obtained via the `RepositoryFactory`.
- This demonstrates how the Application Layer controller orchestrates:
    - Reading from one aggregate’s repository (`AirControlArea`)
    - Feeding that data (external reference `areaCode`)
    - Into the creation of a new aggregate (`WeatherData`)
