# US050 — Tests and Coverage

## Scope

US050 covers registering an air control area from the backoffice UI and through bootstrap. The current implementation enforces:

- Unique air control area codes through the controller and repository identity.
- Valid geographic boundaries through the `GeoBoundary` value object.
- Bootstrap seeding of at least one valid area during startup.

---

## Automated Tests

### GeoBoundaryTest

**Location:**  
`src/test/java/aisafe/aircontrolarea/domain/GeoBoundaryTest.java`

**Coverage:**

- `ensureValidGeoBoundaryIsCreatedSuccessfully`
- `ensureNorthLatitudeMustBeStrictlyGreaterThanSouthLatitude`
- `ensureNorthLatitudeCannotBeEqualToSouthLatitude`
- `ensureNorthLatitudeCannotBeGreaterThan90`
- `ensureSouthLatitudeCannotBeLessThanMinus90`
- `ensureEastLongitudeCannotBeInvalid`
- `ensureWestLongitudeCannotBeInvalid`

---

### AirControlAreaTest

**Location:**  
`src/test/java/aisafe/aircontrolarea/domain/AirControlAreaTest.java`

**Coverage:**

- `ensureValidAirControlAreaIsCreatedSuccessfully`
- `ensureAirControlAreaMustHaveValidCode`
- `ensureAirControlAreaCannotHaveEmptyCode`
- `ensureAirControlAreaMustHaveValidName`
- `ensureAirControlAreaCannotHaveNegativeFuel`
- `ensureAirControlAreaMustHaveBoundaries`

---

## Coverage by Acceptance Criterion

- **US050.1:** Covered by `ensureValidAirControlAreaIsCreatedSuccessfully`.
- **US050.2:** Enforced in `RegisterAirControlAreaController` and Repository by the JPA `@Id` constraint and identity lookup before save.
- **US050.3:** Covered comprehensively by the `GeoBoundaryTest` suite.
- **US050.4:** `AiSafeBootstrap.bootstrapAirControlAreas()` seeds a valid default area on startup.

---

## Manual / Integration Coverage

The remaining use-case behavior is validated through the console application:

1. Login as a Backoffice Operator or Admin.
2. Open the Air Control Area registration menu:
    - `Air Control > Register Air Control Area`
3. Submit valid coordinates and a unique code.
4. Observe the success message and verify the persisted record.

---

## Bootstrap Coverage

- Run the bootstrap application.
- Confirm that the default air control area is created when missing.
- Confirm that the bootstrap process does **not duplicate** the same area code on subsequent runs.

---

## Notes

- No new domain concepts were introduced beyond what was planned in the analysis phase.
- Bootstrap seeding is intentionally simple and uses a valid sample area code (e.g., `PT-N`) with valid geographic coordinates.