# US050 — Tests and Coverage

## Scope

US050 covers registering an Air Control Area with a unique area code, name, minimum fuel requirement, and valid geographic boundaries.

## Automated Tests

### `AirControlAreaTest`

Location: `src/test/java/aisafe/aircontrolarea/domain/AirControlAreaTest.java`

- `ensureValidAirControlAreaCanBeCreated`
- `ensureAreaCodeCannotBeNullOrBlank`
- `ensureNameCannotBeNullOrBlank`
- `ensureMinimumFuelCannotBeNegative`
- `ensureBoundariesCannotBeNull`

### `GeoBoundaryTest`

Location: `src/test/java/aisafe/aircontrolarea/domain/GeoBoundaryTest.java`

- `ensureValidCoordinatesCreateBoundary`
- `ensureNorthLatitudeMustBeGreaterThanSouthLatitude`
- `ensureCoordinatesMustBeWithinLatitudeLimits`
- `ensureCoordinatesMustBeWithinLongitudeLimits`

## Coverage by Acceptance Criterion

- AC050.1: `ensureValidAirControlAreaCanBeCreated`, `ensureAreaCodeCannotBeNullOrBlank`, `ensureNameCannotBeNullOrBlank`, `ensureMinimumFuelCannotBeNegative`, `ensureBoundariesCannotBeNull`
- AC050.2: Enforced by `@EmbeddedId` on `AirControlAreaCode` and `@UniqueConstraint` at persistence level
- AC050.3: `ensureNorthLatitudeMustBeGreaterThanSouthLatitude`, `ensureCoordinatesMustBeWithinLatitudeLimits`, `ensureCoordinatesMustBeWithinLongitudeLimits`
- AC050.4: Covered by `AiSafeBootstrap` which registers the default area idempotently
