# US033 — Tests and Coverage

## Scope

US033 lists all backoffice users — both active and inactive — for an authenticated Admin. The use case does not introduce a new domain aggregate; it relies on the `User` aggregate (US031) and the `UserRepository.findAll()` query.

## Automated tests

The unit tests live in `src/test/java/aisafe/usermanagement/domain/UserTest.java` and indirectly exercise the data exposed by `ListUsersUI`:

| Test | Acceptance Criterion |
|---|---|
| `ensureUserCanBeCreatedWithAllFields` | AC033.1 (all fields present in list) |
| `ensureUserSkillsAssessmentDateGetterReturnsCorrectValue` | AC033.2 (skills assessment shown) |
| `ensureSecurityClearanceToStringContainsExpirationDate` | AC033.2 (clearance display) |
| `ensureSecurityLevelLowCodeIsZero` / `ensureSecurityLevelCriticalCodeIsFour` | AC033.2 (level codes) |
| `ensureUsersWithSameMecanographicNumberAreEqual` | AC033.1 (identity preserved across active/inactive) |
| `ensureUserAggregateReflectsDeactivatedState` (in `DisableEnableUserTest`) | AC033.1 (disabled users still listable) |

Authorization (AC033.3) is enforced by `ListUsersController.listAllUsers()` calling `authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ADMIN)`. This is exercised by the EAPLI authorization layer; no dedicated unit test is added.

## Coverage by Acceptance Criterion

- **AC033.1** — `ListUsersController.listAllUsers()` returns the result of `userRepo.findAll()`, which includes inactive users. Verified by `DisableEnableUserTest.ensureUserAggregateReflectsDeactivatedState` (a deactivated user remains queryable through its `SystemUser`).
- **AC033.2** — Every field shown in the listing (username, status, first name, last name, email, position) has its getter exercised by `UserTest` and `SecurityClearanceTest`.
- **AC033.3** — Role check on the controller method; the `Users >` submenu in `MainMenu` is gated behind `AiSafeRoles.ADMIN`.

## Manual tests

**Manual test — AC033.1 / AC033.2 (list all users with full details):**

1. Run `AiSafeConsoleApp` and login as `admin` (role: ADMIN).
2. Navigate to `Users > 2 — List Users`.
3. Expected: a table is displayed showing all registered users (active and inactive) with columns for username, status (ACTIVE / INACTIVE), first name, last name, email, and position.

**Manual test — AC033.3 (only Administrator can list users):**

1. Login as a non-admin user (e.g., `operator` with BACKOFFICE_OPERATOR role).
2. Expected: the `Users >` submenu is not available in the main menu.
