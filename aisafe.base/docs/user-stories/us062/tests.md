# US062 — Tests and Coverage

## Scope

US062 covers listing active collaborators of a customer (Air Transport Company or Air Control Area), filtering out collaborators whose system user is inactive.

## Automated Tests

The listing logic is validated through repository filtering and controller behaviour. No isolated domain unit tests exist for this US as it is a query operation with no domain invariants.

### Repository filtering

The `CollaboratorRepository` implementations (`InMemoryCollaboratorRepository` and `JpaCollaboratorRepository`) filter collaborators where `systemUser.active = true`:

- `findActiveByAirTransportCompany(IATACode)` — returns only active collaborators for a company
- `findActiveByAirControlArea(AirControlAreaCode)` — returns only active collaborators for an area

## Coverage by Acceptance Criterion

- AC062.1: UI prompts for customer type selection (Air Transport Company or Air Control Area)
- AC062.2: UI loads and displays available customers before asking for selection
- AC062.3: Repository methods filter on `systemUser.active = true`
- AC062.4: Controller returns empty result and UI displays informative message when no active collaborators exist
- AC062.5: Controller checks `BACKOFFICE_OPERATOR` or `ADMIN` role via `AuthorizationService`

---

### 4.2. Acceptance Tests

Authorization and repository-level filtering are infrastructure concerns validated by manual integration testing. No isolated domain unit tests exist for this US as it is a query-only operation.

**Manual test — AC062.1 / AC062.2 / AC062.3 (list company collaborators):**

1. Run `AiSafeBackofficeApp` and login as a Backoffice Operator or Admin.
2. Navigate to `Collaborators > List Collaborators`.
3. Select `Air Transport Company` and choose an existing company (e.g. `TP`).
4. Expected: list of all active collaborators for `TP` is displayed, each showing username and contact info.

**Manual test — AC062.3 (disabled collaborator not listed):**

1. Disable a collaborator of company `TP` via US064.
2. Navigate to `Collaborators > List Collaborators` and select `TP` again.
3. Expected: the disabled collaborator no longer appears in the list.

**Manual test — AC062.4 (empty list message):**

1. Select a customer that has no active collaborators registered.
2. Expected: an informative message such as `No active collaborators found for this customer.` is displayed instead of an empty list.

**Manual test — AC062.5 (role enforcement):**

1. Login as a user without Backoffice Operator or Admin role (e.g. ATCC).
2. Expected: the List Collaborators option is not available in the menu.
