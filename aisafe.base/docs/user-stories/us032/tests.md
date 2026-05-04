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
