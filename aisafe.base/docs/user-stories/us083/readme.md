# US083 — Flight DSL Specification

## 1. Requirements

**US083:** As a Project Manager, I want the team to specify and implement the Flight Description DSL, so that flight plans can be formally defined and validated.

**Acceptance criteria:**
- AC083.1 — Grammar defined in ANTLR4 with lexical and syntactic rules.
- AC083.2 — Lexical and syntactic validation performed by the generated parser.
- AC083.3 — AST produced via a **Visitor** (project requirement §3.4.4).
- AC083.4 — Range validation performed via a **Listener** (project requirement §3.4.4).

---

## 2. DSL Language Reference

### Structure

One flight plan per file. Keywords are **case-insensitive**.

```
FLIGHT <id> TYPE <REGULAR|CHARTER> {
  LEG {
    DEPARTURE: <airport> <YYYY-MM-DD> <HH:MM>;
    ARRIVAL:   <airport> <YYYY-MM-DD> <HH:MM>;
    ROUTE:     <airport> -> <airport>;
    SEGMENT {
      START: (<lat>, <lon>);
      END:   (<lat>, <lon>);
      ALTITUDE: <value> <M|KM|FT>  WIDTH: <value> <M|KM|FT>;
      WIND: (<direction>, <value> <KNOT|M/S>);
    }
    FUEL: <value> <KG|L>;
  }
}
```

### Token rules

| Token | Format | Examples |
|-------|--------|---------|
| Flight ID | 2 uppercase + 1–4 digits + optional uppercase | `TP123`, `AA1234A` |
| Airport | IATA (3 letters) or ICAO (4 letters) | `LIS`, `LPPT` |
| Date | `YYYY-MM-DD` | `2026-06-01` |
| Time | `HH:MM` | `10:00` |
| Distance unit | `M` \| `KM` \| `FT` | altitude and width |
| Speed unit | `KNOT` \| `M/S` | wind speed only |
| Fuel unit | `KG` \| `L` | |

Distance and speed units are **separate grammar rules** — the grammar statically prevents mixing them:

```
ALTITUDE: 10000 KNOT   →  syntax error  (KNOT is not a distanceUnit)
WIND: (180, 12 M)      →  syntax error  (M is not a speedUnit)
```

---

## 3. Design

### Parse pipeline

```
DSL text
   │
   ▼
[Stage 1] ANTLR Lexer + Parser
          → if syntax errors: return ParseResult.invalid(errors)
   ▼
[Stage 2] FlightPlanValidationListener   (LISTENER)
          exitWindDecl  → wind direction ∈ [0, 359]
          exitCoordinate → lat ∈ [-90,90]  lon ∈ [-180,180]
          → if range errors: return ParseResult.invalid(errors)
   ▼
[Stage 3] FlightPlanAstBuilderVisitor    (VISITOR)
          builds FlightPlanAst from the parse tree
   ▼
[Stage 4] FlightPlanSemanticValidator
          cross-field rules on the AST
   ▼
ParseResult.valid(ast)
```

### Visitor vs Listener

| Component | Pattern | Reason |
|-----------|---------|--------|
| `FlightPlanAstBuilderVisitor` | **Visitor** | Each `visitX` returns a typed AST node (`FlightPlanAst`, `LegAst`, …). Visitor supports return values; Listener does not. |
| `FlightPlanValidationListener` | **Listener** | Stateful side-effect — accumulates errors into a list. `ParseTreeWalker` handles traversal automatically. |

### Key classes

| Class | Role |
|-------|------|
| `FlightPlanDsl.g4` | ANTLR4 grammar |
| `FlightPlanParserFacade` | Orchestrates the 4-stage pipeline |
| `FlightPlanAstBuilderVisitor` | Visitor — builds AST |
| `FlightPlanValidationListener` | Listener — validates ranges |
| `FlightPlanSemanticValidator` | Cross-field semantic checks |

---

## 4. How to run

```bash
cd aisafe.base

# Compile (regenerates ANTLR sources automatically)
mvn compile

# Run all tests
mvn test

# Run only the DSL grammar tests
mvn test -Dtest=FlightPlanDslFileTest
```

---

## 5. Test scenarios

### Valid files — grammar must accept

**`01_single_leg_regular.dsl`** — baseline case

```dsl
FLIGHT TP123 TYPE REGULAR {
  LEG {
    DEPARTURE: OPO 2026-06-01 10:00;
    ARRIVAL: LIS 2026-06-01 10:45;
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

**`03_multi_leg.dsl`** — two legs with correct airport and time sequence (OPO → LIS → FAO)

**`06_lowercase_keywords.dsl`** — all keywords in lowercase; verifies case-insensitivity

**`10_units_ft_knot.dsl`** — altitude in `FT`, width in `KM`, wind speed in `KNOT`; verifies unit type safety

---

### Invalid — lexical errors

**`01_unknown_character.dsl`** — `@` is not a valid token

```dsl
ROUTE: OPO -> @LIS;   // lexer error: token recognition error at '@'
```

**`02_malformed_date_slashes.dsl`** — date uses `/` instead of `-`

```dsl
DEPARTURE: OPO 2026/06/01 10:00;   // lexer error: '/' is not a valid token here
```

---

### Invalid — syntactic errors

**`01_missing_semicolon.dsl`** — missing `;` after DEPARTURE

```dsl
DEPARTURE: OPO 2026-06-01 10:00     // parser error: expecting ';', found 'ARRIVAL'
ARRIVAL: LIS 2026-06-01 10:45;
```

**`04_empty_flight_no_legs.dsl`** — `leg+` requires at least one leg

```dsl
FLIGHT TP123 TYPE REGULAR {
}   // parser error: expecting LEG, found '}'
```

**`07_invalid_flight_type.dsl`** — `PRIVATE` is not in the grammar

```dsl
FLIGHT TP123 TYPE PRIVATE {   // parser error: PRIVATE not in flightType rule
```

---

### Invalid — semantic errors

**`01_negative_fuel.dsl`** — fuel must be strictly positive

```dsl
FUEL: -5300 KG;   // semantic error: fuel quantity must be strictly positive
```

**`03_same_coordinates.dsl`** — START and END must differ

```dsl
START: (+41.15, -8.61);
END:   (+41.15, -8.61);   // semantic error: start and end coordinates must be different
```

**`07_leg_sequence_airport_mismatch.dsl`** — arrival of leg 1 ≠ departure of leg 2

```dsl
// Leg 1 arrives LIS, Leg 2 departs FAO
// semantic error: Leg 1 arrival airport (LIS) must match leg 2 departure airport (FAO)
```

**`09_route_origin_mismatch.dsl`** — ROUTE origin ≠ first leg departure

```dsl
DEPARTURE: OPO ...
ROUTE: FAO -> LIS;   // semantic error: Route origin (FAO) must match first leg departure (OPO)
```

---

## 6. Test results

```
[FlightPlanDslFileTest]   Tests run: 13, Failures: 0, Errors: 0
BUILD SUCCESS
```
