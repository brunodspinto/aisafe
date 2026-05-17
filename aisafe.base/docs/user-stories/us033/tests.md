# US033 — Tests and Coverage

## Scope

US033 lists all backoffice users — both active and inactive — for an authenticated Admin. The use case does not introduce a new domain aggregate; it relies on the `User` aggregate (US031) and the `UserRepository.findAll()` query.

## Automated Tests

The unit test `src/test/java/aisafe/usermanagement/application/ListUsersControllerTest.java` exercises the use case end-to-end against the in-memory repositories:

| Test | Acceptance Criterion |
|---|---|
| `ensureAdminCanListAllUsersIncludingInactiveUsers` | AC033.1 and AC033.2 |
| `ensureNonAdminCannotListUsers` | AC033.3 |

Supporting domain tests live in `src/test/java/aisafe/usermanagement/domain/UserTest.java` and `src/test/java/aisafe/usermanagement/domain/DisableEnableUserTest.java`.

Authorization is enforced by `ListUsersController.allUsers()` calling `authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ADMIN)`.

## Manual / Integration Coverage

The manual scenario exercises the console UI and verifies that the `Users >` submenu is shown only for ADMIN and that the listing displays active and inactive users.

## Coverage by Acceptance Criterion

- **AC033.1** — `ListUsersController.allUsers()` returns the result of `userRepo.findAll()`, which includes inactive users. Verified by `DisableEnableUserTest`.
- **AC033.2** — The fields shown in the listing (username, status, first name, last name, email, position) are exercised by `UserTest`.
- **AC033.3** — Role check on the controller method; the `Users >` submenu in `MainMenu` is gated behind `AiSafeRoles.ADMIN`.

---

### Manual Acceptance Tests

**Manual test — AC033.1 / AC033.2 (list all users with full details):**

1. Run `AiSafeConsoleApp` and login as `admin` (role: ADMIN).
2. Navigate to `Users > 2 — List Users`.
3. Expected: a table is displayed showing all registered users (active and inactive) with columns for username, status (ACTIVE / INACTIVE), first name, last name, email, and position.

**Manual test — AC033.3 (only Administrator can list users):**

1. Login as a non-admin user (e.g., `operator` with BACKOFFICE_OPERATOR role).
2. Expected: the `Users >` submenu is not available in the main menu.
