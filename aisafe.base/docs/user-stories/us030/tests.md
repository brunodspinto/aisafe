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

---

### 4.2. Acceptance Tests

Authentication and authorization are infrastructure concerns and are primarily validated by manual integration testing. The `AddUserController` unit-level verification of the authorization check is covered indirectly by the US031 test suite: the controller calls `authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ADMIN)`, which throws `UnauthorizedException` when the session lacks the required role (AC030.6).

Detailed unit-test coverage is documented in [tests.md](tests.md).

**Manual test — AC030.2 (max attempts):**

1. Run `AiSafeConsoleApp`.
2. Enter wrong credentials 3 times.
3. Expected: `Unable to authenticate. Please contact your administrator.` and application exits.

**Manual test — AC030.4 (role-based menu):**

1. Login as `admin` (role: ADMIN).
2. Expected: Main menu shows `1 — My Account >` and `2 — Users >`.
3. Logout, then login as a non-admin user.
4. Expected: Main menu shows only `1 — My Account >`.

**Manual test — AC030.5 (logout):**

1. Login as `admin`.
2. Select `1 — My Account > 1 — Logout`.
3. Expected: Session is cleared and the following menu is shown:
   ```
   1 - Login again
   0 - Exit
   ```
   Choosing `1` restarts the login flow; choosing `0` exits the application.
