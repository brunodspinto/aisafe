# AISafe Base

## Flight Plan DSL Module

This folder now contains a standalone DSL parser implementation based on ANTLR.

### Main Inputs
- Grammar: `src/main/antlr4/FlightPlanDsl.g4`
- Parser facade: `src/main/java/aisafe/dsl/parser/FlightPlanParserFacade.java`
- Sample runner: `src/main/java/aisafe/dsl/FlightPlanDslRunner.java`

### Build/Test (when Maven is available)

```bash
mvn -f aisafe.base/pom.xml test
```

### What it provides
- lexical + syntactic validation
- syntax error collection with line/column details
- internal AST-like domain representation (`aisafe.dsl.ast`)
