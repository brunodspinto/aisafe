# US063 — Tests and Coverage

## Scope

US063 covers editing the contact information (email and phone number) of an active collaborator of a customer.

## Automated Tests

### `UserTest`

Location: `src/test/java/aisafe/usermanagement/domain/UserTest.java`

- `ensureUpdateContactChangesEmailAndPhone`
- `ensureUpdateContactRejectsNullEmail`
- `ensureUpdateContactRejectsNullPhone`
- `ensureUpdateContactRejectsBlankPhone`

## Coverage by Acceptance Criterion

- AC063.1: UI prompts for customer type selection
- AC063.2: UI loads and displays available customers before asking for selection
- AC063.3: Controller uses `CollaboratorRepository` to load only active collaborators for selection
- AC063.4: `ensureUpdateContactRejectsNullEmail`; email format validated by `Email` value object
- AC063.5: `ensureUpdateContactRejectsNullPhone`, `ensureUpdateContactRejectsBlankPhone`
- AC063.6: Controller checks `BACKOFFICE_OPERATOR` or `ADMIN` role via `AuthorizationService`
