# US071 — Tests and Coverage

## Scope

US071 covers decommissioning an aircraft from an Air Transport Company's fleet. The operation is permanent and irreversible: once an aircraft is decommissioned it cannot be reactivated. Only active aircraft are eligible.

## Automated Tests

### `AircraftTest`

Location: `src/test/java/aisafe/aircraft/domain/AircraftTest.java`

- `ensureAircraftIsCreatedWithActiveOperationalStatus`
- `ensureDecommissionChangesStatus`
- `ensureCannotDecommissionAlreadyDecommissioned`
- `ensureIsActiveReturnsTrueForActiveAircraft`
- `ensureIsActiveReturnsFalseAfterDecommission`

## Coverage by Acceptance Criterion

- AC071.1 / AC071.5: `ensureIsActiveReturnsFalseAfterDecommission`; UI only lists active aircraft (manual test)
- AC071.2: Manual test — confirmation step required before operation completes
- AC071.3: `ensureDecommissionChangesStatus`; status persisted as `DECOMMISSIONED` after confirmation (manual test)
- AC071.4: Manual test — role enforcement (non-ATCC user cannot access the option)

---

### 4.2. Acceptance Tests

Authorization and persistence are infrastructure concerns validated by manual integration testing. Decommission domain rules are covered by the automated unit tests above.

**Manual test — AC071.3 / AC071.2 (full decommission flow):**

1. Run `AiSafeBackofficeApp` and login as an Air Transport Company Collaborator (`atcc` / `Password1`).
2. Navigate to `Fleet Management > Decommission Aircraft`.
3. Select a company (e.g. `TAP`).
4. The system lists only active aircraft. Select one (e.g. `CS-TUG`).
5. The system asks: `"Are you sure you want to decommission CS-TUG? (yes/no)"`. Answer `yes`.
6. Expected: confirmation message displayed — `Aircraft successfully decommissioned. Registration: CS-TUG. Status: DECOMMISSIONED.`
7. Navigate back to `Fleet Management > List Fleet`. Confirm `CS-TUG` is no longer listed (or shows as decommissioned, depending on list filter).

**Manual test — AC071.1 / AC071.5 (decommissioned aircraft not selectable):**

1. With `CS-TUG` already decommissioned (from the previous test), navigate again to `Fleet Management > Decommission Aircraft` and select the same company.
2. Expected: `CS-TUG` does not appear in the list. The UI only shows aircraft with status `ACTIVE`, so a decommissioned aircraft can never be selected again.

**Manual test — AC071.2 (cancelling at confirmation aborts the operation):**

1. Navigate to `Fleet Management > Decommission Aircraft` and select an active aircraft.
2. When prompted `"Are you sure you want to decommission <reg>? (yes/no)"`, answer `no`.
3. Expected: the operation is cancelled, no status change occurs, and the aircraft remains active.

**Manual test — AC071.4 (role enforcement):**

1. Login as a user without the `ATCC` or `ADMIN` role (e.g. a Weather Person).
2. Expected: the `Decommission Aircraft` option is not available in the menu.
