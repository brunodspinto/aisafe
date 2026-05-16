# US061 — Tests and Coverage

## Scope

US061 covers registering a collaborator (company or ATCC) associated with exactly one customer (Air Transport Company or Air Control Area).

## Automated Tests

### `CollaboratorTest`

Location: `src/test/java/aisafe/collaborator/domain/CollaboratorTest.java`

**Criação e associação:**
- `ensureCompanyCollaboratorCanBeCreated`
- `ensureAreaCollaboratorCanBeCreated`
- `ensureUserCannotBeNullForCompanyCollaborator`
- `ensureUserCannotBeNullForAreaCollaborator`
- `ensureCompanyCannotBeNull`
- `ensureAreaCannotBeNull`

**Getters e atributos:**
- `ensureCompanyCollaboratorHasCorrectUser`
- `ensureAreaCollaboratorHasCorrectUser`
- `ensureAirTransportCompanyGetterWorks`
- `ensureAirControlAreaGetterWorks`
- `ensureCompanyCollaboratorHasNullArea`
- `ensureAreaCollaboratorHasNullCompany`
- `ensureCustomerNameForCompanyCollaborator`
- `ensureCustomerNameForAreaCollaborator`

**Identidade e igualdade:**
- `ensureEqualsReturnsTrueForSameInstance`
- `ensureEqualsReturnsFalseForNull`
- `ensureEqualsReturnsFalseForDifferentType`
- `ensureHashCodeIsConsistent`
- `ensureSameAsReturnsTrueForSameInstance`
- `ensureTwoCollaboratorsWithSameIdAreEqual`

**toString:**
- `ensureToStringContainsCustomerName`
- `ensureToStringContainsAreaName`

## Coverage by Acceptance Criterion

- AC061.1: `ensureCompanyCollaboratorCanBeCreated`, `ensureAreaCollaboratorCanBeCreated`, `ensureCompanyCannotBeNull`, `ensureAreaCannotBeNull`
- AC061.2: `ensureCompanyCollaboratorHasCorrectUser`, `ensureUserCannotBeNullForCompanyCollaborator`, `ensureUserCannotBeNullForAreaCollaborator`
- AC061.3: Uniqueness of system user enforced by EAPLI framework
- AC061.4: No email domain verification — by design
- AC061.5: Covered by `AiSafeBootstrap` which registers fco1 and atcc1 idempotently

---

### 4.2. Acceptance Tests

Authorization and system user uniqueness are infrastructure concerns validated by manual integration testing. The collaborator association rules (company vs area) are covered by the automated unit tests above.

**Manual test — AC061.1 / AC061.2 (register company collaborator):**

1. Run `AiSafeBackofficeApp` and login as a Backoffice Operator.
2. Navigate to `Collaborators > Register Collaborator`.
3. Select customer type `Air Transport Company` and choose an existing company (e.g. `TP`).
4. Provide user details: username `atcc2`, password `Password1`, first name `Ana`, last name `Silva`, phone `912000001`, email `asilva@tap.pt`, role `ATCC`.
5. Expected: confirmation message displayed and collaborator visible when listing collaborators for `TP`.

**Manual test — AC061.1 / AC061.2 (register area collaborator):**

1. Select customer type `Air Control Area` and choose an existing area (e.g. `EUR`).
2. Provide user details with role `FLIGHT_CONTROL_OPERATOR`.
3. Expected: confirmation message displayed and collaborator visible when listing collaborators for `EUR`.

**Manual test — AC061.3 (duplicate username rejected):**

1. Attempt to register a collaborator using a username that already exists in the system.
2. Expected: the system rejects the operation with a uniqueness violation error.
