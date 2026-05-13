# US057 — Tests and Coverage

## Scope

US057 covers the addition of an `EngineModel` reference to an existing `AircraftModel`'s list of certified engines. The implementation must ensure that the aggregates remain uncoupled, concurrency is safely managed, and strict business invariants are preserved.

The test suite enforces:
- Domain rules regarding engine duplication and type compatibility.
- Safe concurrent updates to the same `AircraftModel` using JPA Optimistic Locking (`@Version`).
- Proper authorization ensuring only Backoffice Operators can perform the action.

---

## 1. Automated Tests

### 1.1. Domain Tests: AircraftModelTest
**Location:** `src/test/java/aisafe/aircraftmodel/domain/AircraftModelTest.java`

The following tests validate the core domain invariants inside the `AircraftModel` aggregate root when handling engine additions. *(Note: Creation and basic validation tests for the aggregates belong to US055 and US056).*

* `ensureCanAddCompatibleEngineType()`
    * **Description:** Verifies the "happy path" where a compatible and new engine model is added to the aircraft's certified list, increasing its size.
* `ensureCannotAddNullEngineModel()`
    * **Description:** Ensures robustness by preventing the addition of a null engine reference.
* `ensureCannotAddIncompatibleEngineType()`
    * **Description:** Validates **US057.1**. Ensures that incompatible engine types (e.g., trying to add a `TURBOPROP` to an aircraft model initialized with a `TURBOFAN`) are rejected by the aggregate.
* `ensureCannotAddDuplicateEngineModel()`
    * **Description:** Validates **US057.2**. Ensures that adding an engine whose identity is already in the `certifiedEngines` collection throws an `IllegalArgumentException`.

### 1.2. Application Tests: AddEngineToAircraftModelControllerTest
**Location:** `src/test/java/aisafe/aircraftmodel/application/AddEngineToAircraftModelControllerTest.java`

These tests mock the repositories and authorization service to ensure the controller orchestrates the flow correctly.

* `ensureOnlyBackofficeOperatorCanAddEngine()`
    * **Description:** Validates **US057.4**. Mocks the `AuthorizationService` to simulate a user with a non-authorized role and ensures the controller throws an `UnauthorizedException`.

### 1.3. Persistence Tests: JpaAircraftModelRepositoryTest (Integration)
**Location:** `src/test/java/aisafe/infrastructure/persistence/jpa/JpaAircraftModelRepositoryTest.java`

* `ensureOptimisticLockingThrowsExceptionOnConcurrentUpdate()`
    * **Description:** Validates **US057.3**. Simulates a scenario where two parallel transactions read the same `AircraftModel`, both add a different engine, and attempt to save. Asserts that the second `save()` throws an `OptimisticLockException` (or a wrapped `ConcurrencyException`), proving that the `@Version` annotation is functioning correctly.

---

## 2. Coverage by Acceptance Criterion

* **US057.1 (Compatibility):** Covered by `ensureCannotAddIncompatibleEngineType()` in the Domain layer. The `AircraftModel` aggregate holds the business rules to reject incompatible engine profiles.
* **US057.2 (No Duplicates):** Covered by `ensureCannotAddDuplicateEngineModel()`. The domain logic explicitly checks for existence before adding.
* **US057.3 (Concurrency / Optimistic Locking):** Covered mechanically via the `@Version` annotation in the `AircraftModel` entity, and tested via the integration test `ensureOptimisticLockingThrowsExceptionOnConcurrentUpdate()`.
* **US057.4 (Authorization):** Covered by `ensureOnlyBackofficeOperatorCanAddEngine()` in the controller tests, leveraging the framework's `AuthzRegistry`.

---

## 3. Manual / Integration Coverage

To fully validate the End-to-End flow, the following manual test script must be executed using the Console Application:

### Scenario A: Happy Path & Duplication Prevention
1. Login as `backoffice_operator`.
2. Navigate to `Aircraft Configuration > Add Engine to Aircraft Model`.
3. Select an existing Aircraft Model (e.g., `Boeing 737`).
4. Select an Engine Model (e.g., `CFM56`).
5. **Observe:** Success message indicating the engine was certified.
6. Repeat steps 2 through 4 with the **exact same** Aircraft and Engine models.
7. **Observe:** The system rejects the operation with a business rule violation message.

### Scenario B: Concurrency Control (Optimistic Locking)
1. Open **two separate terminal windows** and run the Backoffice Application in both.
2. Login as `backoffice_operator` in both terminals.
3. In **Terminal 1**, navigate to `Add Engine to Aircraft Model`, select an Aircraft Model, but **do not** select the engine yet.
4. In **Terminal 2**, navigate to the same menu, select the **same** Aircraft Model, select an engine, and confirm the addition. (Operation succeeds).
5. Return to **Terminal 1**, select an engine, and try to confirm.
6. **Observe:** The system detects the version mismatch and aborts the operation, displaying a user-friendly concurrency error.