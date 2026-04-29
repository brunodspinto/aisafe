# US 50 - Register an Air Control Area

## 1. Context

*This US allows the Backoffice Operator to register a new air control area in the AISafe system.*

### 1.1 List of Issues

- **Analysis:** Define the domain model for the AirControlArea aggregate and identify its invariants (unique code, valid geographic boundaries).
- **Design:** Design the standard "Register X" sequence diagram and class diagram for the domain and persistence layers.
- **Implement:** Create the `AirControlArea` entity, `GeoBoundary` value object, JPA repositories, Controller, UI, and the Bootstrap process.
- **Test:** Unit tests for `AirControlArea` instantiation and `GeoBoundary` spatial validations.

---

## 2. Requirements

**US050**  
As a Backoffice Operator, I want to register an air control area. The area code must be unique in the system. Geographic boundaries must be valid. This must also be achievable by a bootstrap process.

### Acceptance Criteria

- **US050.1:** The system must allow the Backoffice Operator to register a new air control area by providing a unique area code and its geographic boundaries.
- **US050.2:** The area code must be globally unique in the system.
- **US050.3:** The geographic boundaries must be logically valid (e.g., North latitude cannot be inferior to South latitude).
- **US050.4:** The registration must be achievable by a bootstrap process upon system startup.

### Dependencies / References

- No prior dependencies.
- Acts as a foundational dependency for:
    - **US052** (Create an Airport)
    - **US041** (Register Weather Data)

---

## 3. Analysis

The `AirControlArea` aggregate was designed following Domain-Driven Design (DDD) principles to ensure low coupling and high cohesion.

### Main Components

- `AirControlArea` — Aggregate root representing the airspace.
- `AirControlAreaCode` — Value object acting as business identity with validation.
- `GeoBoundary` — Value object encapsulating geographic coordinates.

The `GeoBoundary` ensures that:

- North latitude > South latitude
- Coordinates form a valid rectangular area

### Persistence (JPA)

- `GeoBoundary` → `@Embeddable`
- Used inside `AirControlArea` with `@Embedded`
- Avoids unnecessary joins

### Domain Model


> **Note:** To be added.

---

## 4. Design

### 4.1 Realization

This use case follows the standard **"Register X"** architectural pattern.

### Sequence Diagram


> **Note:** To be added.

### Class Diagram


> **Note:** To be added.

---

### 4.2 Acceptance Tests

#### Test 1 — Invalid GeoBoundary

Verifies that invalid latitude values are rejected.

**Covers:** US050.3

```java
@Test(expected = IllegalArgumentException.class)
public void ensureGeoBoundaryCannotHaveInvalidLatitudes() {
    new GeoBoundary(30.0, 40.0, -10.0, 10.0);
}
```
### Test 2 — Invalid AirControlArea

Verifies that null code or boundaries are not allowed.

```java
@Test(expected = IllegalArgumentException.class)
public void ensureAirControlAreaMustHaveValidCodeAndBoundaries() {
    new AirControlArea(null, new GeoBoundary(40.0, 30.0, -10.0, 10.0));
}
````

## 5. Implementation

### Key Implementation Details

- `AirControlArea` → `@Entity`
- `AirControlAreaCode` → `@Column(unique = true)`
- `GeoBoundary` → `@Embeddable`
- Used with `@Embedded` inside `AirControlArea`
- Created `AirControlAreaBootstrapper` to support system startup initialization

## 6. Integration / Demonstration

### Run Instructions

```bash
# Run bootstrap (creates initial data)
./run-bootstrap.sh

# Run backoffice
./run-backoffice.sh

# Login with:
# Username: admin (or a backoffice operator credentials)
# Password: Password1

````
## 7. Observations

The `GeoBoundary` acts as an autonomous validator. By delegating the spatial logic to this Value Object, the `AirControlArea` aggregate root remains clean and highly cohesive.

The system uses an `AirControlAreaRepository` (with a JPA implementation `JpaAirControlAreaRepository`) obtained via the `RepositoryFactory` to keep the application layer decoupled from the persistence technology framework.