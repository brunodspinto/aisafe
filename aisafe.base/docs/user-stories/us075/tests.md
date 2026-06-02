# US075 — Tests and Coverage

## Scope

US075 covers adding a pilot to the authenticated collaborator's air transport company. A pilot is a system user (with the `PILOT` role) certified for one or more existing aircraft models. Testing is split between automated unit tests for the `Pilot` aggregate (domain invariants) and manual acceptance tests for the complete end-to-end use case.

In line with the project's testing convention, the `Application` layer (controllers) is not unit-tested — controllers are thin orchestrators that delegate to the domain and repositories, and are exercised end-to-end through the manual acceptance tests.

## Automated Tests

### `PilotTest`

Location: `src/test/java/aisafe/pilot/domain/PilotTest.java`

> Note: tests use helper methods `validUser(String)` (builds a system user with the `PILOT` role wrapped in a `User`) and `validCompany()` (returns `IATACode.valueOf("TP")`).

---

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

## Coverage by Acceptance Criterion

- **AC075.1** (pilot is a registered system user with the `PILOT` role): the role is assigned by the controller; the domain ensures the pilot wraps a non-null `User`. Covered by `ensureUserCannotBeNull` and validated end-to-end by the manual acceptance test for AC075.1.
- **AC075.2** (associated with the authenticated collaborator's company): the company is resolved automatically by the controller from the authenticated session and stored as `companyIataCode` in the aggregate. Covered by the manual acceptance test for AC075.2 (the cross-company check confirms an ATCC cannot add a pilot to another company); `ensureCompanyCannotBeNull` guarantees the invariant at the domain level.
- **AC075.3** (certified for at least one aircraft model): `ensurePilotMustHaveAtLeastOneCertification`, `ensurePilotCertificationSetCannotBeNull`, `ensureCertificationsCannotContainNull`, `ensureIsCertifiedForReturnsTrueForCertifiedModel`.
- **AC075.4** (all referenced aircraft models must already exist): validated by the controller against `AircraftModelRepository.ofIdentity(...)`. Covered by the manual acceptance test for AC075.4 (including the mixed valid/invalid scenario).
- **AC075.5** (newly added pilot is active by default): `ensurePilotIsActiveByDefault`, also asserted in `ensureValidPilotCanBeCreated`.
- Identity / value semantics: `ensurePilotHasCorrectUser`, `ensureCertificationsAreDefensivelyCopied`, `ensureCertificationsAreUnmodifiable`, `ensureToStringContainsCompany`, `ensureEqualsReturnsTrueForSameInstance`, `ensureEqualsReturnsFalseForNull`, `ensureHashCodeIsConsistent`, `ensureSameAsReturnsTrueForSameInstance`.

Total: **16 automated unit tests** for the `Pilot` aggregate, all passing. The full `aisafe.base` test suite (518 tests) passes with these additions.

---

## Acceptance Tests

The following manual acceptance tests validate the user story end-to-end against the running system.

### Prerequisites

Before running the acceptance tests:

1. The H2 database server must be running (`./start-h2.sh` or `./start-h2.bat`).
2. The bootstrap must have been executed once (`./run-bootstrap.sh` or `./run-bootstrap.bat`) so that the system has:
   - At least one `AirTransportCompany` registered (e.g. `TP — TAP Air Portugal`).
   - At least two `AircraftModel`s registered (so that multi-model certifications can be tested).
   - At least one Backoffice Operator and one ATCC collaborator of that company (e.g. username `atcc1`, password `Password1`).
3. Run the application with `./run-jpa.sh` or `./run-jpa.bat`.

---

### AC075.1 — A pilot must be a system user with the PILOT role

**Goal:** confirm that completing the "Add Pilot" flow creates a new system user and that this user holds the PILOT role.

**Steps:**

1. Login as an ATCC (e.g. `atcc1` / `Password1`).
2. From the main menu, select `Pilots >` → `Add Pilot`.
3. Select one of the listed aircraft models as the pilot's certification.
4. Provide all required user data (username `pilot1`, password `Password1`, first name, last name, both e-mails, phone, position, security clearance and skills assessment date).
5. Confirm the registration.
6. Logout.
7. Login as Admin (`admin` / `Password1`) and open `Users > List Users`.

**Expected:**
- The console confirms `Pilot successfully registered!` and prints `Username: pilot1`.
- In the admin list, `pilot1` appears with the `PILOT` role assigned.
- The mecanographic number of the new user starts with the prefix `PIL` (e.g. `PIL00001`), distinguishing it from non-pilot users that use the `EMP` prefix.

---

### AC075.2 — A pilot must belong to exactly one air transport company (the authenticated collaborator's)

**Goal:** confirm that the pilot is automatically associated with the ATCC's own company and that the ATCC is never offered the choice of a different company.

**Steps:**

1. Login as an ATCC of company **TP** (e.g. `atcc1`).
2. Navigate to `Pilots > Add Pilot`.
3. Observe the entire input flow.
4. Complete the registration with a new username (e.g. `pilot-tp-1`).

**Expected:**
- At no point during the flow is the operator asked to select or type a company / IATA code.
- The final success message shows `Company: TP`.
- The pilot is persisted with `companyIataCode = TP` (verifiable in the database table `T_PILOT`, column `company_iata_code`).

**Cross-company check:**

1. Logout.
2. Login as an ATCC of a different company (e.g. an ATCC of `BA`).
3. Add a new pilot.
4. Expected: the new pilot is associated with `BA`, never with `TP`. There is no UI path that allows one ATCC to add a pilot to another company.

---

### AC075.3 — A pilot must be certified for at least one aircraft model

**Goal:** confirm that the system rejects any attempt to create a pilot with no certifications.

**Steps:**

1. Login as an ATCC.
2. Navigate to `Pilots > Add Pilot`.
3. When prompted for certified aircraft model ids, leave the field empty (just press Enter).
4. Continue filling the remaining fields.

**Expected:**
- Before any user data is persisted, the system rejects the operation with the message:
  ```
  A pilot must be certified for at least one aircraft model.
  ```
- No new `SystemUser`, no new `User`, and no new `Pilot` are created (verifiable in the database — no new rows in `T_AISAFE_USER` or `T_PILOT`).

---

### AC075.4 — All aircraft models referenced in the certifications must already exist

**Goal:** confirm that the system validates each chosen aircraft model id against the existing catalogue.

**Steps:**

1. Login as an ATCC.
2. Navigate to `Pilots > Add Pilot`.
3. Note the listed aircraft model ids.
4. When prompted for certified ids, enter an id that is not in the list (e.g. `999999`).

**Expected:**
- The system rejects the operation with the message:
  ```
  Aircraft model not found: 999999.
  ```
- No new `Pilot` is created. The transaction is rolled back, so no orphan `SystemUser` is left behind.

**Mixed valid/invalid:**

1. Provide a comma-separated list with one valid id and one invalid id (e.g. `1,999999`).
2. Expected: the operation is still rejected — every id must exist before the pilot is created.

---

### AC075.5 — A newly added pilot is active by default

**Goal:** confirm that a freshly registered pilot is immediately usable.

**Steps:**

1. Login as an ATCC and add a new pilot (`pilot-active-1`).
2. Logout.
3. Login as the newly created pilot (`pilot-active-1` / the password chosen during registration).

**Expected:**
- Login succeeds.
- The pilot menu is displayed.
- In the persisted `T_PILOT` table, the column `active` holds the value `true` for the new pilot.

---

### Authorization

**Goal:** confirm that only authenticated ATCCs can perform this action.

**Test A — non-ATCC user:**

1. Login as a Backoffice Operator (no ATCC role).
2. Inspect the main menu.

**Expected:**
- The `Pilots >` submenu is not visible — the menu is rendered based on the authenticated user's roles.

**Test B — unauthenticated:**

1. Without logging in, attempt to invoke the use case (e.g. via an automated client or by skipping login if possible).

**Expected:**
- The controller rejects the call with an authorization error (`UnauthorizedException` / "User not authenticated"). No state is changed.

---

### Edge cases

**Duplicate username:**

1. As ATCC, add a pilot with username `pilot-dup`.
2. Repeat the flow trying to add another pilot with the same username `pilot-dup`.

**Expected:** the second registration is rejected by the EAPLI `UserManagementService` with a uniqueness violation message. The transaction is rolled back — no orphan `Pilot` is created.

**Invalid e-mail format:**

1. As ATCC, start adding a pilot and enter an invalid e-mail (e.g. `not-an-email`) when prompted for the AISafe `Email` value object.

**Expected:** the operation is rejected with an `IllegalArgumentException` raised by the `Email` value object constructor. The UI catches it and shows a friendly validation message.

**Password not meeting the policy:**

1. As ATCC, try to register a pilot with a password that fails the policy (e.g. `pass`).

**Expected:** the EAPLI `UserManagementService` rejects the password through `AiSafePasswordPolicy` (minimum 6 characters, at least one digit and one uppercase letter).

**Expired security clearance:**

1. As ATCC, try to register a pilot with a security clearance whose expiration date is in the past.

**Expected:** the `SecurityClearance` value object constructor rejects the input — the expiration date must be today or in the future.

---

## Coverage matrix (acceptance tests)

| AC | Manual test |
|----|-------------|
| AC075.1 | "AC075.1 — A pilot must be a system user with the PILOT role" |
| AC075.2 | "AC075.2 — A pilot must belong to exactly one air transport company" (plus cross-company check) |
| AC075.3 | "AC075.3 — A pilot must be certified for at least one aircraft model" |
| AC075.4 | "AC075.4 — All aircraft models referenced must already exist" (plus mixed valid/invalid) |
| AC075.5 | "AC075.5 — A newly added pilot is active by default" |
| Authorization | "Authorization — Test A" (menu hiding) and "Test B" (controller rejection) |
| Robustness | "Edge cases" (duplicate username, invalid e-mail, weak password, expired clearance) |
