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

---

### 4.2. Acceptance Tests

Authorization (Backoffice Operator role) and persistence-level uniqueness (name + maker) are infrastructure concerns primarily validated by manual integration testing. Domain validations (weights, engine list, type) are fully covered by the automated unit tests above.

**Manual test — AC055.1 / AC055.2 / AC055.3 / AC055.4 (full registration flow):**

1. Run `AiSafeBackofficeApp` and login as a Backoffice Operator.
2. Navigate to `Aircraft Models > Register Aircraft Model`.
3. Provide: name `737-800`, maker `Boeing`, type `PASSENGER`, empty weight `41000`, MTOW `79015`, MZFW `62731`, max fuel `26020`, ceiling `41000`, cruise speed `842`, wing span `35.8`, wing area `125.0`, drag coefficient `0.025`, lift coefficient `1.8`.
4. Select at least one existing engine model (e.g. `CFM56`).
5. Expected: confirmation message displayed.

**Manual test — AC055.2 (no engine model rejected):**

1. Attempt to complete the registration without selecting any engine model.
2. Expected: the system rejects the operation with a message indicating at least one certified engine is required.

**Manual test — AC055.4 (MTOW less than empty weight rejected):**

1. Provide empty weight `80000` and MTOW `70000`.
2. Expected: the system rejects the operation with a weight validation error message.

**Manual test — AC055.1 (duplicate name+maker rejected):**

1. Attempt to register a second model with the same name and maker (e.g. `737-800` / `Boeing`).
2. Expected: the system rejects the operation with a uniqueness violation message.
