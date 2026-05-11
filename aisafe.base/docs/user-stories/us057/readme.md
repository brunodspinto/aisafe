### US 57 - Add an Engine Model to an Aircraft Model

## 1. Context
This US is implemented in Sprint 2 and allows the Backoffice Operator to add an engine model to an aircraft model’s list of certified engines in the AISafe system. It depends on US030 (Authentication and Authorization), which must be in place so that only an authenticated Backoffice Operator can invoke this feature.

The AircraftModel is an independent aggregate that is updated in this operation. This use case depends on the prior existence of aircraft models and engine models (US055 and US056) and acts as a foundational dependency for US070 (Add an aircraft), which requires the aircraft's theoretical model to have certified engines associated with it before a physical aircraft can be instantiated.


--------------------------------------------------------------------------------

## 2. Requirements
**US057** As a Backoffice Operator, I want to add an engine model to an aircraft model’s list of certified engines.

##### Acceptance Criteria
*   **US057.1:** The engine type must be compatible with the aircraft model.
*   **US057.2:** The same engine model cannot be added twice to the same aircraft model.
*   **US057.3:** The system must handle concurrent updates to the same aircraft model (Optimistic Locking) and inform the user if the data was changed by someone else in the meantime [1, 2].
*   **US057.4:** The user performing this action must be authenticated and have the `BACKOFFICE_OPERATOR` role.

##### Dependencies / References
*   Requires **US055** (Create an Aircraft Model) as the base entity must already exist in the system [3, 4].
*   Requires **US056** (Create an Aircraft Engine Model) as the catalog of engines must be populated [4].
*   Requires **US030 / US031** (Authentication and Authorization) to validate the Backoffice Operator role.

--------------------------------------------------------------------------------

## 3. Analysis
Since this use case modifies an existing aggregate rather than creating a new one, the architectural focus shifts to **Data Consistency and Concurrency Control**.

### Main Components
*   **AircraftModel** — Aggregate root representing the aircraft specification.
*   **EngineModelCode** (or `EngineModelId`) — Value object acting as an external reference to the `EngineModel` aggregate.

**Architectural Decision (Low Coupling):** To maintain the boundaries of Domain-Driven Design, the `AircraftModel` will not hold a direct JPA `@ManyToMany` collection of `EngineModel` entities. Instead, it will hold a collection of primitive identifiers (e.g., Strings or specific Value Objects) referencing the certified engines.

**Architectural Decision (Concurrency Control):** Because multiple Backoffice Operators might try to update the same `AircraftModel` simultaneously, we must implement **Optimistic Locking** [1, 5].
*   The `AircraftModel` entity will be updated with a `version` attribute annotated with JPA's `@Version` [1].
*   When a concurrent update conflict occurs, JPA will throw an `OptimisticLockException`, which the repository layer must catch and wrap into a `ConcurrencyException` to be presented to the user [2, 6].

### Domain Model

![Domain Model](svg/US057-domain-model.svg)

## 4. Design

### 4.1 Realization

This use case follows the standard EAPLI Application Engineering Process. The Controller orchestrates the reading of both catalogs (`AircraftModel` and `EngineModel`), allows the user to select them, delegates the business logic (compatibility and duplication checks) to the `AircraftModel` aggregate, and finally saves the updated entity.

---

## Sequence Diagram

![Sequence Diagram](svg/US057-SD.svg)
---

## Class Diagram

![Class Diagram](svg/US057-class-diagram.svg)

---

### 4.2 Acceptance Tests

### Test 1

Verifies that the same engine model cannot be added twice to the aircraft model.

```java
@Test(expected = IllegalArgumentException.class)
public void ensureCannotAddDuplicateEngineModel() {
    // Arrange
    AircraftModel model = new AircraftModel(...);
    EngineModel engine = new EngineModel(...);
    
    // Act
    model.addCertifiedEngine(engine);
    model.addCertifiedEngine(engine); // Should throw exception
}
```

---

# 5. Implementation

## Key Implementation Details

- `AircraftModel` → Added a new attribute `private Long version;` annotated with `@Version` to enforce Optimistic Locking via JPA.

- `certifiedEngines` → A `Set<String>` (or specific ID Value Object) is used with `@ElementCollection` to store the references to the `EngineModel` aggregate, avoiding the overhead of fetching entire engine entities when loading the aircraft model.

- `JpaAircraftModelRepository` → The `save()` method is prepared to catch the `OptimisticLockException` thrown by the JPA provider and wrap it into a `ConcurrencyException`, which is then gracefully handled by the UI layer to inform the user.

- The `AuthorizationService` ensures that only users with the `BACKOFFICE_OPERATOR` role can execute this action.

---

# 6. Integration / Demonstration

## Run Instructions

To test this functionality, ensure the system has been bootstrapped first so that Aircraft Models and Engine Models exist in the database.

```bash
# Run bootstrap (creates initial data, including Aircraft and Engine Models)
./run-bootstrap.sh

# Run backoffice
./run-backoffice.sh

# Login with:
# Username: backoffice_operator (or the specific username created in your bootstrap)
# Password: Password1

# Navigate to: 
# Aircraft Configuration -> Add Engine to Aircraft Model
```

---

# 7. Observations

The logic to validate engine compatibility and uniqueness is placed entirely inside the `AircraftModel` domain class (`addCertifiedEngine(EngineModel engine)`). This ensures the Aggregate Root maintains its own invariants.
