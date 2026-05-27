# US075 — Tests and Coverage

## Scope

US075 covers adding a pilot to the authenticated collaborator's air transport company. A pilot is a system user (with the `PILOT` role) certified for one or more existing aircraft models. Testing is split between the `Pilot` aggregate invariants (domain) and the use case orchestration (application controller).

## Automated Tests

### `PilotTest`

Location: `src/test/java/aisafe/pilot/domain/PilotTest.java`

> Note: tests use helper methods `validUser(String)` (builds a system user with the `PILOT` role wrapped in a `User`) and `validCompany()` (returns `IATACode.valueOf("TP")`).

**Test:** `ensureValidPilotCanBeCreated`

```java
@Test
void ensureValidPilotCanBeCreated() {
    final Pilot pilot = new Pilot(validUser("pilot1"), validCompany(), Set.of(1L, 2L));
    assertEquals(validCompany(), pilot.companyIataCode());
    assertEquals(2, pilot.certifiedAircraftModelIds().size());
    assertTrue(pilot.isActive());
}
```

**Test:** `ensurePilotIsActiveByDefault`

```java
@Test
void ensurePilotIsActiveByDefault() {
    final Pilot pilot = new Pilot(validUser("pilot-active"), validCompany(), Set.of(1L));
    assertTrue(pilot.isActive());
}
```

**Test:** `ensureUserCannotBeNull`

```java
@Test
void ensureUserCannotBeNull() {
    assertThrows(IllegalArgumentException.class,
            () -> new Pilot(null, validCompany(), Set.of(1L)));
}
```

**Test:** `ensureCompanyCannotBeNull`

```java
@Test
void ensureCompanyCannotBeNull() {
    assertThrows(IllegalArgumentException.class,
            () -> new Pilot(validUser("pilot2"), null, Set.of(1L)));
}
```

**Test:** `ensurePilotCertificationSetCannotBeNull`

```java
@Test
void ensurePilotCertificationSetCannotBeNull() {
    assertThrows(IllegalArgumentException.class,
            () -> new Pilot(validUser("pilot3"), validCompany(), null));
}
```

**Test:** `ensurePilotMustHaveAtLeastOneCertification`

```java
@Test
void ensurePilotMustHaveAtLeastOneCertification() {
    assertThrows(IllegalArgumentException.class,
            () -> new Pilot(validUser("pilot4"), validCompany(), Set.of()));
}
```

**Test:** `ensureCertificationsCannotContainNull`

```java
@Test
void ensureCertificationsCannotContainNull() {
    final Set<Long> withNull = new HashSet<>();
    withNull.add(null);
    assertThrows(IllegalArgumentException.class,
            () -> new Pilot(validUser("pilot5"), validCompany(), withNull));
}
```

**Test:** `ensureIsCertifiedForReturnsTrueForCertifiedModel`

```java
@Test
void ensureIsCertifiedForReturnsTrueForCertifiedModel() {
    final Pilot pilot = new Pilot(validUser("pilot6"), validCompany(), Set.of(1L));
    assertTrue(pilot.isCertifiedFor(1L));
    assertFalse(pilot.isCertifiedFor(99L));
}
```

**Test:** `ensureCertificationsAreDefensivelyCopied`

```java
@Test
void ensureCertificationsAreDefensivelyCopied() {
    final Set<Long> source = new HashSet<>(Set.of(1L));
    final Pilot pilot = new Pilot(validUser("pilot7"), validCompany(), source);
    source.add(99L);
    assertEquals(1, pilot.certifiedAircraftModelIds().size());
    assertFalse(pilot.isCertifiedFor(99L));
}
```

**Test:** `ensureCertificationsAreUnmodifiable`

```java
@Test
void ensureCertificationsAreUnmodifiable() {
    final Pilot pilot = new Pilot(validUser("pilot8"), validCompany(), Set.of(1L));
    assertThrows(UnsupportedOperationException.class,
            () -> pilot.certifiedAircraftModelIds().add(2L));
}
```

**Test:** `ensurePilotHasCorrectUser`

