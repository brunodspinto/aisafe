# US052 — Tests and Coverage

## Scope

US052 covers registering an Airport with a unique IATA code, ICAO code, name, location coordinates, and association to an Air Control Area.

## Automated Tests

### `AirportTest`

Location: `src/test/java/aisafe/airport/domain/AirportTest.java`

- `ensureValidAirportCanBeCreated`
- `ensureIATACodeCannotBeNull`
- `ensureICAOCodeCannotBeNull`
- `ensureNameCannotBeNull`
- `ensureNameCannotBeBlank`
- `ensureTownCannotBeNull`
- `ensureCountryCannotBeNull`
- `ensureLocationCannotBeNull`

### `AirportIATACodeTest`

Location: `src/test/java/aisafe/airport/domain/AirportIATACodeTest.java`

- `ensureValidIATACodeIsAccepted`
- `ensureIATACodeCannotBeNull`
- `ensureIATACodeCannotBeBlank`
- `ensureIATACodeMustBeExactly3Letters`
- `ensureIATACodeMustBeUppercase`

### `AirportICAOCodeTest`

Location: `src/test/java/aisafe/airport/domain/AirportICAOCodeTest.java`

- `ensureValidICAOCodeIsAccepted`
- `ensureICAOCodeCannotBeNull`
- `ensureICAOCodeCannotBeBlank`
- `ensureICAOCodeMustBeExactly4Letters`
- `ensureICAOCodeMustBeUppercase`

### `GeoCoordinateTest`

Location: `src/test/java/aisafe/airport/domain/GeoCoordinateTest.java`

- `ensureValidCoordinateCanBeCreated`
- `ensureLatitudeCannotBeAbove90`
- `ensureLatitudeCannotBeBelow90`
- `ensureLongitudeCannotBeAbove180`
- `ensureLongitudeCannotBeBelow180`

## Coverage by Acceptance Criterion

- AC052.1: `ensureValidAirportCanBeCreated` (area code required parameter)
- AC052.2: `ensureValidIATACodeIsAccepted`, `ensureIATACodeMustBeExactly3Letters`, `ensureIATACodeMustBeUppercase`; uniqueness enforced by `@UniqueConstraint`
- AC052.3: `ensureValidICAOCodeIsAccepted`, `ensureICAOCodeMustBeExactly4Letters`, `ensureICAOCodeMustBeUppercase`; uniqueness enforced by `@UniqueConstraint`
- AC052.4: `ensureValidCoordinateCanBeCreated`, `ensureLatitudeCannotBeAbove90`, `ensureLatitudeCannotBeBelow90`, `ensureLongitudeCannotBeAbove180`, `ensureLongitudeCannotBeBelow180`
- AC052.5: Covered by `AiSafeBootstrap` which registers LIS and OPO idempotently
