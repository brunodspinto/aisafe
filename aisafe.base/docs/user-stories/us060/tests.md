# US060 — Tests and Coverage

## Scope

US060 covers registering an Air Transport Company with a unique IATA code, ICAO code, and company name.

## Automated Tests

### `AirTransportCompanyTest`

Location: `src/test/java/aisafe/airtransportcompany/domain/AirTransportCompanyTest.java`

**Test:** `ensureIATACodeRejectsOneChar`

```java
@Test
void ensureIATACodeRejectsOneChar() {
    assertThrows(IllegalArgumentException.class, () -> IATACode.valueOf("T"));
}
```

**Test:** `ensureIATACodeRejectsThreeChars`

```java
@Test
void ensureIATACodeRejectsThreeChars() {
    assertThrows(IllegalArgumentException.class, () -> IATACode.valueOf("TAP"));
}
```

**Test:** `ensureIATACodeRejectsDigits`

```java
@Test
void ensureIATACodeRejectsDigits() {
    assertThrows(IllegalArgumentException.class, () -> IATACode.valueOf("T1"));
}
```

**Test:** `ensureIATACodeRejectsLowercase`

```java
@Test
void ensureIATACodeRejectsLowercase() {
    assertThrows(IllegalArgumentException.class, () -> IATACode.valueOf("tp"));
}
```

**Test:** `ensureValidIATACodeIsAccepted`

```java
@Test
void ensureValidIATACodeIsAccepted() {
    final IATACode code = IATACode.valueOf("TP");
    assertEquals("TP", code.toString());
}
```

**Test:** `ensureICAOCodeRejectsOneChar`

```java
@Test
void ensureICAOCodeRejectsOneChar() {
    assertThrows(IllegalArgumentException.class, () -> ICAOCode.valueOf("T"));
}
```

**Test:** `ensureICAOCodeRejectsFourChars`

```java
@Test
void ensureICAOCodeRejectsFourChars() {
    assertThrows(IllegalArgumentException.class, () -> ICAOCode.valueOf("TAPT"));
}
```

**Test:** `ensureICAOCodeAcceptsTwoChars`

```java
@Test
void ensureICAOCodeAcceptsTwoChars() {
    final ICAOCode code = ICAOCode.valueOf("TP");
    assertEquals("TP", code.toString());
}
```

**Test:** `ensureICAOCodeAcceptsThreeChars`

```java
@Test
void ensureICAOCodeAcceptsThreeChars() {
    final ICAOCode code = ICAOCode.valueOf("TAP");
    assertEquals("TAP", code.toString());
}
```

**Test:** `ensureCompanyNameCannotBeNull`

```java
@Test
void ensureCompanyNameCannotBeNull() {
    assertThrows(IllegalArgumentException.class,
            () -> new AirTransportCompany(null, IATACode.valueOf("TP"), ICAOCode.valueOf("TAP")));
}
```

**Test:** `ensureCompanyNameCannotBeBlank`

```java
@Test
void ensureCompanyNameCannotBeBlank() {
    assertThrows(IllegalArgumentException.class,
            () -> new AirTransportCompany("   ", IATACode.valueOf("TP"), ICAOCode.valueOf("TAP")));
}
```

**Test:** `ensureCompanyIdentityIsIATACode`

```java
@Test
void ensureCompanyIdentityIsIATACode() {
    final AirTransportCompany company =
            new AirTransportCompany("TAP Air Portugal", IATACode.valueOf("TP"), ICAOCode.valueOf("TAP"));
    assertEquals(IATACode.valueOf("TP"), company.identity());
}
```

**Test:** `ensureCompaniesWithSameIATACodeAreEqual`

```java
@Test
void ensureCompaniesWithSameIATACodeAreEqual() {
    final AirTransportCompany a =
            new AirTransportCompany("TAP Air Portugal", IATACode.valueOf("TP"), ICAOCode.valueOf("TAP"));
    final AirTransportCompany b =
            new AirTransportCompany("TAP", IATACode.valueOf("TP"), ICAOCode.valueOf("TP"));
    assertEquals(a, b);
}
```

**Test:** `ensureAircraftCanBeAddedToCompanyFleet`

```java
@Test
void ensureAircraftCanBeAddedToCompanyFleet() {
    final AirTransportCompany company =
            new AirTransportCompany("TAP Air Portugal", IATACode.valueOf("TP"), ICAOCode.valueOf("TAP"));
    final Aircraft aircraft = validAircraft("CS-TUA");
    company.addAircraftToFleet(aircraft);
    assertTrue(company.fleet().contains("CS-TUA"));
    assertEquals(1, company.fleet().size());
}
```

**Test:** `ensureDuplicateAircraftRegistrationCannotBeAddedToFleet`

```java
@Test
void ensureDuplicateAircraftRegistrationCannotBeAddedToFleet() {
    final AirTransportCompany company =
            new AirTransportCompany("TAP Air Portugal", IATACode.valueOf("TP"), ICAOCode.valueOf("TAP"));
    final Aircraft aircraft = validAircraft("CS-TUA");
    company.addAircraftToFleet(aircraft);
    company.addAircraftToFleet(aircraft);
    assertEquals(1, company.fleet().size());
}
```

---

## Coverage by Acceptance Criterion

- AC060.1: `ensureCompanyNameCannotBeNull`, `ensureCompanyNameCannotBeBlank`; uniqueness enforced at persistence level
- AC060.2: `ensureValidIATACodeIsAccepted`, `ensureIATACodeRejectsOneChar`, `ensureIATACodeRejectsThreeChars`, `ensureIATACodeRejectsDigits`, `ensureIATACodeRejectsLowercase`
- AC060.3: `ensureICAOCodeAcceptsTwoChars`, `ensureICAOCodeAcceptsThreeChars`, `ensureICAOCodeRejectsOneChar`, `ensureICAOCodeRejectsFourChars`
- AC060.4: `ensureCompanyIdentityIsIATACode`, `ensureCompaniesWithSameIATACodeAreEqual`; uniqueness enforced by `@EmbeddedId`
- AC060.5: Uniqueness enforced by `@Column(unique = true)` on ICAO code; validated by manual test
- AC060.6: Controller checks `BACKOFFICE_OPERATOR` role via `AuthorizationService`; validated by manual test
- AC060.7: Covered by `AiSafeBootstrap` which registers TP, FR and LH idempotently
- Fleet management (supports US070): `ensureAircraftCanBeAddedToCompanyFleet`, `ensureDuplicateAircraftRegistrationCannotBeAddedToFleet`

---

## Acceptance Tests

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
