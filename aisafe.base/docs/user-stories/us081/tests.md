# US081 — Tests and Coverage

## Scope

US081 covers the creation of a DSL grammar for defining flight plans, including lexical, syntactic, and semantic validation, and correct unit conversion when constructing the AST.

## Automated Tests

### `FlightPlanDslFileTest`

Location: `src/test/java/aisafe/dsl/parser/FlightPlanDslFileTest.java`

Parameterised test class that feeds every `.dsl` file from `src/test/resources/dsl/` through the parser and asserts the expected outcome.

**Test:** `ensureValidDslFileIsAccepted` *(4 parameterised runs)*

Files under `src/test/resources/dsl/valid/`:
- `01_single_leg_regular.dsl`
- `03_multi_leg.dsl`
- `06_lowercase_keywords.dsl`
- `10_units_ft_knot.dsl`

```java
@ParameterizedTest(name = "[valid] {0}")
@MethodSource("validFiles")
void ensureValidDslFileIsAccepted(final Path file) throws IOException {
    final FlightPlanParserFacade.ParseResult result = parse(file);
    assertTrue(result.isValid(),
            "Expected VALID but got errors: " + result.errors());
}
```

**Test:** `ensureInvalidLexicalDslFileIsRejected` *(2 parameterised runs)*

Files under `src/test/resources/dsl/invalid/lexical/`:
- `01_unknown_character.dsl`
- `02_malformed_date_slashes.dsl`

```java
@ParameterizedTest(name = "[invalid/lexical] {0}")
@MethodSource("invalidLexicalFiles")
void ensureInvalidLexicalDslFileIsRejected(final Path file) throws IOException {
    final FlightPlanParserFacade.ParseResult result = parse(file);
    assertFalse(result.isValid(),
            "Expected INVALID (lexical) but was accepted with no errors.");
}
```

**Test:** `ensureInvalidSyntacticDslFileIsRejected` *(3 parameterised runs)*

Files under `src/test/resources/dsl/invalid/syntactic/`:
- `01_missing_semicolon.dsl`
- `04_empty_flight_no_legs.dsl`
- `07_invalid_flight_type.dsl`

```java
@ParameterizedTest(name = "[invalid/syntactic] {0}")
@MethodSource("invalidSyntacticFiles")
void ensureInvalidSyntacticDslFileIsRejected(final Path file) throws IOException {
    final FlightPlanParserFacade.ParseResult result = parse(file);
    assertFalse(result.isValid(),
            "Expected INVALID (syntactic) but was accepted with no errors.");
}
```

**Test:** `ensureInvalidSemanticDslFileIsRejected` *(4 parameterised runs)*

Files under `src/test/resources/dsl/invalid/semantic/`:
- `01_negative_fuel.dsl`
- `03_same_coordinates.dsl`
- `07_leg_sequence_airport_mismatch.dsl`
- `09_route_origin_mismatch.dsl`

```java
@ParameterizedTest(name = "[invalid/semantic] {0}")
@MethodSource("invalidSemanticFiles")
void ensureInvalidSemanticDslFileIsRejected(final Path file) throws IOException {
    final FlightPlanParserFacade.ParseResult result = parse(file);
    assertFalse(result.isValid(),
            "Expected INVALID (semantic) but was accepted with no errors.");
}
```

---

### `FlightPlanSemanticValidatorTest`

Location: `src/test/java/aisafe/dsl/parser/FlightPlanSemanticValidatorTest.java`

Unit tests for `FlightPlanSemanticValidator`, verifying cross-field semantic rules applied to parsed flight-plan ASTs.

**Test:** `ensureValidPlanProducesNoErrors` — a fully valid plan yields an empty error list.

**Test:** `ensureNegativeFuelProducesError` — a leg with negative fuel amount produces an error containing `"fuel"`.

**Test:** `ensureSegmentWithSameStartAndEndProducesError` — a segment whose start and end coordinates are identical produces an error containing `"coordinates"`.

**Test:** `ensureLegArrivalMustMatchNextLegDeparture` — when two legs are chained and the arrival airport of the first differs from the departure airport of the second, an error containing `"arrival airport"` is produced.

**Test:** `ensureLegArrivalTimeMustPrecedeNextLegDepartureTime` — when a second leg departs before the first leg arrives, an error containing `"precede"` is produced.

**Test:** `ensureRouteOriginMustMatchFirstLegDeparture` — a route whose origin differs from the departure airport produces an error containing `"Route origin"`.

