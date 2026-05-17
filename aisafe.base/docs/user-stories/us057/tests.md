# US057 — Tests and Coverage

## Scope

US057 covers adding a certified engine model to an existing aircraft model, including engine type compatibility enforcement and duplicate prevention.

## Automated Tests

### `AircraftModelTest`

Location: `src/test/java/aisafe/aircraftmodel/domain/AircraftModelTest.java`

- `ensureFirstEngineCannotBeNull`
- `ensureCannotAddNullEngineModel`
- `ensureCannotAddIncompatibleEngineType`
- `ensureCanAddCompatibleEngineType`
- `ensureCannotAddDuplicateEngineModel`
- `ensureCertifiedEnginesIsUnmodifiable`
- `ensureCanRemoveEngineWhenMoreThanOneExists`
- `ensureCannotRemoveLastEngine`
- `ensureCannotRemoveNullEngine`
- `ensureCannotRemoveEngineThatIsNotCertified`

## Coverage by Acceptance Criterion

- AC057.1: `ensureCanAddCompatibleEngineType`, `ensureCannotAddIncompatibleEngineType`, `ensureFirstEngineCannotBeNull`
- AC057.2: `ensureCannotAddDuplicateEngineModel`, `ensureCannotAddNullEngineModel`, `ensureCertifiedEnginesIsUnmodifiable`
- AC057.3: Optimistic Locking enforced by `@Version` em `AircraftModel`; validado por teste de aceitação manual
- AC057.4: Controller verifica role `BACKOFFICE_OPERATOR` via `AuthorizationService`; validado por teste de aceitação manual

---

### 4.2. Acceptance Tests

Authorization (`BACKOFFICE_OPERATOR` role), Optimistic Locking e persistência são infraestrutura validada por testes manuais. As regras de compatibilidade e duplicação de engines estão cobertas pelos testes unitários acima.

**Manual test — AC057.1 / AC057.2 (full add engine flow):**

1. Run `AiSafeBackofficeApp` and login as a Backoffice Operator.
2. Navigate to `Aircraft Configuration > Add Engine to Aircraft Model`.
3. Select an existing Aircraft Model (e.g. `737-800`).
4. Select a compatible Engine Model (e.g. `CFM56`).
5. Expected: confirmation message `Engine 'CFM56' successfully added to aircraft model '737-800'.`

**Manual test — AC057.1 (incompatible engine type rejected):**

1. Select a TURBOFAN-based aircraft model and attempt to add a TURBOPROP engine.
2. Expected: the system rejects the operation with a compatibility error.

**Manual test — AC057.2 (duplicate engine rejected):**

1. Attempt to add the same engine model a second time to the same aircraft model.
2. Expected: the system rejects the operation with a duplicate error.

**Manual test — AC057.3 (Optimistic Locking):**

1. Open two terminal sessions both logged in as Backoffice Operator.
2. In Terminal 1, start adding an engine to a model but do not confirm yet.
3. In Terminal 2, add a different engine to the same model and confirm.
4. Return to Terminal 1 and confirm.
5. Expected: Terminal 1 receives a concurrency error indicating the model was already modified.

**Manual test — AC057.4 (role enforcement):**

1. Login as a user without the `BACKOFFICE_OPERATOR` role.
2. Expected: the Add Engine option is not available in the menu.

**Manual test — remove engine (full removal flow):**

1. Run `AiSafeBackofficeApp` and login as a Backoffice Operator.
2. Navigate to `Aircraft Configuration > Remove Engine from Aircraft Model`.
3. Select an aircraft model with more than one certified engine (e.g. `737-800` with `CFM56` and `GE90`).
4. Select the engine to remove (e.g. `GE90`).
5. Expected: confirmation message displayed. The model retains at least one certified engine.

**Manual test — remove last engine rejected:**

1. Select an aircraft model with exactly one certified engine.
2. Attempt to remove that engine.
3. Expected: the system rejects the operation with a message indicating the model must retain at least one certified engine.
