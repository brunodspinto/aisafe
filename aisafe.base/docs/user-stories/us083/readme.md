# US083 - Flight DSL Specification and Validation

## 1. Context
This task was implemented in Sprint 2 to define a formal and validated way of writing flight plans. The goal is to provide a small DSL so flight plans can be described in text and checked before being used by the system.

## 2. Requirements
**US083:** As a Project Manager, I want the team to specify and implement the Flight Description DSL, so that flight plans can be formally defined and validated.

Acceptance criteria:
- The informal lexical and syntactic specification of the DSL is documented.
- A formal grammar is defined using ANTLR.
- The system performs lexical and syntactic validation.
- An internal representation (AST or domain objects) is produced.
- Invalid inputs generate clear and informative error messages.

## 3. Analysis
The team selected ANTLR to avoid manual parser implementation and keep grammar rules explicit.

For Sprint 2, the implementation was intentionally scoped as a minimal viable solution:
- Core Flight DSL with one flight and one or more legs.
- Lexer and parser generated from grammar rules.
- Direct mapping from parse result to AST records.
- Structured parse result with either valid AST or error list.

## 4. Design
The design follows a simple flow:
- Define lexical and syntactic rules in a single grammar file.
- Parse input text using ANTLR generated lexer/parser.
- Build internal representation (`FlightPlanAst` and related records).
- Return clear parse errors with line and column when input is invalid.

Main artifacts used:
- `aisafe.base/src/main/antlr4/FlightPlanDsl.g4`
- `aisafe.base/src/main/java/aisafe/dsl/parser/FlightPlanParserFacade.java`
- `aisafe.base/src/main/java/aisafe/dsl/ast/*`

## 5. Implementation
Implemented files:
- Grammar: `aisafe.base/src/main/antlr4/FlightPlanDsl.g4`
- Parser and mapping: `aisafe.base/src/main/java/aisafe/dsl/parser/FlightPlanParserFacade.java`
- AST records: `aisafe.base/src/main/java/aisafe/dsl/ast/*`
- Manual run example: `aisafe.base/src/main/java/aisafe/dsl/FlightPlanDslRunner.java`
- Tests: `aisafe.base/src/test/java/aisafe/dsl/parser/FlightPlanParserFacadeTest.java`
- Test DSL files: `aisafe.base/src/test/resources/dsl/valid-flight-plan.dsl`, `aisafe.base/src/test/resources/dsl/invalid-flight-plan.dsl`

Build note:
- ANTLR generated classes are created during build in `target/generated-sources/antlr4`.
