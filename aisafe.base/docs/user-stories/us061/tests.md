# US061 — Tests and Coverage

## Scope

US061 covers registering a collaborator (company or ATCC) associated with exactly one customer (Air Transport Company or Air Control Area).

## Automated Tests

### `CollaboratorTest`

Location: `src/test/java/aisafe/collaborator/domain/CollaboratorTest.java`

- `ensureCompanyCollaboratorCanBeCreated`
- `ensureCompanyCollaboratorHasCorrectUser`
- `ensureAreaCollaboratorCanBeCreated`
- `ensureUserCannotBeNullForCompanyCollaborator`
- `ensureUserCannotBeNullForAreaCollaborator`
- `ensureCompanyCannotBeNull`
- `ensureAreaCannotBeNull`
- `ensureAirTransportCompanyGetterWorks`
- `ensureAirControlAreaGetterWorks`

## Coverage by Acceptance Criterion

- AC061.1: `ensureCompanyCollaboratorCanBeCreated`, `ensureAreaCollaboratorCanBeCreated`, `ensureCompanyCannotBeNull`, `ensureAreaCannotBeNull`
- AC061.2: `ensureCompanyCollaboratorHasCorrectUser`, `ensureUserCannotBeNullForCompanyCollaborator`, `ensureUserCannotBeNullForAreaCollaborator`
- AC061.3: Uniqueness of system user enforced by EAPLI framework
- AC061.4: No email domain verification — by design
- AC061.5: Covered by `AiSafeBootstrap` which registers fco1 and atcc1 idempotently
