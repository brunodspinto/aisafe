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

---

### 4.2. Acceptance Tests

Authorization (Backoffice Operator role) and the engine removal flow are validated by manual integration testing. The domain invariants (last engine protection, non-certified engine rejection) are fully covered by the automated unit tests above.

**Manual test — AC058.1 (remove certified engine):**

1. Run `AiSafeBackofficeApp` and login as a Backoffice Operator.
2. Navigate to `Aircraft Models > Remove Engine Model from Aircraft Model`.
3. Select an aircraft model that has at least two certified engines.
4. Select one of the engines to remove.
5. Expected: confirmation message displayed and the removed engine no longer appears in the model's certified engine list.

**Manual test — AC058.2 (last engine removal rejected):**

1. Select an aircraft model that has exactly one certified engine.
2. Attempt to remove that engine.
3. Expected: the system rejects the operation with a message indicating that at least one engine must remain.

**Manual test — AC058.3 (role enforcement):**

1. Login as a user without the Backoffice Operator role (e.g. ATCC).
2. Expected: the Remove Engine Model option is not available in the menu.
