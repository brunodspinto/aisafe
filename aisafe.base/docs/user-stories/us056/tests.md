# US056 — Tests and Coverage

## Scope

US056 covers creating an Aircraft Engine Model with a unique name+manufacturer combination, type, thrust and TSFC values.

## Automated Tests

### `EngineModelTest`

Location: `src/test/java/aisafe/enginemodel/domain/EngineModelTest.java`

- `ensureValidEngineModelCanBeCreated`
- `ensureNameCannotBeNull`
- `ensureNameCannotBeBlank`
- `ensureMakerNameCannotBeNull`
- `ensureMakerNameCannotBeBlank`
- `ensureEngineTypeCannotBeNull`
- `ensureThrustAtStandstillMustBePositive`
- `ensureThrustAtStandstillCannotBeNegative`
- `ensureThrustAtCruiseSpeedMustBePositive`
- `ensureThrustAtCruiseSpeedCannotBeNegative`
- `ensureTsfcMustBePositive`
- `ensureTsfcCannotBeNegative`
- `ensureNameIsTrimmedOnConstruction`
- `ensureMakerNameIsTrimmedOnConstruction`
- `ensureEqualsReturnsTrueForSameInstance`
- `ensureEqualsReturnsFalseForNull`
- `ensureSameAsReturnsTrueForSameInstance`
- `ensureHashCodeIsConsistent`

## Coverage by Acceptance Criterion

- AC056.1: Name+maker uniqueness enforced by `@UniqueConstraint(columnNames = {"name", "makerName"})`
- AC056.2: `ensureEngineTypeCannotBeNull`
- AC056.3: `ensureThrustAtStandstillMustBePositive`, `ensureThrustAtStandstillCannotBeNegative`, `ensureThrustAtCruiseSpeedMustBePositive`, `ensureThrustAtCruiseSpeedCannotBeNegative`
- AC056.4: `ensureTsfcMustBePositive`, `ensureTsfcCannotBeNegative`
- AC056.5: Controller checks `BACKOFFICE_OPERATOR` role via `AuthorizationService`
- AC056.6: Covered by `AiSafeBootstrap` which registers CFM56, PW4000 and PT6A-65B idempotently

---

### 4.2. Acceptance Tests

Authorization (Backoffice Operator role) and persistence-level uniqueness are infrastructure concerns primarily validated by manual integration testing. Domain validations (thrust, TSFC, engine type) are fully covered by the automated unit tests above.

**Manual test — AC056.1 / AC056.2 / AC056.3 / AC056.4 (full registration flow):**

1. Run `AiSafeBackofficeApp` and login as a Backoffice Operator.
2. Navigate to `Engine Models > Register Engine Model`.
3. Provide: name `CFM56`, manufacturer `CFM International`, type `TURBOFAN`, thrust at standstill `130`, thrust at cruise `27`, TSFC `0.545`.
4. Expected: confirmation message displayed.

**Manual test — AC056.1 (duplicate name+manufacturer rejected):**

1. Attempt to register another engine with name `CFM56` and manufacturer `CFM International`.
2. Expected: the system rejects the operation with a uniqueness violation message.

**Manual test — AC056.5 (role enforcement):**

1. Login as a user without the Backoffice Operator role (e.g. ATCC).
2. Expected: the Register Engine Model option is not available in the menu.
