# US050 — Tests and Coverage

## Scope

US050 covers registering an air control area from the backoffice UI and through bootstrap. The current implementation enforces:

- unique air control area codes through the controller and repository identity
- valid geographic boundaries through the `GeoBoundary` value object
- bootstrap seeding of at least one valid area during startup

## Automated Tests

### `GeoBoundaryTest`

Location: `src/test/java/aisafe/aircontrolarea/domain/GeoBoundaryTest.java`

Coverage:

- `ensureValidCoordinatesCreateBoundary`
- `ensureNorthLatitudeMustBeGreaterThanSouthLatitude`
- `ensureCoordinatesMustBeWithinLatitudeLimits`
- `ensureCoordinatesMustBeWithinLongitudeLimits`

### `AirControlAreaTest`

Location: `src/test/java/aisafe/aircontrolarea/domain/AirControlAreaTest.java`

Coverage:

- `ensureValidAirControlAreaCanBeCreated`
- `ensureAreaCodeCannotBeNullOrBlank`
- `ensureNameCannotBeNullOrBlank`
- `ensureMinimumFuelCannotBeNegative`
- `ensureBoundariesCannotBeNull`

## Coverage by Acceptance Criterion

- US050.1: `ensureValidAirControlAreaCanBeCreated`
- US050.2: enforced in `RegisterAirControlAreaController` by repository identity lookup before save
- US050.3: `GeoBoundaryTest`
- US050.4: `AiSafeBootstrap.bootstrapAirControlAreas()` seeds a valid default area on startup

## Manual / Integration Coverage

The remaining use-case behavior is validated through the console application:

- login as a Backoffice Operator or Admin
- open the Air Control Area registration menu
- submit valid coordinates and a unique code
- observe success message and persisted record

Bootstrap coverage:

- run the bootstrap application
- confirm that the default air control area is created when missing
- confirm that bootstrap does not duplicate the same area code on subsequent runs

## Notes

- No new domain concepts were introduced, so the domain model does not need updates.
- Bootstrap seeding is intentionally simple and uses a valid sample area code (`PT-N`) with valid coordinates.