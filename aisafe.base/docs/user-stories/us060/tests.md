# US060 — Tests and Coverage

## Scope

US060 covers registering an Air Transport Company with a unique IATA code, ICAO code, and company name.

## Automated Tests

### `AirTransportCompanyTest`

Location: `src/test/java/aisafe/airtransportcompany/domain/AirTransportCompanyTest.java`

- `ensureValidIATACodeIsAccepted`
- `ensureIATACodeRejectsOneChar`
- `ensureIATACodeRejectsThreeChars`
- `ensureIATACodeRejectsDigits`
- `ensureIATACodeRejectsLowercase`
- `ensureICAOCodeAcceptsTwoChars`
- `ensureICAOCodeAcceptsThreeChars`
- `ensureICAOCodeRejectsOneChar`
- `ensureICAOCodeRejectsFourChars`
- `ensureCompanyNameCannotBeNull`
- `ensureCompanyNameCannotBeBlank`
- `ensureCompanyIdentityIsIATACode`
- `ensureCompaniesWithSameIATACodeAreEqual`
- `ensureAircraftCanBeAddedToCompanyFleet`
- `ensureDuplicateAircraftRegistrationCannotBeAddedToFleet`

## Coverage by Acceptance Criterion

- AC060.1: `ensureCompanyNameCannotBeNull`, `ensureCompanyNameCannotBeBlank`; uniqueness enforced at persistence level
- AC060.2: `ensureValidIATACodeIsAccepted`, `ensureIATACodeRejectsOneChar`, `ensureIATACodeRejectsThreeChars`, `ensureIATACodeRejectsDigits`, `ensureIATACodeRejectsLowercase`
- AC060.3: `ensureICAOCodeAcceptsTwoChars`, `ensureICAOCodeAcceptsThreeChars`, `ensureICAOCodeRejectsOneChar`, `ensureICAOCodeRejectsFourChars`
- AC060.4: `ensureCompanyIdentityIsIATACode`, `ensureCompaniesWithSameIATACodeAreEqual`; uniqueness enforced by `@EmbeddedId`
- AC060.5: Uniqueness enforced by `@Column(unique = true)` on ICAO code
- AC060.6: Controller checks `BACKOFFICE_OPERATOR` role via `AuthorizationService`
- AC060.7: Covered by `AiSafeBootstrap` which registers TP, FR and LH idempotently
