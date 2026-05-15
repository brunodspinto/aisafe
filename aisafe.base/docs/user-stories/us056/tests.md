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
