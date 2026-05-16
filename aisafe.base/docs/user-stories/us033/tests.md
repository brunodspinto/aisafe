# US033 — Acceptance Tests

## AC033.1 / AC033.2 — List all users with full details

**Manual test:**

1. Run `AiSafeConsoleApp` and login as `admin` (role: ADMIN).
2. Navigate to `Users > 2 — List Users`.
3. Expected: a table is displayed showing all registered users (active and inactive) with columns for username, status (ACTIVE / INACTIVE), first name, last name, email, and position.

## AC033.3 — Only Administrator can list users

**Manual test:**

1. Login as a non-admin user (e.g., `operator` with BACKOFFICE_OPERATOR role).
2. Expected: the `Users >` submenu is not available in the main menu.
