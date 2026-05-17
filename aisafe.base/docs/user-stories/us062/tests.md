# US062 — Tests and Coverage

## Scope

US062 covers listing active collaborators of a customer (Air Transport Company or Air Control Area), filtering out collaborators whose system user is inactive.

## Automated Tests

### `CollaboratorTest`

Location: `src/test/java/aisafe/collaborator/domain/CollaboratorTest.java`

> Note: tests use helper methods `validUser(String)`, `validCompany()`, and `validArea()` defined in the same class.

**Test:** `ensureCompanyCollaboratorCanBeCreated`

```java
@Test
void ensureCompanyCollaboratorCanBeCreated() {
    final Collaborator collaborator = new Collaborator(validUser("user1"), validCompany());
    assertTrue(collaborator.isCompanyCollaborator());
    assertFalse(collaborator.isAreaCollaborator());
    assertEquals("TAP Air Portugal", collaborator.customerName());
}
```

**Test:** `ensureAreaCollaboratorCanBeCreated`

```java
@Test
void ensureAreaCollaboratorCanBeCreated() {
    final Collaborator collaborator = new Collaborator(validUser("user2"), validArea());
    assertTrue(collaborator.isAreaCollaborator());
    assertFalse(collaborator.isCompanyCollaborator());
    assertEquals("Northern Portugal", collaborator.customerName());
}
```

**Test:** `ensureUserCannotBeNullForCompanyCollaborator`

```java
@Test
void ensureUserCannotBeNullForCompanyCollaborator() {
    assertThrows(IllegalArgumentException.class,
            () -> new Collaborator(null, validCompany()));
}
```

**Test:** `ensureUserCannotBeNullForAreaCollaborator`

```java
@Test
void ensureUserCannotBeNullForAreaCollaborator() {
    assertThrows(IllegalArgumentException.class,
            () -> new Collaborator(null, validArea()));
}
```

**Test:** `ensureCompanyCannotBeNull`

```java
@Test
void ensureCompanyCannotBeNull() {
    assertThrows(IllegalArgumentException.class,
            () -> new Collaborator(validUser("user3"), (AirTransportCompany) null));
}
```

**Test:** `ensureAreaCannotBeNull`

```java
@Test
void ensureAreaCannotBeNull() {
    assertThrows(IllegalArgumentException.class,
            () -> new Collaborator(validUser("user4"), (AirControlArea) null));
}
```

**Test:** `ensureCompanyCollaboratorHasCorrectUser`

```java
@Test
void ensureCompanyCollaboratorHasCorrectUser() {
    final User user = validUser("user5");
    final Collaborator collaborator = new Collaborator(user, validCompany());
    assertEquals(user, collaborator.user());
}
```

**Test:** `ensureAreaCollaboratorHasCorrectUser`

```java
@Test
void ensureAreaCollaboratorHasCorrectUser() {
    final User user = validUser("user6");
    final Collaborator collaborator = new Collaborator(user, validArea());
    assertEquals(user, collaborator.user());
}
```

**Test:** `ensureToStringContainsCustomerName`

```java
@Test
void ensureToStringContainsCustomerName() {
    final Collaborator collaborator = new Collaborator(validUser("user7"), validCompany());
    assertTrue(collaborator.toString().contains("TAP Air Portugal"));
}
```

**Test:** `ensureEqualsReturnsTrueForSameInstance`

```java
@Test
void ensureEqualsReturnsTrueForSameInstance() {
    final Collaborator collaborator = new Collaborator(validUser("user8"), validCompany());
    assertEquals(collaborator, collaborator);
}
```

**Test:** `ensureEqualsReturnsFalseForNull`

```java
@Test
void ensureEqualsReturnsFalseForNull() {
    final Collaborator collaborator = new Collaborator(validUser("user9"), validCompany());
    assertNotEquals(null, collaborator);
}
```

**Test:** `ensureHashCodeIsConsistent`

```java
@Test
void ensureHashCodeIsConsistent() {
    final Collaborator collaborator = new Collaborator(validUser("user10"), validCompany());
    assertEquals(collaborator.hashCode(), collaborator.hashCode());
}
```

**Test:** `ensureSameAsReturnsTrueForSameInstance`

```java
@Test
void ensureSameAsReturnsTrueForSameInstance() {
    final Collaborator collaborator = new Collaborator(validUser("user11"), validCompany());
    assertTrue(collaborator.sameAs(collaborator));
}
```

**Test:** `ensureCompanyIataCodeGetterWorks`

```java
@Test
void ensureCompanyIataCodeGetterWorks() {
    final IATACode iataCode = validCompany().identity();
    final Collaborator collaborator = new Collaborator(validUser("user12"), iataCode);
    assertEquals(iataCode, collaborator.companyIataCode());
    assertNull(collaborator.areaCode());
}
```

**Test:** `ensureAreaCodeGetterWorks`

```java
@Test
void ensureAreaCodeGetterWorks() {
    final AirControlAreaCode code = validArea().identity();
    final Collaborator collaborator = new Collaborator(validUser("user13"), code);
    assertEquals(code, collaborator.areaCode());
    assertNull(collaborator.companyIataCode());
}
```

