# US120 — Flight DSL Specification and Validation

## 1. Context

US120 is the **LPROG** deliverable of the project. It specifies and implements the *Flight
Description DSL* — a small textual language in which a complete flight plan (legs, route,
segments, altitude slots, wind and fuel) can be written and then formally validated before
the system accepts it.

The DSL is the input format consumed by the rest of the system: US121 (*Create a flight plan
from a file*) imports `.dsl` files that must conform to this grammar, and US085 (*Test/Validate
Flight Plan*) re-parses the persisted `dslContent` through this same pipeline to obtain the
route geometry it feeds to the C simulator. US120 therefore defines the contract the other
flight-plan stories rely on.

The implementation lives in `aisafe.base/src/main/java/aisafe/dsl/` plus the ANTLR4 grammar in
`aisafe.base/src/main/antlr4/FlightPlanDsl.g4`. ANTLR generates the lexer/parser into
`aisafe.dsl.generated` at build time (no generated sources are committed).

---

## 2. Requirements

**US120** As a Project Manager, I want the team to specify and implement the Flight
Description DSL, so that flight plans can be formally defined and validated.

**Acceptance Criteria** (from the assignment, p.29):

| AC | Description | Where met |
|----|-------------|-----------|
| AC120.1 | The informal lexical and syntactic specification of the DSL is documented. | §3.1 + grammar comments in `FlightPlanDsl.g4` |
| AC120.2 | A formal grammar is defined using ANTLR. | `FlightPlanDsl.g4` |
| AC120.3 | The system performs lexical, syntactic and semantic analysis. | `FlightPlanParserFacade` 4-stage pipeline (§4.1) |
| AC120.4 | Use of listeners / visitors. | `FlightPlanValidationListener` (listener) + `FlightPlanAstBuilderVisitor` (visitor) |
| AC120.5 | An internal representation (AST or domain objects) is produced. | `aisafe.dsl.ast.*` records |
| AC120.6 | Invalid inputs generate clear and informative error messages. | `ParseError(line, column, message, offendingSymbol)` |

**Dependencies / References:**

- **US121** — *Create a flight plan from a file*: imports files that must conform to this DSL
  and be validated according to US120.
- **US085** — *Test/Validate Flight Plan*: re-parses `dslContent` via `FlightPlanParserFacade`
  to obtain the AST consumed by the C `flight_tester`.

---

## 3. Analysis

### 3.1 Informal specification

A flight plan is one `FLIGHT` with an identifier, a type (`REGULAR` | `CHARTER`) and one or
more `LEG`s. Each leg declares a departure and arrival date-time, a route
(`origin -> destination`), one or more `SEGMENT`s, and a fuel quantity. Each segment declares
its start/end coordinates, one or more altitude slots (altitude + width), and a wind vector.

Lexical highlights (all in `FlightPlanDsl.g4`):

- **Keywords are case-insensitive**, implemented with per-letter fragments (`A`..`Z`), so
  `FLIGHT` and `flight` are both accepted (see valid sample `06_lowercase_keywords.dsl`).
- **Airport codes:** `ICAO_CODE` (4 uppercase letters) is declared **before** `IATA_CODE`
  (3 uppercase letters) so ANTLR's longest-match picks ICAO when applicable.
- **Identifier:** 2 uppercase letters + 1–4 digits + optional letter suffix (`TP123`, `TP1234A`).
- **`DATE`** = `YYYY-MM-DD`, **`TIME`** = `HH:MM`, **`NUMBER`** = integer or decimal.
- Whitespace, `//` line comments and `/* */` block comments are skipped.

### 3.2 Internal representation (AST)

The parse tree is converted into immutable Java `record`s in `aisafe.dsl.ast`:

| Record | Fields |
|--------|--------|
| `FlightPlanAst` | `identifier`, `FlightType`, `List<LegAst> legs` |
| `LegAst` | `departure`, `arrival` (`EndpointAst`), `route` (`RouteAst`), `List<SegmentAst>`, `fuel` (`FuelAst`) |
| `SegmentAst` | `from`, `to` (`CoordinateAst`), `altitudeMeters`, `widthMeters`, `windSpeed`, `windDirection` |
| `CoordinateAst` | `latitude`, `longitude` |
| `RouteAst` / `EndpointAst` / `FuelAst` / `FlightType` | airport codes / date-time / fuel amount+unit / `REGULAR`\|`CHARTER` |

No new domain aggregate is introduced by US120 — the AST is the boundary object the
application layer (US085/US121) consumes.

---

## 4. Design

### 4.1 Four-stage validation pipeline

`FlightPlanParserFacade.parse(String)` returns a `ParseResult` (valid + AST, or invalid +
ordered `List<ParseError>`). It performs, in order:

1. **Lexical + syntactic analysis** — ANTLR `FlightPlanDslLexer` / `FlightPlanDslParser`. The
   default ANTLR error listeners are *removed* and replaced by a custom `BaseErrorListener`
   that records every `syntaxError` as a `ParseError(line, column, msg, offendingSymbol)`.
   If any syntactic error is found, parsing stops and the errors are returned.
