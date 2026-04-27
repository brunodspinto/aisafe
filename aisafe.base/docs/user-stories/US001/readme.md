# US001 — Technical Constraints

## 1. Context

This task was assigned during Sprint 1 and establishes the technical foundation upon which all subsequent development will be built. It covers the non-functional requirements defined in section 5 of the project requirements, ensuring the team follows the correct tools, languages, build processes and quality standards throughout all sprints.

Since Sprint 1 only involves EAPLI and LAPR4, not all constraints are immediately applicable. Those that are not yet relevant are documented here for completeness and will be enforced in later sprints.

---

## 2. Requirements

**US001:** As Project Manager, I want the team to follow the technical constraints and concerns of the project, as described in section 5 of the project requirements.

**Non-Functional Requirements addressed:**

| NFR | Description | Sprint 1 Status |
|-----|-------------|-----------------|
| NFR01 | Scrum project management | In progress |
| NFR02 | Technical documentation in Markdown with PlantUML | Done |
| NFR03 | Test coverage ≥ 90% on domain and controller packages | Deferred to Sprint 2 |
| NFR04 | Source control on GitHub, main branch only | Done |
| NFR05 | Continuous integration with Maven and GitHub Actions | Done |
| NFR06 | Every commit must leave the system in a valid state | Enforced |
| NFR07 | Unix deployment scripts and readme | Partially done |
| NFR08 | Persistence configurable between in-memory and RDBMS | Deferred to Sprint 2 |
| NFR09 | Authentication and authorisation enforced | Deferred to Sprint 2 |
| NFR10 | Java as main language | Enforced |

---

## 3. Analysis

The team analysed the technical constraints and identified which ones apply in Sprint 1 and which are deferred. The priority in Sprint 1 was to establish the project infrastructure — source control, build automation, continuous integration, documentation structure and deployment scripts — so that all subsequent sprints can build on a solid foundation.

---

## 4. Design

### Build Tool — Maven (NFR05)

The project uses Maven as the build automation tool. The `pom.xml` at the root defines a multi-module project structure with the following modules:

- `exemplo.core` — domain and application core
- `exemplo.persistence.impl` — persistence implementation
- `exemplo.infrastructure.application` — infrastructure layer
- `exemplo.app.backoffice.console` — backoffice console application
- `exemplo.app.user.console` — user console application
- `exemplo.app.other.console` — additional console application
- `exemplo.app.common.console` — shared console utilities
- `exemplo.app.bootstrap` — application bootstrapper
- `exemplo.bootstrappers` — domain bootstrappers

Key build configuration:

- Java 21 (`maven.compiler.release=21`)
- JUnit 5 (Jupiter) for unit testing
- JaCoCo for test coverage reporting (`target/jacoco.exec`)
- Lombok for boilerplate reduction
- H2 in-memory database for development and testing
- EAPLI Framework (core, authz, pubsub) version 25.0.0-RELEASE
- Maven Surefire Report Plugin for test reporting
- UML Doclet for Javadoc generation with UML diagrams

### Continuous Integration — GitHub Actions (NFR05)

A GitHub Actions workflow is configured at `.github/workflows/` and runs on every push or pull request to `main`/`master`, and also nightly at 02:00 UTC via a scheduled cron trigger.

The pipeline:
1. Checks out the repository
2. Sets up JDK 21 (Temurin distribution) with Maven cache
3. Runs `mvn -B clean verify` — which compiles, runs all tests and generates reports

This ensures that every commit is validated automatically and that the nightly build always reflects the current state of the codebase.

### Documentation (NFR02)

All documentation is maintained in the `docs/` folder in Markdown format. PlantUML is used for all diagrams. A script generates PNG exports automatically:

```bash
bash libs/scripts/generate-plantuml-diagrams.sh
```

Requirements: Java 11+, `libs/plantuml-1.2026.2.jar`.

### Unix Scripts (NFR07)

Unix-compatible scripts are available under `libs/scripts/`:

| Script | Purpose |
|--------|---------|
| `build.sh` | Full project build — runs `clean.sh`, Maven build and C build |
| `clean.sh` | Cleans all build artifacts |
| `build_c.sh` | Builds the C simulation component |
| `run_placeholder.sh` | Temporary run entry point for Sprint 1 |
| `generate-plantuml-diagrams.sh` | Generates PNG diagrams from PlantUML sources |

---

## 5. Implementation

The following actions were performed in Sprint 1:

1. Multi-module Maven project structure created and validated locally and on the CI server.
2. GitHub Actions workflow configured with JDK 21, Maven cache and nightly build schedule.
3. JaCoCo configured in the root `pom.xml` for future coverage enforcement (NFR03).
4. Unix scripts created for build, clean and PlantUML diagram generation.
5. Documentation structure created under `docs/` with a `README.md` index.
6. PlantUML diagrams configured and generated for Sprint 1 artifacts.

---

## 6. Integration/Demonstration

The CI pipeline is active and visible in the **Actions** tab of the GitHub repository. Every push to `main` triggers the pipeline automatically. The nightly build runs at 02:00 UTC regardless of commits.

To build and test locally:

```bash
bash libs/scripts/build.sh
mvn test
```

To generate documentation diagrams:

```bash
bash libs/scripts/generate-plantuml-diagrams.sh
```

---

## 7. Observations

**NFR03 — Test Coverage:** JaCoCo is already configured in the `pom.xml` and will collect coverage data on every build. The 90% coverage threshold enforcement on `domain` and `controller` packages will be introduced in Sprint 2 when functional domain code is implemented.

**NFR08 — Database by Configuration:** The H2 in-memory database dependency is already included in the `pom.xml`. The configurable switch between in-memory and RDBMS persistence will be implemented in Sprint 2.

**NFR09 — Authentication and Authorisation:** The EAPLI Framework authz module (`eapli.framework.infrastructure.authz`) is already a dependency in the `pom.xml`. Integration will be implemented in Sprint 2.

**NFR10 — Programming Languages:** Java 21 is the main language. C will be introduced in Sprint 2 for the simulation component (SCOMP). ANTLR is being used for LPROG.