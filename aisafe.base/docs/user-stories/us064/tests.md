# US064 — Tests and Coverage

## Scope

US064 covers disabling an active collaborator of a customer, which deactivates the collaborator's system user account.

## Automated Tests

### `CollaboratorTest`

Location: `src/test/java/aisafe/collaborator/domain/CollaboratorTest.java`

- `ensureCompanyCollaboratorCanBeCreated`
- `ensureAreaCollaboratorCanBeCreated`
- `ensureUserCannotBeNullForCompanyCollaborator`
- `ensureUserCannotBeNullForAreaCollaborator`
- `ensureCompanyCannotBeNull`
- `ensureAreaCannotBeNull`
- `ensureCustomerNameForCompanyCollaborator`
- `ensureCustomerNameForAreaCollaborator`
- `ensureCompanyCollaboratorHasNullArea`
- `ensureAreaCollaboratorHasNullCompany`

## Coverage by Acceptance Criterion

- AC064.1: UI prompts for customer type then customer selection
- AC064.2: Controller uses `CollaboratorRepository` filtering to list only active collaborators
- AC064.3: Deactivation delegates to `SystemUser.deactivate()` — covered by EAPLI framework behaviour
- AC064.4: `SystemUser.deactivate()` throws when already inactive — enforced by EAPLI framework
- AC064.5: Controller checks `BACKOFFICE_OPERATOR` or `ADMIN` role via `AuthorizationService`

---

### 4.2. Acceptance Tests

Authorization and the deactivation flow via EAPLI's `UserManagementService` are infrastructure concerns validated by manual integration testing. The collaborator domain structure is covered by the automated unit tests above.

**Manual test — AC064.1 / AC064.2 / AC064.3 (disable collaborator):**

1. Run `AiSafeBackofficeApp` and login as a Backoffice Operator.
2. Navigate to `Collaborators > Disable Collaborator`.
3. Select `Air Transport Company` and choose an existing company (e.g. `TP`).
4. Select an active collaborator from the list and confirm the operation.
5. Expected: confirmation message displayed; the collaborator no longer appears when listing active collaborators for `TP`.

**Manual test — AC064.4 (disabling already-inactive collaborator not possible):**

1. After disabling a collaborator, run the disable flow again for the same customer.
2. Expected: the previously disabled collaborator does not appear in the active list and therefore cannot be selected for disabling.

**Manual test — AC064.5 (role enforcement):**

1. Login as a user without Backoffice Operator or Admin role (e.g. ATCC).
2. Expected: the Disable Collaborator option is not available in the menu.