2. **Range validation (Listener)** — `ParseTreeWalker.DEFAULT.walk(FlightPlanValidationListener, tree)`
   checks token-level ranges directly on the parse tree: latitude ∈ [-90, 90], longitude ∈
   [-180, 180], wind direction ∈ [0, 359].
3. **AST construction (Visitor)** — `FlightPlanAstBuilderVisitor extends FlightPlanDslBaseVisitor`
   traverses the tree and builds the `FlightPlanAst`.
4. **Cross-field semantic validation** — `FlightPlanSemanticValidator.validate(ast)` enforces
   rules that span multiple nodes: fuel strictly positive; altitude/width positive; start ≠ end
   coordinate; segment continuity (end of segment *i* = start of *i+1*); leg sequencing
   (arrival airport of leg *N* = departure of leg *N+1*); chronological ordering
   (departure < arrival, and leg *N* arrival < leg *N+1* departure); no airport visited twice.

This satisfies AC120.4 by using **both** required ANTLR mechanisms for distinct concerns: a
*listener* for stateless per-node range checks, and a *visitor* for building the typed AST.

### 4.2 Error model (AC120.6)

Every failure is a `ParseError` carrying line, column, a human-readable message and the
offending symbol — e.g. `Wind direction must be in range 0-359 (got 400).` or
`Leg 1: departure (2026-06-01 11:00) must be before arrival (2026-06-01 10:45).` Errors are
**accumulated**, not fail-fast, within each stage so the user sees all problems of a given
kind at once.

---

## 5. Implementation

| File | Role |
|------|------|
| `src/main/antlr4/FlightPlanDsl.g4` | ANTLR4 grammar — lexer + parser rules (AC120.1/.2) |
| `dsl/parser/FlightPlanParserFacade.java` | Orchestrates the 4-stage pipeline; defines `ParseResult` / `ParseError` |
| `dsl/parser/FlightPlanValidationListener.java` | **Listener** — coordinate / wind-direction range checks |
| `dsl/parser/FlightPlanAstBuilderVisitor.java` | **Visitor** — parse tree → `FlightPlanAst` |
| `dsl/parser/FlightPlanSemanticValidator.java` | Cross-field semantic rules |
| `dsl/ast/*.java` | Immutable AST records (AC120.5) |

The ANTLR grammar is compiled by the `antlr4-maven-plugin`; generated lexer/parser classes
land in `aisafe.dsl.generated` and are not committed.

---

## 6. Integration / Demonstration

A minimal valid plan (`src/test/resources/dsl/valid/01_single_leg_regular.dsl`):

```
FLIGHT TP123 TYPE REGULAR {
  LEG {
    DEPARTURE: 2026-06-01 10:00;
    ARRIVAL: 2026-06-01 10:45;
    ROUTE: OPO -> LIS;
    SEGMENT {
      START: (+41.15, -8.61);
      END: (+38.72, -9.14);
      ALTITUDE: 10000 M WIDTH: 2000 M;
      WIND: (180, 12 M/S);
    }
    FUEL: 5300 KG;
  }
}
```

```java
ParseResult result = new FlightPlanParserFacade().parse(dslText);
if (result.isValid()) {
    FlightPlanAst ast = result.ast().orElseThrow();
} else {
    result.errors().forEach(System.out::println); // line, column, message
}
```

Run the DSL tests:

```bash
mvn -f aisafe.base/pom.xml test -Dtest=FlightPlanDslFileTest,FlightPlanSemanticValidatorTest,FlightPlanAstBuilderVisitorTest
```

### Test corpus (`src/test/resources/dsl/`)

| Bucket | Files | Validates |
|--------|-------|-----------|
| `valid/` | single-leg, multi-leg, lowercase keywords, FT/KNOT units | accepted plans |
| `invalid/lexical/` | unknown character, malformed date | lexer errors |
| `invalid/syntactic/` | missing semicolon, empty flight, invalid type | parser errors |
| `invalid/semantic/` | negative fuel, same coordinates, airport mismatch, route-origin mismatch, departure-after-arrival, segment discontinuity | semantic errors |

---

## 7. Observations

- **No domain aggregate added** — the AST is the contract; the application layer (US085/US121)
  maps it onward. This keeps the `aisafe.dsl` package free of persistence concerns.
- **Listener + Visitor are used for complementary reasons** — range checks are stateless and
  fit a listener walk; AST construction needs return values and fits a visitor. This is a
  deliberate design choice, not duplication.
- **Generated sources are not committed** — ANTLR regenerates `aisafe.dsl.generated` on every
  build, so the grammar is the single source of truth.
- **Reused by US085** — `FlightPlanParserFacade` is invoked again at test time to recover the
  route geometry from the stored `dslContent`, so any change to the grammar/validation
  automatically governs which plans are testable.
