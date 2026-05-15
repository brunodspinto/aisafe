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

---

### 4.2. Acceptance Tests

Authorization (Backoffice Operator role) and persistence-level uniqueness are infrastructure concerns primarily validated by manual integration testing. The geographic boundary validations are fully covered by the automated unit tests above.

**Manual test — AC050.1 (full registration flow):**

1. Run `AiSafeBackofficeApp` and login as a Backoffice Operator.
2. Navigate to `Air Control Areas > Register Air Control Area`.
3. Provide: code `EUR`, name `European Area`, minimum fuel `500`, north latitude `71`, south latitude `35`, east longitude `40`, west longitude `-25`.
4. Expected: confirmation message displayed and area visible in the list.

**Manual test — AC050.2 (duplicate area code rejected):**

1. Attempt to register a second area with code `EUR`.
2. Expected: the system rejects the operation with a duplicate code error message.

**Manual test — AC050.3 (invalid boundaries rejected):**

1. Attempt to register an area providing south latitude `50` and north latitude `40` (south greater than north).
2. Expected: the system rejects the operation with a boundary validation error message.
