# US121 - Tests and Coverage

## Automated Tests

### `CreateFlightPlanFromFileControllerTest`

Location: `src/test/java/aisafe/flightplan/application/CreateFlightPlanFromFileControllerTest.java`

| Test | Acceptance Criteria | Expected Result |
|------|---------------------|-----------------|
| `ensureValidDslFileCreatesDraftFlightPlan` | AC121.1, AC121.2, AC121.4 | A valid `.dsl` file is parsed, converted to a `FlightPlan`, saved, and created in `DRAFT` status. |
| `ensureInvalidLexicalFileIsRejectedAndNotPersisted` | AC121.2, AC121.3, AC121.4 | Lexically invalid DSL produces a meaningful error and no plan is saved. |
| `ensureInvalidSyntacticFileIsRejectedAndNotPersisted` | AC121.2, AC121.3, AC121.4 | Syntactically invalid DSL produces a meaningful error and no plan is saved. |
| `ensureInvalidSemanticFileIsRejectedAndNotPersisted` | AC121.2, AC121.3, AC121.4 | Semantically invalid DSL produces a meaningful error and no plan is saved. |
| `ensureDuplicateDesignatorIsRejected` | AC121.4 | A file whose designator already exists is rejected and no duplicate is persisted. |
| `ensureUnsupportedFileExtensionIsRejectedBeforeImport` | AC121.1 | Files outside `.dsl` are rejected before import. |
| `ensureAuthorizationFailureStopsImportFlow` | AC121.5 | Authorization failure stops the import before parsing or persistence. |

### Reused US120 DSL Tests

US121 relies on the US120 parser pipeline. These tests remain part of the acceptance coverage:

| Test Class | Coverage |
|------------|----------|
| `FlightPlanDslFileTest` | Valid and invalid files across lexical, syntactic and semantic buckets. |
| `FlightPlanSemanticValidatorTest` | Cross-field semantic rules such as fuel, coordinates, route continuity and chronology. |
| `FlightPlanAstBuilderVisitorTest` | AST construction and unit conversion. |

## Commands

```bash
mvn test -Dtest=CreateFlightPlanFromFileControllerTest
mvn test -Dtest=FlightPlanDslFileTest,FlightPlanSemanticValidatorTest,FlightPlanAstBuilderVisitorTest
mvn test
```

## Execution Notes

- `mvn test -Dtest=CreateFlightPlanFromFileControllerTest` succeeds with 7 tests run.
- `mvn test -Dtest=CreateFlightPlanFromFileControllerTest,FlightPlanDslFileTest,FlightPlanSemanticValidatorTest,FlightPlanAstBuilderVisitorTest` succeeds with 41 tests run.
- `mvn test` succeeds with 753 tests run, 0 failures, 0 errors, and 0 skipped.

## Manual Scenario

1. Login as a Pilot.
2. Open **Flight Plans > Create Flight Plan from DSL File**.
3. Provide `src/test/resources/dsl/valid/01_single_leg_regular.dsl`.
4. Confirm the system creates flight plan `TP123` in `DRAFT` status.
5. Repeat with `src/test/resources/dsl/invalid/syntactic/01_missing_semicolon.dsl`.
6. Confirm the system displays a line/column validation error and does not create a plan.
