# US032 — Tests and Coverage

## Scope

US032 covers disabling and re-enabling backoffice users from the administrator console flow. The implementation reuses the existing `User` aggregate and the EAPLI `SystemUser` active flag.

## Automated Tests

### `UserTest`

Location: `src/test/java/aisafe/usermanagement/domain/UserTest.java`

The AISafe-side coverage of US032 is the `User` aggregate's identity, equality and contract (used by `DisableEnableUserController.findByUsername`):

- `ensureUsersWithSameMecanographicNumberAreEqual`
- `ensureUserConstructorRejectsNullSystemUser`
- `ensureIdentityReturnsMecanographicNumber`

### `DisableEnableUserTest`

Location: `src/test/java/aisafe/usermanagement/domain/DisableEnableUserTest.java`

- `ensureNewSystemUserStartsActive`
- `ensureActiveUserCanBeDeactivated`
- `ensureInactiveUserCanBeReactivated`
- `ensureReactivatedUserIsFullyActive`
- `ensureDeactivatingAlreadyInactiveUserThrows`
- `ensureActivatingAlreadyActiveUserIsIdempotent`
- `ensureUserAggregateReflectsDeactivatedState`
- `ensureMultipleToggleCyclesPreserveState`

### EAPLI framework

The actual activate/deactivate toggle logic is owned by `eapli.framework.infrastructure.authz.application.UserManagementService` (`activateUser` / `deactivateUser`) and is tested by the framework itself.

## Coverage by Acceptance Criterion

- AC032.1 (deactivate active user): `DisableEnableUserController.toggleUser` reads `systemUser.isActive()` and calls `userSvc.deactivateUser(systemUser)`; framework guarantees the active flag is flipped.
- AC032.2 (reactivate inactive user): Same path; calls `userSvc.activateUser(systemUser)` when `isActive()` is false.
- AC032.3 (UI shows current state): `DisableEnableUserUI.allUsers()` lists users and displays their current active flag before the operator chooses.
- AC032.4 (role enforcement): Controller calls `authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ADMIN)` on every public method; throws `UnauthorizedException` otherwise — validated by manual test below.
- AC032.5 (idempotent / no-op handling): Framework's `activateUser`/`deactivateUser` are idempotent on the already-target state; not exercised because the UI only offers the opposite of the current state.

## Notes

- The active/inactive state is owned by EAPLI `SystemUser`, not by a new AISafe entity.
- The console UI shows the current ACTIVE/DISABLED status before the administrator performs the toggle.

---

### 4.2. Acceptance Tests

Authorization (Admin role check), the EAPLI toggle, and the end-to-end UI flow are infrastructure concerns primarily validated by manual integration testing.

**Manual test — AC032.1 / AC032.3 (disable active user):**

1. Run `AiSafeBackofficeApp` and login as `admin`.
2. Navigate to `Users > Disable/Enable User`.
3. Expected: list of users is displayed, each showing current status (`ACTIVE` or `DISABLED`).
4. Select an `ACTIVE` user and confirm the action.
5. Expected: confirmation message displayed; the user's status changes to `DISABLED` in the list.

**Manual test — AC032.2 / AC032.3 (re-enable disabled user):**

1. From the same menu, select a `DISABLED` user.
2. Confirm the action.
3. Expected: confirmation message displayed; the user's status changes back to `ACTIVE` in the list.

**Manual test — AC032.4 (role enforcement):**

1. Login as a non-admin user (e.g. Backoffice Operator).
2. Expected: the Disable/Enable User option is not available in the menu.

**Manual test — AC032.5 (toggle round-trip):**

1. Disable an active user.
2. Re-enable the same user immediately.
3. Expected: both operations succeed and the user ends up `ACTIVE` again.
