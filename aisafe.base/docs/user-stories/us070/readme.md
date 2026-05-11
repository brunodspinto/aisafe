# US070 — Add Aircraft to Air Transport Company

## 1. Context

This US is implemented in Sprint 2 and allows an Air Transport Company Collaborator (ATCC) to add a new physical aircraft to their company's fleet in the AISafe system. It depends on US030 (Authentication and Authorization), which must be in place so that only an authenticated ATCC can invoke this feature.

The `Aircraft` is an independent aggregate, but its creation is tightly coupled to the prior existence of an `AirTransportCompany` (US060) and `AircraftModel` (US055). This use case acts as a foundational dependency for US080 (Create a flight plan), which requires an active aircraft to be assigned to a flight.

---

## 2. Requirements

**US070** As an Air Transport Company Collaborator, I want to add an aircraft to my company’s fleet.

**Acceptance Criteria:**

- **AC070.1** The aircraft is of a given model, and the number of seats of each type/class must be provided.
- **AC070.2** The total number of seats configured in the cabin must not exceed the associated model’s maximum capacity.
- **AC070.3** An aircraft is identified by its registration number, which must be globally unique.
- **AC070.4** The aircraft must be registered to a country.
- **AC070.5** The aircraft has an operational status (e.g., ACTIVE).
- **AC070.6** Only an authenticated Air Transport Company Collaborator (ATCC) may perform this action.

**Dependencies/References:**

- US030 — Authentication and Authorization must be implemented first.
- US055 — Create an Aircraft Model (provides the catalog of available models and their max capacities).
- US060 — Register an Air Transport Company (provides the fleet where the aircraft will be added).
- Acts as a prerequisite for:
    - US071 — Decommission an Aircraft
    - US080 — Create a Flight Plan (a flight plan uses an aircraft from the fleet)

---

## 3. Analysis

The `Aircraft` aggregate was designed following DDD principles. Its identity is the aircraft `registration` number, which must be unique. To ensure low coupling between domains, the `Aircraft` references the `AircraftModel` strictly by its ID, and the `AirTransportCompany` references its owned aircraft by their registration IDs. The seat distribution logic is encapsulated within a `CabinConfiguration` Value Object.

The main classes involved are:

| Class                           | Type                    | Responsibility                                                                                    |
|---------------------------------|-------------------------|---------------------------------------------------------------------------------------------------|
| `Aircraft`                      | Entity / Aggregate Root | Holds registration (identity), registered country, operational status, and reference to the model |
| `CabinConfiguration`            | Value Object            | Validates and stores the number of seats (first, business, economy) and calculates total capacity |
| `AircraftRepository`            | Repository Interface    | Persistence contract for the Aircraft aggregate                                                   |
| `AirTransportCompanyRepository` | Repository Interface    | Used to fetch the ATCC's company and update its fleet                                             |
| `AircraftModelRepository`       | Repository Interface    | Used to fetch available aircraft models for user selection                                        |
| `AddAircraftController`         | Application Controller  | Orchestrates the use case, validates capacity rules, enforces ATCC role                           |
| `AddAircraftUI`                 | UI                      | Collects registration, country, and cabin seat distribution from the operator                     |

The following domain model excerpt shows the aggregate structure:

![Domain Model](svg/US070-domain-model.svg)

---

## 4. Design

### 4.1. Realization

1. The UI (`AddAircraftUI`) prompts the ATCC for the aircraft's registration number, country, model selection, and seat configuration (first, business, economy).
2. Input is validated inline before calling the controller (non-blank strings, positive seat numbers).
3. The controller calls `authz.ensureAuthenticatedUserHasAnyOf(ATCC)`.
4. The controller fetches the authenticated user's `AirTransportCompany` (via `AirTransportCompanyRepository`) and the selected `AircraftModel`.
5. The controller instantiates `CabinConfiguration` and validates that its total seats do not exceed the `AircraftModel`'s maximum capacity (Domain Rule).
6. The controller instantiates `Aircraft` with the validated data — domain invariants are enforced inside the constructor.
7. The aircraft is persisted via `AircraftRepository.save()`.
8. The new aircraft's identity (registration) is added to the company's fleet, and the company is updated via `AirTransportCompanyRepository.save()` (triggering Optimistic Locking via `@Version`).
9. The UI confirms success: `Aircraft '...' successfully added to the fleet.`

The following sequence diagram illustrates the flow:

![Sequence Diagram](svg/US070-SD.svg)

The following class diagram shows the classes involved:

![Class Diagram](svg/US070-class-diagram.svg)

---

### 4.2 Acceptance Tests

Detailed coverage is documented in [tests.md](testsUS070.md).

## 5. Implementation

### Key Implementation Details

- `Aircraft` → Annotated with `@Entity` and implements `AggregateRoot<String>`.
- `registrationNumber` → Annotated with `@Id` and normalized before persistence; the controller/repository rejects duplicates before save.
- `CabinConfiguration` → Implements `ValueObject` and uses `@Embeddable` to encapsulate seat distribution rules.
- `CabinConfiguration` is mapped inside `Aircraft` using the `@Embedded` annotation.
- `AirTransportCompany` → Uses `@ElementCollection` to store a list/set of Aircraft Registration IDs (Strings) to maintain Low Coupling, and uses the `@Version` annotation to enforce Optimistic Locking when adding a new aircraft to the fleet.
- Repository implementations were provided for all required aggregates:
    - `InMemoryAircraftRepository` / `JpaAircraftRepository`
    - `InMemoryAirTransportCompanyRepository` / `JpaAirTransportCompanyRepository`

Bootstrap support is implemented in `AiSafeBootstrap`, which seeds a default valid Air Transport Company and an active Aircraft in its fleet if they do not already exist.

## 6. Integration / Demonstration

### Run Instructions

```bash
# Run bootstrap (creates initial data, including Models and Companies)
./run-bootstrap.sh

# Run backoffice / user application
./run-backoffice.sh

# Login with:
# Username: atcc (or a specific Air Transport Company Collaborator account)
# Password: Password1

# Navigate to: 
# Fleet Management -> Add Aircraft
```

## 7. Observations

The `CabinConfiguration` acts as an autonomous validator. By delegating the seat counting logic to this Value Object, the `Aircraft` aggregate root remains clean and highly cohesive.

The capacity business rule (**US070.2**) is robustly enforced during the `Aircraft` instantiation by passing the `AircraftModel` to the constructor, which immediately checks if:

```text
cabin.totalSeats() <= model.maxCapacity()
