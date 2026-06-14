# US081 — Create Flight Plan from File

## 1. Requirements

**US081:** As a Pilot, I want to create a flight plan from a file.

**Acceptance criteria:**

| AC | Description |
|----|-------------|
| AC081.1 | The file must conform to the Core Flight DSL specification (US083). |
| AC081.2 | The file must be validated through lexical, syntactic, range and semantic analysis. |
| AC081.3 | Invalid files must produce meaningful error messages with line and column. |
| AC081.4 | Only valid flight plans may be imported and stored. |
| AC081.5 | A successfully imported flight plan is created with status `DRAFT`. |

---

## 2. Analysis

### Validation pipeline

```
Stage 1 — Lexical + Syntactic   (ANTLR Lexer + Parser)
Stage 2 — Range validation       (FlightPlanValidationListener)
Stage 3 — AST construction       (FlightPlanAstBuilderVisitor)
Stage 4 — Semantic validation    (FlightPlanSemanticValidator)
```

Each stage only runs if the previous produced no errors.

### Semantic rules

#### Range rules — Stage 2 (Listener)

| Field | Valid range |
|-------|------------|
| Wind direction | 0 – 359 degrees |
| Latitude | −90 .. +90 |
| Longitude | −180 .. +180 |

#### Cross-field rules — Stage 4 (Validator)

| Rule | Description |
|------|-------------|
| Fuel > 0 | Each leg's fuel must be strictly positive |
| At least one segment | Each leg must have at least one segment |
| Coordinates differ | START and END of a segment cannot be equal |
| Altitude > 0 | Altitude must be positive |
| Width > 0 | Width must be positive |
| Wind speed ≥ 0 | Wind speed cannot be negative |
| Leg airport sequence | Arrival of leg N must match departure of leg N+1 |
| Leg time sequence | Arrival time of leg N must precede departure of leg N+1 |
| Leg route sequence | Destination of leg N must match origin of leg N+1 |
| No airport twice | The same airport cannot appear more than once |
| Valid date/time | All dates and times must be valid calendar values |

All errors are collected in a **single pass** — validation never stops at the first error.

---

## 3. Design

### Sequence

```
Pilot enters file path
        │
        ▼
CreateFlightPlanFromFileUI
        │
        ▼
CreateFlightPlanFromFileController
        │
        ├─► FlightPlanParserFacade.parse(dsl)
        │         ├─ Stage 1: ANTLR lexer + parser
        │         ├─ Stage 2: FlightPlanValidationListener
        │         ├─ Stage 3: FlightPlanAstBuilderVisitor
        │         └─ Stage 4: FlightPlanSemanticValidator
        │
        │  if valid:
        ├─► FlightPlan.fromDsl(ast, dslContent)   → DRAFT
        └─► FlightPlanRepository.save(plan)
```

![Sequence Diagram](svg/US081-SD.svg)

![Class Diagram](svg/US081-class-diagram.svg)

### Key classes

| Class | Role |
|-------|------|
| `CreateFlightPlanFromFileUI` | Console UI — reads file path from the user |
| `CreateFlightPlanFromFileController` | Use-case orchestrator |
| `FlightPlanParserFacade` | 4-stage parse pipeline |
| `FlightPlanValidationListener` | Range validation (Listener) |
| `FlightPlanAstBuilderVisitor` | AST construction (Visitor) |
| `FlightPlanSemanticValidator` | Cross-field semantic checks |
| `FlightPlan` | Aggregate root |

---

## 4. How to run

```bash
cd aisafe.base

# Run all tests
mvn test

# Run only DSL acceptance tests
mvn test -Dtest=FlightPlanDslFileTest

# Run only semantic validator unit tests
mvn test -Dtest=FlightPlanSemanticValidatorTest
```

### Application (console)

```bash
mvn exec:java
```

1. Login as Pilot.
2. Select **Flight Plans → Create Flight Plan from DSL File**.
3. Enter the full path to the `.dsl` file.

**Success:**
```
Flight plan successfully created!
  Designator : TP123
  Type       : REGULAR
  Status     : DRAFT
```

**Failure (syntax error):**
```
Flight plan is invalid:
  [line 6, col 4] missing ';' at 'ARRIVAL'
```

