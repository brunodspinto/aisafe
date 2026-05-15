# US070 — Tests and Coverage

## Scope

US070 covers adding an aircraft to the fleet of an Air Transport Company, with a registration number, country, crew size, manufacture year, cabin configuration, and an associated aircraft model.

## Automated Tests

### `AircraftTest`

Location: `src/test/java/aisafe/aircraft/domain/AircraftTest.java`

- `ensureValidAircraftIsCreatedSuccessfully`
- `ensureAircraftIsCreatedWithActiveOperationalStatus`
- `ensureAircraftRegistrationIsNormalisedToUpperCase`
- `ensureAircraftMustHaveValidRegistrationNumber`
- `ensureAircraftRegistrationCannotBeEmpty`
- `ensureAircraftMustBeRegisteredToACountry`
- `ensureAircraftCannotExceedModelMaximumCapacity`
- `ensureAircraftWithExactModelCapacityIsAccepted`

### `CabinConfigurationTest`

Location: `src/test/java/aisafe/aircraft/domain/CabinConfigurationTest.java`

- `ensureValidCabinConfigurationIsCreatedSuccessfully`
- `ensureCabinWithZeroAllSeatsThrows`
- `ensureCabinConfigurationCannotHaveNegativeFirstClassSeats`
- `ensureCabinConfigurationCannotHaveNegativeBusinessClassSeats`
- `ensureCabinConfigurationCannotHaveNegativeEconomyClassSeats`
- `ensureTotalSeatsIsCalculatedCorrectly`

## Coverage by Acceptance Criterion

- AC070.1: `ensureValidCabinConfigurationIsCreatedSuccessfully`, `ensureCabinWithZeroAllSeatsThrows`, `ensureTotalSeatsIsCalculatedCorrectly`
- AC070.2: `ensureAircraftCannotExceedModelMaximumCapacity`, `ensureAircraftWithExactModelCapacityIsAccepted`
- AC070.3: `ensureAircraftMustHaveValidRegistrationNumber`, `ensureAircraftRegistrationCannotBeEmpty`, `ensureAircraftRegistrationIsNormalisedToUpperCase`; uniqueness enforced by `@EmbeddedId`
- AC070.4: `ensureAircraftMustBeRegisteredToACountry`
- AC070.5: `ensureAircraftIsCreatedWithActiveOperationalStatus`
- AC070.6: Controller checks `ATCC` role via `AuthorizationService`

---

### 4.2. Acceptance Tests

Authorization (ATCC role) and company-fleet association are infrastructure concerns validated by manual integration testing. Cabin configuration and registration number validations are fully covered by the automated unit tests above.

**Manual test — AC070.1 / AC070.3 / AC070.4 / AC070.5 (full registration flow):**

1. Run `AiSafeBackofficeApp` and login as an ATCC user.
2. Navigate to `Fleet > Add Aircraft`.
3. Select an aircraft model (e.g. `737-800` by `Boeing`).
4. Provide: registration `CS-TNA`, country `Portugal`, crew size `6`, manufacture year `2010`, first class seats `0`, business class seats `20`, economy class seats `150`.
5. Expected: confirmation message displayed; aircraft appears in the fleet list with operational status `ACTIVE`.

**Manual test — AC070.2 (cabin capacity exceeded rejected):**

1. Provide a seat configuration whose total (first + business + economy) exceeds the selected model's maximum passenger capacity.
2. Expected: the system rejects the operation with a capacity validation error.

**Manual test — AC070.3 (duplicate registration rejected):**

1. Attempt to add a second aircraft with registration `CS-TNA`.
2. Expected: the system rejects the operation with a uniqueness violation message.

**Manual test — AC070.6 (role enforcement):**

1. Login as a user without the ATCC role (e.g. Admin or Backoffice Operator).
2. Expected: the Add Aircraft option is not available in the menu.
