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

**US050** As a Backoffice Operator, I want to register an air control area. The area code must be unique in the system. Geographic boundaries must be valid. This must also be achievable by a bootstrap process.

### Acceptance Criteria

- **US050.1:** The system must allow the Backoffice Operator to register a new air control area by providing a unique area code, a name, the minimum fuel required, and its geographic boundaries.
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
- `GeoBoundary` — Value object encapsulating geographic coordinates.

> **Architectural Decision:** Initially considered, the `AirControlAreaCode` was discarded as a dedicated Value Object. Since its only business rule is "uniqueness", this validation is better delegated to the database constraint (Primary Key). Thus, `areaCode` is modeled as a primitive `String`.

The `GeoBoundary` ensures that:
- North latitude > South latitude
- Latitudes are between -90 and 90
- Longitudes are between -180 and 180

### Persistence (JPA)

- `GeoBoundary` → `@Embeddable`
- Used inside `AirControlArea` with `@Embedded`
- Avoids unnecessary joins by keeping the boundaries in the same table as the Air Control Area.


### Domain Model

![Domain Model](svg/US050-domain-model.svg)

---

## 4. Design

### 4.1 Realization

This use case follows the standard "Register X" architectural pattern. Following the Application Engineering Process guidelines, the Sequence Diagram focuses on the core domain orchestration and omits the explicit fetching of the Persistence Context to avoid UML programming boilerplate.

### Sequence Diagram

![Sequence Diagram](svg/US050-SD.svg)


### Class Diagram

![Class Diagram](svg/US050-class-diagram.svg)


---

### 4.2 Acceptance Tests

Detailed coverage is documented in [tests.md](tests.md).

## 5. Implementation

### Key Implementation Details

- `AirControlArea` → Annotated with `@Entity` and implements `AggregateRoot<String>`.
- `areaCode` → Annotated with `@Id` and normalized before persistence; the controller rejects duplicates before save.
- `GeoBoundary` → Implements `ValueObject` and uses `@Embeddable`.
- Mapped inside `AirControlArea` using the `@Embedded` annotation.
- Two repository implementations were provided:
    - `InMemoryAirControlAreaRepository` (for testing)
    - `JpaAirControlAreaRepository` (for production)

Bootstrap support is implemented in `AiSafeBootstrap`, which seeds a default valid air control area if it does not already exist.

## 6. Integration / Demonstration

### Run Instructions

```bash
# Run bootstrap (creates initial data)
./run-bootstrap.sh

# Run backoffice
./run-backoffice.sh

# Login with:
# Username: admin (or a backoffice operator account)
# Password: Password1

# Navigate to: 
# Air Control Management -> Register Air Control Area

````
## 7. Observations

The `GeoBoundary` acts as an autonomous validator. By delegating the spatial logic to this Value Object, the `AirControlArea` aggregate root remains clean and highly cohesive.

The system uses an `AirControlAreaRepository` (with a JPA implementation `JpaAirControlAreaRepository`) obtained via the `RepositoryFactory` to keep the application layer decoupled from the persistence technology framework.