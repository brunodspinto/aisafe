# US050 - Register an Air Control Area

## 1. Context

*This US allows the Backoffice Operator to register a new air control area in the AISafe system.*

### 1.1 List of Issues

- **Analysis:** Define the domain model for the AirControlArea aggregate and identify its invariants (unique code, valid geographic boundaries).
- **Design:** Design the standard "Register X" sequence diagram and class diagram for the domain and persistence layers.
- **Implement:** Create the `AirControlArea` entity, `GeoBoundary` value object, JPA repositories, Controller, UI, and the Bootstrap process.
- **Test:** Unit tests for `AirControlArea` instantiation and `GeoBoundary` spatial validations, plus manual acceptance tests for the UI and bootstrap flow.

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

> **Architectural Decision:** Initially considered, the `AirControlAreaCode` was discarded as a dedicated Value Object. The code remains a primitive `String`, but uniqueness is enforced explicitly in the controller before saving and is also backed by the database primary key.

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

All automated coverage is implemented with JUnit 5 and is split across:

- `src/test/java/aisafe/aircontrolarea/domain/GeoBoundaryTest.java`
- `src/test/java/aisafe/aircontrolarea/domain/AirControlAreaTest.java`

**Manual test — US050.1 (register with valid data):**

1. Run `AiSafeConsoleApp` and login as `admin` (or a BACKOFFICE_OPERATOR).
2. Navigate to `Air Control Areas > 1 — Register Air Control Area`.
3. Enter a unique area code (e.g., `PT-S`).
4. Enter a name (e.g., `Southern Portugal`).
5. Enter minimum fuel required (e.g., `500`).
6. Enter geographic boundaries: North Latitude: 41.5, South Latitude: 37.0, West Longitude: -10.0, East Longitude: -6.0.
7. Confirm the registration.
8. Expected: Success message; area is registered and saved to the database.

**Manual test — US050.2 (unique area code):**

1. Perform the steps above to register an area with code `PT-N`.
2. Attempt to register another area with the same code `PT-N`.
3. Expected: The system rejects the operation with an error message stating the code already exists.

**Manual test — US050.3 (valid geographic boundaries):**

1. Try to register an area with North Latitude: 37.0 and South Latitude: 41.5 (reversed).
2. Expected: System rejects with an error: "North latitude must be greater than South latitude.".
3. Try to register with Latitude: 91.0 (outside range).
4. Expected: System rejects with an error: "Latitude must be between -90 and 90.".
5. Try to register with Longitude: -181.0 (outside range).
6. Expected: System rejects with an error: "Longitude must be between -180 and 180.".

**Manual test — US050.4 (bootstrap registration):**

1. Run `./run-bootstrap.sh` to initialize the system with seed data.
2. Check the database or logs to confirm a default air control area (e.g., `PT-N`) was created.
3. Run the bootstrap again without clearing data.
4. Expected: No duplicate `PT-N` area is inserted; the system handles the idempotent bootstrap gracefully.

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

```
## 7. Observations

The `GeoBoundary` acts as an autonomous validator. By delegating the spatial logic to this Value Object, the `AirControlArea` aggregate root remains clean and highly cohesive.

The system uses an `AirControlAreaRepository` (with a JPA implementation `JpaAirControlAreaRepository`) obtained via the `RepositoryFactory` to keep the application layer decoupled from the persistence technology framework.