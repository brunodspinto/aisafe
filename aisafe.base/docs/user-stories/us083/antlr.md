# ANTLR — Lexer and Parser Generation

## Overview

The Flight Plan DSL grammar is defined in a single file written by the team:

```
src/main/antlr4/FlightPlanDsl.g4
```

From this file, ANTLR automatically generates the Lexer and Parser during the Maven build. These generated files are **not written by hand** and should not be edited directly.

---

## Generated Files

Running `mvn compile` produces the following files under `target/generated-sources/antlr4/`:

| File | Description |
|---|---|
| `FlightPlanDslLexer.java` | Tokenises the input (breaks text into tokens) |
| `FlightPlanDslParser.java` | Validates the token sequence against grammar rules |
| `FlightPlanDslListener.java` | Listener interface (one method per grammar rule) |
| `FlightPlanDslBaseListener.java` | Default empty implementation of the listener |
| `FlightPlanDslVisitor.java` | Visitor interface (one method per grammar rule) |
| `FlightPlanDslBaseVisitor.java` | Default empty implementation of the visitor |

---

## How to Generate

```bash
cd aisafe.base
mvn compile
```

The files will appear at:

```
aisafe.base/target/generated-sources/antlr4/
```

---

## Lexer vs Parser

The grammar file `FlightPlanDsl.g4` is a **combined grammar** — it contains both lexer and parser rules in a single file.

**Lexer rules** (uppercase names) — break the raw text into tokens:

```antlr
FLIGHT    : F L I G H T ;
DATE      : DIGIT DIGIT DIGIT DIGIT '-' DIGIT DIGIT '-' DIGIT DIGIT ;
COLON     : ':' ;
```

**Parser rules** (lowercase names) — define the structure of the language:

```antlr
flightPlan : flight EOF ;
flight     : FLIGHT IDENTIFIER TYPE flightType LBRACE leg+ RBRACE ;
leg        : LEG LBRACE departure arrival route segment+ fuel RBRACE ;
```

ANTLR splits these internally and generates a separate `Lexer` and `Parser` class.

---

## Listener and Visitor

ANTLR also generates two traversal interfaces from the grammar.

The project uses both, for different purposes:

| Interface | Used by | Purpose |
|---|---|---|
| Listener | `FlightPlanValidationListener` | Range validation (lat/lon, wind direction). Uses `ParseTreeWalker` — traversal is automatic. |
| Visitor | `FlightPlanAstBuilderVisitor` | AST construction. Each `visitX` method returns a typed AST node. |

The Listener pattern is appropriate when the goal is a side effect (accumulating errors into a list). The Visitor pattern is appropriate when each node must return a value (building the AST bottom-up).

---

## Viewing the Generated Files in IntelliJ

The `target/generated-sources/antlr4/` directory is automatically marked as a generated sources root by the Maven ANTLR4 plugin. The files are visible in the Project panel under:

```
aisafe.base > target > generated-sources > antlr4
```
