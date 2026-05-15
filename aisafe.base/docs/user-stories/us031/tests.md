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
