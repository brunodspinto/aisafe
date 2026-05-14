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

**US050** As a Backoffice Operator, I want to register an air control area.

**Acceptance Criteria:**

- **AC050.1** The system must allow the Backoffice Operator to register a new air control area by providing a unique area code, a name, the minimum fuel required, and its geographic boundaries.
- **AC050.2** The area code must be globally unique in the system (it is the aggregate identity).
- **AC050.3** The geographic boundaries must be logically valid (north latitude must be strictly greater than south latitude; latitudes within −90 to 90; longitudes within −180 to 180).
- **AC050.4** The registration must also be achievable by a bootstrap process upon system startup.

**Dependencies/References:**

- US030 — Authentication and Authorization must be implemented first.
- No domain prerequisites.
- Acts as a prerequisite for:
  - US041 — Register Weather Data (weather data is geographically bound to an area)
  - US052 — Create an Airport (an airport belongs to an air control area)

---

## 3. Analysis

The `AirControlArea` aggregate was designed following DDD principles. Its identity is the `areaCode` string, which must be globally unique. The geographic boundary logic is fully encapsulated in the `GeoBoundary` Value Object, keeping the aggregate root clean and cohesive.

The `AirControlAreaCode` was considered as a dedicated Value Object but discarded — the code remains a primitive `String`, with uniqueness enforced in the controller before saving and backed by the JPA `@Id` constraint.

The main classes involved are:

| Class | Type | Responsibility |
|-------|------|----------------|
| `AirControlArea` | Entity / Aggregate Root | Holds area code (identity), name, minimum fuel, and geographic boundaries |
| `GeoBoundary` | Value Object | Validates and stores geographic coordinates (north/south latitude, east/west longitude) |
| `AirControlAreaRepository` | Repository Interface | Persistence contract for the AirControlArea aggregate |
| `RegisterAirControlAreaController` | Application Controller | Orchestrates the use case; enforces uniqueness and `BACKOFFICE_OPERATOR` role |
| `RegisterAirControlAreaUI` | UI | Collects area code, name, fuel, and boundary coordinates from the operator |

The following domain model excerpt shows the aggregate structure:

![Domain Model](svg/US050-domain-model.svg)

---

## 4. Design

### 4.1. Realization

1. The UI (`RegisterAirControlAreaUI`) prompts the operator for the area code, name, minimum fuel required, and geographic boundaries (north/south latitude, east/west longitude).
2. Input is validated inline before calling the controller (non-blank strings, numeric values within valid ranges).
3. The controller calls `authz.ensureAuthenticatedUserHasAnyOf(BACKOFFICE_OPERATOR)`.
4. The controller checks that no area with the same code already exists via `AirControlAreaRepository`.
5. The controller instantiates `GeoBoundary` — spatial invariants are enforced inside the constructor.
6. The controller instantiates `AirControlArea` with the validated data — remaining domain invariants are enforced inside the constructor.
7. The area is persisted via `AirControlAreaRepository.save()`.
8. The UI confirms success: `Air Control Area '...' registered successfully.`

The following sequence diagram illustrates the flow:

![Sequence Diagram](svg/US050-SD.svg)

The following class diagram shows the classes involved:

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

The implementation is distributed across the following packages in `aisafe.base`:

| Package | Class | Role |
|---------|-------|------|
| `aisafe.aircontrolarea.domain` | `AirControlArea` | Aggregate root, table `T_AIR_CONTROL_AREA` |
| `aisafe.aircontrolarea.domain` | `GeoBoundary` | Value object (`@Embeddable`) — geographic boundary validation |
| `aisafe.aircontrolarea.repositories` | `AirControlAreaRepository` | Repository interface |
| `aisafe.aircontrolarea.application` | `RegisterAirControlAreaController` | Use case orchestrator |
| `aisafe.infrastructure.persistence.inmemory` | `InMemoryAirControlAreaRepository` | In-memory persistence |
| `aisafe.infrastructure.persistence.jpa` | `JpaAirControlAreaRepository` | JPA persistence |
| `aisafe.app.console.presentation.aircontrolarea` | `RegisterAirControlAreaUI` | Console UI |

`RepositoryFactory` must expose an `airControlAreas()` method, and both `InMemoryRepositoryFactory` and `JpaRepositoryFactory` must provide implementations. Bootstrap support is implemented in `AiSafeBootstrap`, which seeds a default valid air control area if one does not already exist.

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

- `GeoBoundary` acts as an autonomous validator. By delegating all spatial logic to this Value Object, the `AirControlArea` aggregate root remains clean and highly cohesive.
- `GeoBoundary` is mapped with `@Embeddable` / `@Embedded` inside `AirControlArea`, avoiding unnecessary table joins and keeping boundary data co-located with the area record.
- `AirControlAreaCode` was considered as a dedicated Value Object but discarded in favour of a plain `String` identity, which is sufficient given the simple format constraint. Uniqueness is enforced both in the controller (pre-save lookup) and at the database level via the JPA `@Id` constraint.
- The controller checks for duplicate codes before instantiating the aggregate, providing a user-friendly error rather than relying solely on the database constraint exception.