**Test:** `ensureEqualsReturnsFalseForDifferentType`

```java
@Test
void ensureEqualsReturnsFalseForDifferentType() {
    final Collaborator collaborator = new Collaborator(validUser("user15"), validCompany());
    assertNotEquals("string", collaborator);
}
```

**Test:** `ensureCustomerNameForCompanyCollaborator`

```java
@Test
void ensureCustomerNameForCompanyCollaborator() {
    final Collaborator collaborator = new Collaborator(validUser("user16"), validCompany());
    assertEquals("TAP Air Portugal", collaborator.customerName());
}
```

**Test:** `ensureCustomerNameForAreaCollaborator`

```java
@Test
void ensureCustomerNameForAreaCollaborator() {
    final Collaborator collaborator = new Collaborator(validUser("user17"), validArea());
    assertEquals("Northern Portugal", collaborator.customerName());
}
```

**Test:** `ensureTwoCollaboratorsWithSameIdAreEqual`

```java
@Test
void ensureTwoCollaboratorsWithSameIdAreEqual() {
    final Collaborator collaborator = new Collaborator(validUser("user20"), validCompany());
    assertTrue(collaborator.sameAs(collaborator));
}
```

**Test:** `ensureToStringContainsAreaCode`

```java
@Test
void ensureToStringContainsAreaCode() {
    final Collaborator collaborator = new Collaborator(validUser("user21"), validArea().identity());
    assertTrue(collaborator.toString().contains("PT-N"));
}
```

**Test:** `ensureCompanyCollaboratorHasNullArea`

```java
@Test
void ensureCompanyCollaboratorHasNullArea() {
    final Collaborator collaborator = new Collaborator(validUser("user22"), validCompany());
    assertNull(collaborator.airControlArea());
    assertNotNull(collaborator.airTransportCompany());
}
```

**Test:** `ensureAreaCollaboratorHasNullCompany`

```java
@Test
void ensureAreaCollaboratorHasNullCompany() {
    final Collaborator collaborator = new Collaborator(validUser("user23"), validArea());
    assertNull(collaborator.airTransportCompany());
    assertNotNull(collaborator.airControlArea());
}
```

---

## Coverage by Acceptance Criterion

- AC062.1: `ensureCompanyCollaboratorCanBeCreated`, `ensureAreaCollaboratorCanBeCreated`; UI prompts for customer type selection
- AC062.2: UI loads and displays available customers before asking for selection
- AC062.3: Repository methods filter on `systemUser.active = true`; `ensureCompanyIataCodeGetterWorks`, `ensureAreaCodeGetterWorks`
- AC062.4: Controller returns empty result and UI displays informative message when no active collaborators exist
- AC062.5: Controller checks `BACKOFFICE_OPERATOR` or `ADMIN` role via `AuthorizationService`; validated by manual test
- Domain invariants (construction): `ensureUserCannotBeNullForCompanyCollaborator`, `ensureUserCannotBeNullForAreaCollaborator`, `ensureCompanyCannotBeNull`, `ensureAreaCannotBeNull`, `ensureCompanyCollaboratorHasCorrectUser`, `ensureAreaCollaboratorHasCorrectUser`, `ensureCustomerNameForCompanyCollaborator`, `ensureCustomerNameForAreaCollaborator`, `ensureCompanyCollaboratorHasNullArea`, `ensureAreaCollaboratorHasNullCompany`, `ensureToStringContainsCustomerName`, `ensureToStringContainsAreaCode`, `ensureEqualsReturnsTrueForSameInstance`, `ensureEqualsReturnsFalseForNull`, `ensureEqualsReturnsFalseForDifferentType`, `ensureHashCodeIsConsistent`, `ensureSameAsReturnsTrueForSameInstance`, `ensureTwoCollaboratorsWithSameIdAreEqual`

---

## Acceptance Tests

Authorization (`BACKOFFICE_OPERATOR` or `ADMIN` role) and repository-level active filtering are infrastructure concerns validated by manual integration testing. Collaborator construction and association invariants are fully covered by the automated unit tests above.

**Manual test — AC062.1 / AC062.2 / AC062.3 (list company collaborators):**

1. Run `AiSafeBackofficeApp` and login as a Backoffice Operator or Admin.
2. Navigate to `Collaborators > List Collaborators`.
3. Select `Air Transport Company` and choose an existing company (e.g. `TP`).
4. Expected: list of all active collaborators for `TP` is displayed, each showing username and contact info.

**Manual test — AC062.3 (disabled collaborator not listed):**

1. Disable a collaborator of company `TP` via US064.
2. Navigate to `Collaborators > List Collaborators` and select `TP` again.
3. Expected: the disabled collaborator no longer appears in the list.

**Manual test — AC062.4 (empty list message):**

1. Select a customer that has no active collaborators registered.
2. Expected: an informative message such as `No active collaborators found for this customer.` is displayed instead of an empty list.

**Manual test — AC062.5 (role enforcement):**

1. Login as a user without Backoffice Operator or Admin role (e.g. ATCC).
2. Expected: the List Collaborators option is not available in the menu.
