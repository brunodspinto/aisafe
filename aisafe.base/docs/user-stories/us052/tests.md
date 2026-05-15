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

---

### 4.2. Acceptance Tests

Authorization (Backoffice Operator role), code uniqueness constraints (IATA, ICAO), and the area association are infrastructure concerns primarily validated by manual integration testing. Code format and coordinate validations are fully covered by the automated unit tests above.

**Manual test — AC052.1 (airport associated with area):**

1. Run `AiSafeBackofficeApp` and login as a Backoffice Operator.
2. Navigate to `Airports > Register Airport`.
3. Select an existing Air Control Area (e.g. `EUR`).
4. Provide: IATA `LIS`, ICAO `LPPT`, name `Humberto Delgado`, town `Lisbon`, country `Portugal`, latitude `38.78`, longitude `-9.14`.
5. Expected: confirmation message displayed and airport visible in the list.

**Manual test — AC052.2 / AC052.3 (duplicate code rejected):**

1. Attempt to register a second airport with IATA `LIS` or ICAO `LPPT`.
2. Expected: the system rejects the operation with a uniqueness violation message.

**Manual test — AC052.2 (invalid IATA code format rejected):**

1. At the IATA prompt enter `li` (2 letters) or `LISS` (4 letters).
2. Expected: the system rejects the input with a format validation error and re-prompts.
