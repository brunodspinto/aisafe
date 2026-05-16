# US058 — Tests and Coverage

## Scope

US058 covers removing a certified engine model from an aircraft model's list of certified engines. Three invariants must hold:

1. The engine being removed must currently be certified on the model.
2. At least one engine must remain after the removal (aircraft model cannot have zero engines).
3. The removal is not possible if any aircraft in service uses this model **and** has the engine in its certified-engines list — preventing decertification of an engine that is still flying.

## Automated Tests

### `AircraftModelTest`

Location: `src/test/java/aisafe/aircraftmodel/domain/AircraftModelTest.java`

Domain-level invariants (1) and (2) are owned by the `AircraftModel` aggregate:

- `ensureCanRemoveEngineWhenMoreThanOneExists` — happy path
- `ensureCannotRemoveLastEngine` — invariant (2)
- `ensureCannotRemoveEngineThatIsNotCertified` — invariant (1)
- `ensureCannotRemoveNullEngine` — null guard

### Invariant (3) — aircraft-in-service check

The "aircraft in service" check is enforced by `RemoveEngineFromAircraftModelController.removeEngine` before delegating to the aggregate, via the new repository method `AircraftRepository.existsAircraftUsingModelEngine(model, engine)`.

This is an infrastructure-spanning check (aircraft repository + aircraft model + engine model) and is exercised by the manual acceptance test below.

## Coverage by Acceptance Criterion

- AC058.1 (only certified engines can be removed): `ensureCannotRemoveEngineThatIsNotCertified`
- AC058.2 (cannot remove last engine): `ensureCannotRemoveLastEngine`
- AC058.3 (not possible if aircrafts in service use this model+engine): controller pre-check via `AircraftRepository.existsAircraftUsingModelEngine`; validated by manual test
- AC058.4 (role enforcement — `BACKOFFICE_OPERATOR`): `AuthorizationService.ensureAuthenticatedUserHasAnyOf`; validated by manual test

---

### 4.2. Acceptance Tests

Authorization, the aircraft-in-service repository query, and the engine removal UI flow are validated by manual integration testing. The domain invariants (last-engine protection, non-certified engine rejection) are fully covered by the automated unit tests above.

**Manual test — AC058.1 / AC058.2 (remove certified engine):**

1. Run `AiSafeBackofficeApp` and login as a Backoffice Operator.
2. Navigate to `Aircraft Models > Remove Engine Model from Aircraft Model`.
3. Select an aircraft model that has at least two certified engines and **no aircraft registered with that model**.
4. Select one of the engines to remove.
5. Expected: confirmation message displayed and the removed engine no longer appears in the model's certified engine list.

**Manual test — AC058.2 (last engine removal rejected):**

1. Select an aircraft model that has exactly one certified engine.
2. Attempt to remove that engine.
3. Expected: the system rejects the operation with a message indicating that at least one engine must remain.

**Manual test — AC058.3 (engine still in use by an aircraft cannot be removed):**

1. Register an aircraft of model `X` (via US070 flow).
2. Attempt to remove any engine certified on model `X`.
3. Expected: the system rejects the operation with a message indicating that there are aircraft in service using this model and engine.
4. Decommission the aircraft (or remove all aircraft of model `X`).
5. Repeat the removal — it now succeeds (assuming invariants 1 and 2 are still met).

**Manual test — AC058.4 (role enforcement):**

1. Login as a user without the Backoffice Operator role (e.g. ATCC).
2. Expected: the Remove Engine Model option is not available in the menu.