```java
@Test
void ensurePilotHasCorrectUser() {
    final User user = validUser("pilot9");
    final Pilot pilot = new Pilot(user, validCompany(), Set.of(1L));
    assertEquals(user, pilot.user());
}
```

**Test:** `ensureToStringContainsCompany`

```java
@Test
void ensureToStringContainsCompany() {
    final Pilot pilot = new Pilot(validUser("pilot10"), validCompany(), Set.of(1L));
    assertTrue(pilot.toString().contains("TP"));
}
```

**Test:** `ensureEqualsReturnsTrueForSameInstance`

```java
@Test
void ensureEqualsReturnsTrueForSameInstance() {
    final Pilot pilot = new Pilot(validUser("pilot11"), validCompany(), Set.of(1L));
    assertEquals(pilot, pilot);
}
```

**Test:** `ensureEqualsReturnsFalseForNull`

```java
@Test
void ensureEqualsReturnsFalseForNull() {
    final Pilot pilot = new Pilot(validUser("pilot12"), validCompany(), Set.of(1L));
    assertNotEquals(null, pilot);
}
```

**Test:** `ensureHashCodeIsConsistent`

```java
@Test
void ensureHashCodeIsConsistent() {
    final Pilot pilot = new Pilot(validUser("pilot13"), validCompany(), Set.of(1L));
    assertEquals(pilot.hashCode(), pilot.hashCode());
}
```

**Test:** `ensureSameAsReturnsTrueForSameInstance`

```java
@Test
void ensureSameAsReturnsTrueForSameInstance() {
    final Pilot pilot = new Pilot(validUser("pilot14"), validCompany(), Set.of(1L));
    assertTrue(pilot.sameAs(pilot));
}
```

---

### `AddPilotControllerTest`

Location: `src/test/java/aisafe/pilot/application/AddPilotControllerTest.java`

> Note: tests configure `AuthzRegistry`, then use helpers `persistCompany(code)`, `authenticateAsAtccOf(company)` (creates an ATCC system user + `Collaborator` of the company and authenticates), and `persistAircraftModel()` (persists a valid model and returns it with its assigned id). `AuthenticationContext` is cleared after each test.

**Test:** `ensurePilotIsAddedToAuthenticatedCollaboratorCompany`

```java
@Test
void ensurePilotIsAddedToAuthenticatedCollaboratorCompany() {
    final String companyCode = nextCompanyCode();
    final AirTransportCompany company = persistCompany(companyCode);
    final String atccUsername = authenticateAsAtccOf(company);
    final AircraftModel model = persistAircraftModel();

    final String pilotUsername = "pilot-" + SEQUENCE.incrementAndGet();
    final Pilot pilot = controller.addPilot(
            pilotUsername, PASSWORD, "Maria", "Voo",
            pilotUsername + "@aisafe.com", "912345678", "Captain",
            new Email(pilotUsername + "@aisafe.com"),
            new SecurityClearance(SecurityLevel.HIGH, LocalDate.now().plusYears(1)),
            LocalDate.now(),
            Set.of(model.identity()));

    assertNotNull(pilot);
    assertEquals(company.identity(), pilot.companyIataCode());
    assertTrue(pilot.isCertifiedFor(model.identity()));
    assertTrue(pilot.isActive());
    assertTrue(pilot.user().systemUser().hasAny(AiSafeRoles.PILOT));

    assertNotNull(atccUsername);
}
```

**Test:** `ensurePilotRequiresAtLeastOneCertification`

```java
@Test
void ensurePilotRequiresAtLeastOneCertification() {
    final AirTransportCompany company = persistCompany(nextCompanyCode());
    authenticateAsAtccOf(company);

    final String pilotUsername = "pilot-" + SEQUENCE.incrementAndGet();
    assertThrows(IllegalArgumentException.class, () -> controller.addPilot(
            pilotUsername, PASSWORD, "No", "Cert",
            pilotUsername + "@aisafe.com", "912345678", "Captain",
            new Email(pilotUsername + "@aisafe.com"),
            new SecurityClearance(SecurityLevel.HIGH, LocalDate.now().plusYears(1)),
            LocalDate.now(),
            Set.of()));
}
```

