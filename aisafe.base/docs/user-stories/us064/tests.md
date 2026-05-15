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
