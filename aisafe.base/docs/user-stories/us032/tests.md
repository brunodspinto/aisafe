# US032 — Tests and Coverage

## Scope

US032 covers disabling and re-enabling backoffice users from the administrator console flow. The implementation reuses the existing `User` aggregate and the EAPLI `SystemUser` active flag.

## Automated Tests

### `DisableEnableUserTest`

Location: `src/test/java/aisafe/usermanagement/domain/DisableEnableUserTest.java`

This class contains 8 unit tests covering the acceptance criteria:

- `ensureNewUserIsActiveByDefault`
- `ensureActiveUserCanBeDeactivated`
- `ensureInactiveUserCanBeReactivated`
- `ensureReactivatedUserIsFullyActive`
- `ensureDeactivatingAlreadyInactiveUserThrows`
- `ensureActivatingAlreadyActiveUserIsIdempotent`
- `ensureUserAggregateReflectsDeactivatedState`
- `ensureUserAggregateReflectsReactivatedState`

## Coverage by Acceptance Criterion

- AC032.1: `ensureActiveUserCanBeDeactivated`
- AC032.2: `ensureInactiveUserCanBeReactivated` and `ensureReactivatedUserIsFullyActive`
- AC032.3: `ensureUserAggregateReflectsDeactivatedState` and `ensureUserAggregateReflectsReactivatedState`
- AC032.4: The controller checks `AuthzRegistry.authorizationService().ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ADMIN)` before listing or toggling users.
- AC032.5: `ensureDeactivatingAlreadyInactiveUserThrows` and `ensureActivatingAlreadyActiveUserIsIdempotent`

## Notes

- The active/inactive state is owned by EAPLI `SystemUser`, not by a new AISafe entity.
- The console UI shows the current ACTIVE/DISABLED status before the administrator performs the toggle.

---

### 4.2. Acceptance Tests

The toggle logic (activate/deactivate) is fully covered by the automated unit tests above. Authorization (Admin role check) and the end-to-end UI flow are infrastructure concerns primarily validated by manual integration testing.

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

**Manual test — AC032.5 (redundant action handled gracefully):**

1. Attempt to disable a user that is already `DISABLED` (if the UI allows selecting disabled users).
2. Expected: the system either prevents the selection or displays an informative message indicating no change was made.

**Manual test — AC032.4 (role enforcement):**

1. Login as a non-admin user (e.g. Backoffice Operator).
2. Expected: the Disable/Enable User option is not available in the menu.
