# US076 — List Company Pilot Roster: Tests

## 1. Scope

This document describes the tests for US076. It covers:

- **Acceptance tests** — one per acceptance criterion, verifying the observable behaviour expected by the ATCC.
- **Automated unit tests** — added in Phase 4 (TDD), exercising `ListPilotRosterController` filtering logic in isolation.

Tests do **not** cover persistence (JPA/H2) — that is validated by manual demonstration.

---

## 2. Acceptance Test Table

| Test ID | Acceptance Criterion | Scenario | Expected Result |
|---------|----------------------|----------|-----------------|
| AT076.1 | AC076.1 — All pilots | Company has two pilots: one active, one inactive | Both pilots appear in the listing |
| AT076.2 | AC076.2 — Active only | One active pilot and one inactive pilot exist for the company | Only the active pilot is returned |
| AT076.3 | AC076.3 — By model name (exact case) | Pilot 1 certified for "A320", Pilot 2 not certified; filter = "A320" | Only Pilot 1 returned |
| AT076.3b | AC076.3 — By model name (case-insensitive) | Same setup; filter = "a320" (lowercase) | Same result as AT076.3 |
| AT076.4 | AC076.4 — Authorization | A non-ATCC user attempts to invoke the listing | `IllegalStateException` thrown; listing not displayed |
| AT076.5 | AC076.5 — Empty result | Active-only filter; company has no active pilots | Message displayed: `"No pilots found for the selected filter."` |

---

## 3. Coverage by Acceptance Criterion

| Acceptance Criterion | Satisfied by (class · method) |
|----------------------|-------------------------------|
| AC076.1 — All pilots | `ListPilotRosterController.allPilots()` — returns the full list from `PilotRepository.findByAirTransportCompany(company)` |
| AC076.2 — Active only | `ListPilotRosterController.activePilots()` — delegates to `allPilots()` then filters by `Pilot.isActive()` |
| AC076.3 — By model name (case-insensitive) | `ListPilotRosterController.pilotsByCertifiedModel(String)` — resolves matching model IDs via `AircraftModelRepository.findAll()` with `equalsIgnoreCase`, then filters by `Pilot.isCertifiedFor(Long)` |
| AC076.4 — Authorization | `ListPilotRosterController.allPilots()` / `activePilots()` / `pilotsByCertifiedModel()` — each calls `authz.ensureAuthenticatedUserHasAnyOf(ATCC)` before proceeding |
| AC076.5 — Empty result message | `ListPilotRosterUI` — detects an empty result list and prints `"No pilots found for the selected filter."` |

---

## 4. Manual Test Steps

### AT076.1 — All pilots

1. Log in as `atcc1` (ATCC role).
2. Navigate to **Pilots → List Pilot Roster**.
3. Select filter **1 — All pilots**.
4. Verify both active and inactive pilots belonging to `atcc1`'s company appear in the table.

### AT076.2 — Active pilots only

1. Log in as `atcc1`.
2. Navigate to **Pilots → List Pilot Roster**.
3. Select filter **2 — Active pilots only**.
4. Verify only pilots with status ACTIVE are listed; inactive pilots are absent.

### AT076.3 — By certified aircraft model name (exact case)

1. Log in as `atcc1`.
2. Navigate to **Pilots → List Pilot Roster**.
3. Select filter **3 — By certified aircraft model name**.
4. Enter model name `A320`.
5. Verify only pilots certified for "A320" are listed; pilots not certified for "A320" are absent.

### AT076.3b — By certified aircraft model name (case-insensitive)

1. Repeat AT076.3 but enter model name `a320` (all lowercase).
2. Verify the result is identical to AT076.3 (case-insensitive string comparison).

### AT076.4 — Authorization enforcement

1. Log in as a user without the ATCC role.
2. Verify the **List Pilot Roster** menu item is either absent or, if invoked directly, results in an authorization error.

### AT076.5 — Empty result message

1. Log in as `atcc1` whose company has no active pilots.
2. Navigate to **Pilots → List Pilot Roster**.
3. Select filter **2 — Active pilots only**.
4. Verify the message `"No pilots found for the selected filter."` is displayed.
