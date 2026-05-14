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

## 4.2 Acceptance Tests

**Manual test — AC083.1 (valid DSL sample):**

1. Open the DSL runner or the parsing workflow that consumes the flight plan text.
2. Provide a valid DSL sample with a flight and its legs.
3. Expected: the parser accepts the input and produces the corresponding AST.

**Manual test — AC083.2 (invalid syntax):**

1. Open the DSL runner or the parsing workflow that consumes the flight plan text.
2. Provide a DSL sample with a syntax error.
3. Expected: the parser rejects the input and reports line and column information.

**Manual test — AC083.3 (structured parse result):**

1. Parse a valid sample and then an invalid sample.
2. Inspect the returned result in both cases.
3. Expected: the success case returns a valid AST and the failure case returns a clear error list.

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

## 6. Integration/Demonstration

- A manual demonstration can be executed with `FlightPlanDslRunner` using valid and invalid sample files.
- Parser usage is integrated through `FlightPlanParserFacade`, which is consumed by application-level workflows requiring flight plan validation.

## 7. Observations

- The Sprint 2 implementation intentionally prioritizes lexical/syntactic validation and AST mapping.
- Semantic validation rules can be expanded in later iterations without changing the grammar entry points.