**Failure (semantic error):**
```
Flight plan is invalid:
  Leg 1: fuel quantity must be strictly positive (got -5300.00).
```

---

## 5. Acceptance tests

### AC081.5 — Flight plan created in DRAFT status

```java
@Test
void ensureStatusStartsAsDraft() {
    final FlightPlan plan = new FlightPlan(
            FlightPlanDesignator.valueOf("TP1234"), "REGULAR", "content");
    assertEquals(FlightPlanStatus.DRAFT, plan.status());
}
```

### AC081.4 — Valid plan produces no errors

```java
@Test
void ensureValidPlanProducesNoErrors() {
    assertTrue(validator.validate(validPlan()).isEmpty());
}
```

### AC081.2 + AC081.3 — Negative fuel rejected with message

```java
@Test
void ensureNegativeFuelProducesError() {
    final LegAst leg = new LegAst(..., new FuelAst(-100.0, "KG"));
    final List<ParseError> errors = validator.validate(
            new FlightPlanAst("TP1234", FlightType.REGULAR, List.of(leg)));
    assertFalse(errors.isEmpty());
    assertTrue(errors.stream().anyMatch(e -> e.message().contains("fuel")));
}
```

### AC081.2 + AC081.3 — Identical segment coordinates rejected

```java
@Test
void ensureSegmentWithSameStartAndEndProducesError() {
    final SegmentAst seg = new SegmentAst(
            new CoordinateAst(38.7, -9.1),
            new CoordinateAst(38.7, -9.1),   // same as start
            10000.0, 50.0, 10.0, 270.0);
    // ...
    assertTrue(errors.stream().anyMatch(e -> e.message().contains("coordinates")));
}
```

### AC081.2 + AC081.3 — Leg airport sequence enforced

```java
@Test
void ensureLegArrivalMustMatchNextLegDeparture() {
    final LegAst leg1 = validLeg("LIS", "OPO");
    final LegAst leg2 = validLeg("FAO", "MAD");   // FAO ≠ OPO
    final List<ParseError> errors = validator.validate(
            new FlightPlanAst("TP1234", FlightType.REGULAR, List.of(leg1, leg2)));
    assertTrue(errors.stream().anyMatch(e -> e.message().contains("arrival airport")));
}
```

### AC081.3 — All errors collected in a single pass

```java
@Test
void ensureAllErrorsAreCollectedInSingleExecution() {
    // plan with: same coordinates + negative altitude + negative width + negative fuel
    final List<ParseError> errors = validator.validate(plan);
    assertTrue(errors.size() >= 3);
}
```

### DSL file acceptance tests (parameterised)

```java
@ParameterizedTest(name = "[valid] {0}")
@MethodSource("validFiles")
void ensureValidDslFileIsAccepted(final Path file) throws IOException {
    final ParseResult result = new FlightPlanParserFacade().parse(Files.readString(file));
    assertTrue(result.isValid(), "Expected VALID but got: " + result.errors());
}

@ParameterizedTest(name = "[invalid/semantic] {0}")
@MethodSource("invalidSemanticFiles")
void ensureInvalidSemanticDslFileIsRejected(final Path file) throws IOException {
    final ParseResult result = new FlightPlanParserFacade().parse(Files.readString(file));
    assertFalse(result.isValid());
}
```

---

## 6. Test results

```
[FlightPlanSemanticValidatorTest]   Tests run: 14, Failures: 0, Errors: 0
[FlightPlanDslFileTest]             Tests run: 13, Failures: 0, Errors: 0
[FlightPlanTest]                    Tests run: 19, Failures: 0, Errors: 0
──────────────────────────────────────────────────────────────────────────
Total                               Tests run: 354, Failures: 0, Errors: 0
BUILD SUCCESS
```

---

## 7. Observations

- `FlightPlanValidationListener` and `FlightPlanSemanticValidator` cover different stages — there is no duplication of rules between them.
- The controller is not unit-tested due to its dependency on the authentication framework and repository, consistent with the approach used for all controllers in the project.
- Designator uniqueness is enforced by the controller before persisting; duplicate designators produce a user-friendly message.