**Test:** `ensureRouteDestinationMustMatchLastLegArrival` — a route whose destination differs from the arrival airport produces an error containing `"Route destination"`.

**Test:** `ensureAirportCannotBeVisitedTwice` — a plan that visits the same airport in two different legs produces an error containing `"visited more than once"`.

**Test:** `ensureAllErrorsAreCollectedInSingleExecution` — a plan with multiple violations (same coordinates, negative altitude, negative width, negative fuel) produces at least 3 errors.

**Test:** `ensureZeroAltitudeProducesError` — a segment with altitude `0.0` produces an error containing `"altitude"`.

**Test:** `ensureZeroWidthProducesError` — a segment with width `0.0` produces an error containing `"width"`.

**Test:** `ensureNegativeWindSpeedProducesError` — a segment with negative wind speed produces an error containing `"wind speed"`.

**Test:** `ensureInvalidDateProducesError` — a leg with an invalid date (e.g. month 13) produces an error containing `"date"`.

**Test:** `ensureLegWithNoSegmentsProducesError` — a leg that has no segments produces an error containing `"segment"`.

---

### `FlightPlanAstBuilderVisitorTest`

Location: `src/test/java/aisafe/dsl/parser/FlightPlanAstBuilderVisitorTest.java`

Unit tests verifying that `FlightPlanAstBuilderVisitor` correctly converts altitude and width values to metres when building the AST. Each test parses an inline DSL string and inspects the resulting `SegmentAst`.

**Test:** `ensureFtAltitudeIsConvertedToMeters` — `35000 FT` altitude is stored as `35000 × 0.3048 = 10668.0` metres.

**Test:** `ensureKmWidthIsConvertedToMeters` — `5 KM` width is stored as `5000.0` metres.

**Test:** `ensureFtWidthIsConvertedToMeters` — `3000 FT` width is stored as `3000 × 0.3048 = 914.4` metres.

**Test:** `ensureMeterValuesAreUnchanged` — values already in `M` are stored without modification.

**Test:** `ensureKmAltitudeIsConvertedToMeters` — `10 KM` altitude is stored as `10000.0` metres.

---

## Coverage by Acceptance Criterion

- AC081.1 (grammar accepts well-formed flight plans): `ensureValidDslFileIsAccepted` (4 runs)
- AC081.2 (grammar rejects lexically invalid input): `ensureInvalidLexicalDslFileIsRejected` (2 runs)
- AC081.3 (grammar rejects syntactically invalid input): `ensureInvalidSyntacticDslFileIsRejected` (3 runs)
- AC081.4 (semantic rules are enforced): `ensureInvalidSemanticDslFileIsRejected` (4 runs); all 14 `FlightPlanSemanticValidatorTest` tests
- AC081.5 (unit conversion): `ensureFtAltitudeIsConvertedToMeters`, `ensureKmWidthIsConvertedToMeters`, `ensureFtWidthIsConvertedToMeters`, `ensureMeterValuesAreUnchanged`, `ensureKmAltitudeIsConvertedToMeters`

---

## Acceptance Tests

**Manual test — AC081.1 (valid FT-unit file parsed end-to-end):**

1. Place the following content in a file `test_ft.dsl`:
   ```
   FLIGHT TP800 TYPE REGULAR {
     LEG {
       DEPARTURE: OPO 2026-07-04 08:00;
       ARRIVAL: MAD 2026-07-04 09:30;
       ROUTE: OPO -> MAD;
       SEGMENT {
         START: (+41.15, -8.61);
         END: (+40.49, -3.56);
         ALTITUDE: 35000 FT WIDTH: 5 KM;
         WIND: (270, 30 KNOT);
       }
       FUEL: 6000 KG;
     }
   }
   ```
2. Parse the file using `FlightPlanParserFacade`.
3. Expected: `result.isValid()` is `true`, `result.ast()` is present, and the first segment's `altitudeMeters()` equals `10668.0` (35000 × 0.3048) and `widthMeters()` equals `5000.0`.

**Manual test — AC081.2 / AC081.3 (invalid input produces error with position):**

1. Place content with a syntax error (e.g. missing semicolon after a field) in a file `test_invalid.dsl`.
2. Parse using `FlightPlanParserFacade`.
3. Expected: `result.isValid()` is `false` and `result.errors()` contains at least one `ParseError` with non-zero `line()` and `column()` values indicating the location of the problem.