**Test:** `ensureUnknownAircraftModelIsRejected`

```java
@Test
void ensureUnknownAircraftModelIsRejected() {
    final AirTransportCompany company = persistCompany(nextCompanyCode());
    authenticateAsAtccOf(company);

    final String pilotUsername = "pilot-" + SEQUENCE.incrementAndGet();
    assertThrows(IllegalArgumentException.class, () -> controller.addPilot(
            pilotUsername, PASSWORD, "Bad", "Model",
            pilotUsername + "@aisafe.com", "912345678", "Captain",
            new Email(pilotUsername + "@aisafe.com"),
            new SecurityClearance(SecurityLevel.HIGH, LocalDate.now().plusYears(1)),
            LocalDate.now(),
            Set.of(999_999L)));
}
```

---

## Coverage by Acceptance Criterion

- **AC075.1** (pilot is a registered system user with the `PILOT` role): `ensurePilotIsAddedToAuthenticatedCollaboratorCompany` asserts `pilot.user().systemUser().hasAny(PILOT)`.
- **AC075.1** (pilot is a registered system user with the `PILOT` role): `ensurePilotIsAddedToAuthenticatedCollaboratorCompany` asserts `pilot.user().systemUser().hasAny(PILOT)`.
- **AC075.3** (certified for at least one aircraft model): `ensurePilotMustHaveAtLeastOneCertification`, `ensurePilotCertificationSetCannotBeNull`, `ensureCertificationsCannotContainNull`, `ensureIsCertifiedForReturnsTrueForCertifiedModel` (domain invariant); `ensurePilotRequiresAtLeastOneCertification` (controller validation).
- **AC075.4** (all referenced aircraft models must exist): `ensureUnknownAircraftModelIsRejected` (controller validation).
- **AC075.2** (associated with the authenticated collaborator's company): `ensurePilotIsAddedToAuthenticatedCollaboratorCompany` asserts `pilot.companyIataCode()` equals the ATCC's company; the company is never passed by the caller — it is resolved from the session. Only an authenticated ATCC may perform this action (`authz.ensureAuthenticatedUserHasAnyOf(ATCC)`).
- **AC075.5** (newly added pilot is active by default): `ensurePilotIsActiveByDefault`, also asserted in `ensureValidPilotCanBeCreated`.
- Identity / value semantics: `ensurePilotHasCorrectUser`, `ensureCertificationsAreDefensivelyCopied`, `ensureCertificationsAreUnmodifiable`, `ensureToStringContainsCompany`, `ensureEqualsReturnsTrueForSameInstance`, `ensureEqualsReturnsFalseForNull`, `ensureHashCodeIsConsistent`, `ensureSameAsReturnsTrueForSameInstance`.

Total: 16 domain tests + 3 controller tests = 19 automated tests for US075. The full `aisafe.base` suite passes with these additions.

---

## Acceptance Tests

Authorization (ATCC role) and the company resolution from the authenticated session are exercised by the controller test through a real `Collaborator` set-up and `AuthenticationContext`. The remaining flow is verified manually.

**Manual test — AC075.1 / AC075.2 / AC075.3 (full registration flow):**

1. Run the back-office app and login as an ATCC user of a company (e.g. `TP`).
2. Navigate to `Pilots > Add Pilot`.
3. From the listed aircraft models, enter one or more model ids (e.g. `1,2`).
4. Provide the pilot's username, password, names, e-mails, phone, position, security clearance and skills date.
5. Expected: confirmation message; the pilot is registered with the `PILOT` role, associated with company `TP`, certified for the selected models.

**Manual test — AC075.2 (no certification rejected):**

1. Leave the certified model ids empty.
2. Expected: `A pilot must be certified for at least one aircraft model.`

**Manual test — AC075.2 (unknown model rejected):**

1. Enter a model id that does not exist.
2. Expected: `Aircraft model not found: <id>.`

**Manual test — AC075.4 (role enforcement):**

1. Login as a user without the ATCC role.
2. Expected: the `Pilots` menu is not available.
