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

---

### 4.2. Acceptance Tests

Authorization and active-collaborator filtering are infrastructure concerns validated by manual integration testing. Email and phone validations are covered by the automated unit tests above.

**Manual test — AC063.1 / AC063.2 / AC063.3 (full edit flow):**

1. Run `AiSafeBackofficeApp` and login as a Backoffice Operator.
2. Navigate to `Collaborators > Edit Collaborator Contact`.
3. Select `Air Transport Company`, choose an existing company (e.g. `TP`), then select an active collaborator from the list.
4. Provide new email `newemail@tap.pt` and phone `913000000`.
5. Expected: confirmation message displayed and the updated details are reflected when the collaborator is listed again.

**Manual test — AC063.4 (invalid email rejected):**

1. At the email prompt enter `bademail` (missing domain).
2. Expected: an error message is shown and the form re-prompts for a valid email address.

**Manual test — AC063.5 (blank phone rejected):**

1. At the phone prompt leave the input blank or enter only spaces.
2. Expected: an error message is shown and the form re-prompts for a non-blank phone number.

**Manual test — AC063.6 (role enforcement):**

1. Login as a user without Backoffice Operator or Admin role.
2. Expected: the Edit Collaborator Contact option is not available in the menu.
