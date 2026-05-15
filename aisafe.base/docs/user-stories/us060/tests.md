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

---

### 4.2. Acceptance Tests

Authorization (Backoffice Operator role) and persistence-level uniqueness (IATA, ICAO, name) are infrastructure concerns primarily validated by manual integration testing. Code format validations are fully covered by the automated unit tests above.

**Manual test — AC060.1 / AC060.2 / AC060.3 (full registration flow):**

1. Run `AiSafeBackofficeApp` and login as a Backoffice Operator.
2. Navigate to `Air Transport Companies > Register Company`.
3. Provide: name `TAP Air Portugal`, IATA code `TP`, ICAO code `TAP`.
4. Expected: confirmation message displayed and company visible in the list.

**Manual test — AC060.2 / AC060.4 (duplicate IATA code rejected):**

1. Attempt to register a second company with IATA code `TP`.
2. Expected: the system rejects the operation with a uniqueness violation message.

**Manual test — AC060.2 (invalid IATA code format rejected):**

1. At the IATA prompt enter `TPX` (3 letters) or `tp` (lowercase).
2. Expected: the system rejects the input with a format validation error and re-prompts for a valid 2-letter uppercase code.

**Manual test — AC060.6 (role enforcement):**

1. Login as a user without the Backoffice Operator role (e.g. ATCC).
2. Expected: the Register Company option is not available in the menu.
