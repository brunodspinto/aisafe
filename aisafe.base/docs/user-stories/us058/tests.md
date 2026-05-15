# US058 — Tests and Coverage

## Scope

US058 covers removing a certified engine model from an aircraft model, with the constraint that at least one engine must remain and the engine must currently be certified on the model.

## Automated Tests

### `AircraftModelTest`

Location: `src/test/java/aisafe/aircraftmodel/domain/AircraftModelTest.java`

- `ensureCanRemoveEngineWhenMoreThanOneExists`
- `ensureCannotRemoveLastEngine`
- `ensureCannotRemoveEngineThatIsNotCertified`
- `ensureCannotRemoveNullEngine`

## Coverage by Acceptance Criterion

- AC058.1: `ensureCannotRemoveEngineThatIsNotCertified`
- AC058.2: `ensureCannotRemoveLastEngine`
- AC058.3: Controller checks `BACKOFFICE_OPERATOR` role via `AuthorizationService`
