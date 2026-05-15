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
