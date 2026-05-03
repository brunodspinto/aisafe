# US030 — Tests and Coverage

## Scope

US030 covers authentication and authorization for the AISafe console application. The implementation is split between the console layer and the `aisafe.auth` facade, which delegates to the EAPLI framework underneath.

## Automated Tests

### `AuthenticationContextTest`

Location: `src/test/java/aisafe/auth/AuthenticationContextTest.java`

This class contains 3 lightweight unit tests:

- `ensureHasRoleReturnsFalseForNullRole`
- `ensureHasAnyRoleReturnsFalseForEmptyRoleArray`
- `ensureHasAnyRoleReturnsFalseForNullRoleArray`

Coverage notes:

- Verifies the local guard logic in the AISafe auth facade.
- Keeps the MVP test set small and stable.
- Does not try to bootstrap a full framework session.

### `UnauthorizedExceptionTest`

Location: `src/test/java/aisafe/auth/UnauthorizedExceptionTest.java`

This class contains 3 unit tests:

- `ensureExceptionCanBeCreatedWithMessage`
- `ensureExceptionCanBeCreatedWithCause`
- `ensureExceptionCanBeThrown`

Coverage notes:

- Confirms the custom exception preserves message and cause.
- Confirms the exception is thrown as expected by authorization checks.

## Manual / Integration Coverage

The login, role-gated menu, and logout flow are validated through the console application flow rather than through isolated unit tests.

Covered scenarios:

- Three invalid login attempts deny access.
- ADMIN users see the Users submenu.
- Non-admin users do not see admin-only menu entries.
- Logout clears the active session and returns to the pre-login menu.

## Notes

- No glossary or domain-model changes were required for US030.
- Authentication is handled through EAPLI `AuthzRegistry` and exposed through `aisafe.auth.AuthenticationContext`.
- Authorization checks for protected operations use `aisafe.auth.AuthorizationService`.
