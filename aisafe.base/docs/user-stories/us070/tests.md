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
