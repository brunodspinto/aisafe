# US031 — Tests and Coverage

## Scope

US031 covers registering a backoffice user with all required attributes (username, password, name, phone, email, position, security clearance, skills date) by an Administrator.

## Automated Tests

### `UserTest`

Location: `src/test/java/aisafe/usermanagement/domain/UserTest.java`

- `ensureUsersWithSameMecanographicNumberAreEqual`
- `ensureUserConstructorRejectsNullSystemUser`

### `EmailTest` / `UserTest` — email validation

- `ensureEmailRejectsInvalidFormat`
- `ensureEmailRejectsMissingDomain`
- `ensureEmailIsSavedLowercase`
- `ensureEmailRejectsBlank`

### `AiSafePasswordPolicyTest`

Location: `src/test/java/aisafe/usermanagement/domain/AiSafePasswordPolicyTest.java`

- `ensurePasswordShorterThanSixCharsIsRejected`
- `ensurePasswordWithoutDigitIsRejected`
- `ensurePasswordWithoutCapitalLetterIsRejected`
- `ensureValidPasswordIsAccepted`
- `ensurePasswordWithExactlyMinimumLengthIsAccepted`
- `ensureNullPasswordIsRejected`
- `ensureEmptyPasswordIsRejected`

### `SecurityClearanceTest` / `UserTest` — security clearance

- `ensureSecurityClearanceIsActiveOnExpirationDay`
- `ensureElevatedAndAboveRequireBodyScan`
- `ensureIsAtLeastRespectsOrder`

## Coverage by Acceptance Criterion

- AC031.1: `ensureUsersWithSameMecanographicNumberAreEqual`, `ensureUserConstructorRejectsNullSystemUser`
- AC031.2: `ensureEmailRejectsInvalidFormat`, `ensureEmailRejectsMissingDomain`, `ensureEmailIsSavedLowercase`, `ensureEmailRejectsBlank`
- AC031.3: `ensurePasswordShorterThanSixCharsIsRejected`, `ensurePasswordWithoutDigitIsRejected`, `ensurePasswordWithoutCapitalLetterIsRejected`, `ensureValidPasswordIsAccepted`, `ensureNullPasswordIsRejected`, `ensureEmptyPasswordIsRejected`
- AC031.4: Validated by EAPLI framework role assignment — no isolated unit test needed
- AC031.5: Enforced by `@UniqueConstraint` on the underlying `SystemUser` table
- AC031.6: Covered by `AiSafeBootstrap` which registers the admin user idempotently

---

### 4.2. Acceptance Tests

Authorization (Admin role check) and database uniqueness constraints are infrastructure concerns primarily validated by manual integration testing. The domain-level field validations (email, password) are fully covered by the automated unit tests above.

**Manual test — AC031.1 (full registration flow):**

1. Run `AiSafeBackofficeApp`.
2. Login as `admin`.
3. Navigate to `Users > Register Backoffice User`.
4. Provide: username `jdoe`, password `Password1`, first name `John`, last name `Doe`, phone `912345678`, email `jdoe@company.com`, position `ENGINEER`, security clearance `BASIC`, skills date `2024-01-01`.
5. Expected: confirmation message displayed and user visible in the user list.

**Manual test — AC031.2 (invalid email rejected):**

1. At the `Email` prompt enter `notanemail`.
2. Expected: an error message is shown and the form re-prompts for a valid email address.

**Manual test — AC031.3 (weak password rejected):**

1. At the `Password` prompt enter `abc` (too short, no digit, no capital letter).
2. Expected: the password policy error is shown and the form re-prompts for a compliant password.

**Manual test — AC031.5 (duplicate username rejected):**

1. After registering user `jdoe` above, attempt to register a second user with the same username.
2. Expected: the system rejects the operation with a uniqueness violation message.
