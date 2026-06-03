# US077 — Tests and Coverage

## Scope

US077 covers making a pilot inactive in the authenticated ATCC's company roster, subject to: ATCC role enforcement, company scoping, and the blocking constraint that a pilot with flight plans assigned cannot be deactivated.

## Automated Tests

### `PilotTest` — US077 additions

Location: `src/test/java/aisafe/pilot/domain/PilotTest.java`

**Deactivation behaviour (US077):**
- `ensureDeactivateSetsActiveToFalse`
- `ensureDeactivateAlreadyInactivePilotThrows`

*(Existing test `ensurePilotIsActiveByDefault` already covers the invariant that a new pilot starts active.)*

### `RemovePilotControllerTest`

Location: `src/test/java/aisafe/pilot/application/RemovePilotControllerTest.java`

**Authorization — no session:**
- `ensureAllActivePilotsThrowsWhenNotAuthenticated`
- `ensureDeactivatePilotThrowsWhenNotAuthenticated`

**Authorization — wrong role:**
- `ensureAllActivePilotsThrowsWhenWrongRole`
- `ensureDeactivatePilotThrowsWhenWrongRole`

**allActivePilotsOfCompany():**
- `ensureAllActivePilotsReturnsOnlyActivePilots`

**deactivatePilot() — happy path:**
- `ensureDeactivatePilotSetsActiveToFalse`
- `ensureDeactivatePilotIsPersisted`

**deactivatePilot() — validation:**
- `ensureDeactivatePilotThrowsWhenAlreadyInactive`
- `ensureDeactivatePilotThrowsWhenPilotNotFound`
- `ensureDeactivatePilotThrowsWhenPilotBelongsToOtherCompany`
- `ensureDeactivatePilotThrowsWhenFlightPlansAssigned`

## Coverage by Acceptance Criterion

| AC | Description | Tests |
|---|---|---|
| AC077.1 | Only ATCC role may act | `ensureAllActivePilotsThrowsWhenNotAuthenticated`, `ensureDeactivatePilotThrowsWhenNotAuthenticated`, `ensureAllActivePilotsThrowsWhenWrongRole`, `ensureDeactivatePilotThrowsWhenWrongRole` |
| AC077.2 | Pilot made inactive, not deleted | `ensureDeactivateSetsActiveToFalse` (domain), `ensureDeactivatePilotSetsActiveToFalse`, `ensureDeactivatePilotIsPersisted` |
| AC077.3 | Cannot deactivate pilot with flight plans | `ensureDeactivatePilotThrowsWhenFlightPlansAssigned` |
| AC077.4 | Only own company pilots | `ensureDeactivatePilotThrowsWhenPilotBelongsToOtherCompany` |

**Additional invariants:**
- Already-inactive pilot cannot be deactivated again: `ensureDeactivateAlreadyInactivePilotThrows` (domain), `ensureDeactivatePilotThrowsWhenAlreadyInactive` (controller)
- Non-existent pilot throws: `ensureDeactivatePilotThrowsWhenPilotNotFound`
- Listing returns only active pilots: `ensureAllActivePilotsReturnsOnlyActivePilots`

Total: **2 domain unit tests** (`PilotTest`) + **11 integration tests** (`RemovePilotControllerTest`) = **13 automated tests**.
