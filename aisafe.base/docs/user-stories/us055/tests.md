# US055 — Tests and Coverage

## Scope

US055 covers registering an Aircraft Model with a name, manufacturer (Maker), type, flight characteristics, and at least one certified engine model.

## Automated Tests

### `AircraftModelTest`

Location: `src/test/java/aisafe/aircraftmodel/domain/AircraftModelTest.java`

- `ensureValidAircraftModelCanBeCreated`
- `ensureModelNameCannotBeNull`
- `ensureMakerCannotBeNull`
- `ensureAircraftTypeCannotBeNull`
- `ensureFirstEngineCannotBeNull`
- `ensureEmptyWeightMustBePositive`
- `ensureMTOWMustBeGreaterThanEmptyWeight`
- `ensureCannotAddNullEngineModel`

### `MakerTest`

Location: `src/test/java/aisafe/maker/domain/MakerTest.java`

- `ensureValidMakerCanBeCreated`
- `ensureNameCannotBeNull`
- `ensureNameCannotBeBlank`
- `ensureCountryCannotBeNull`
- `ensureCountryCannotBeBlank`
- `ensureTwoMakersWithSameNameAreEqual`
- `ensureTwoMakersWithDifferentNamesAreNotEqual`

## Coverage by Acceptance Criterion

- AC055.1: `ensureModelNameCannotBeNull`, `ensureMakerCannotBeNull`; name+maker uniqueness enforced by `@UniqueConstraint`
- AC055.2: `ensureFirstEngineCannotBeNull`, `ensureCannotAddNullEngineModel`
- AC055.3: `ensureAircraftTypeCannotBeNull`
- AC055.4: `ensureEmptyWeightMustBePositive`, `ensureMTOWMustBeGreaterThanEmptyWeight`
- AC055.5: Covered by `AiSafeBootstrap` which registers the 737-800 model idempotently
