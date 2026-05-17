# Design

This document records the high-level design decisions that shape the AISafe code base in Sprint 2. The granular design of each user story lives in `docs/user-stories/usXXX/readme.md`; what follows is what is true across all of them.

## 1. Logical architecture

The codebase follows a classic four-layer DDD partitioning. Each aggregate has its own package; layers are sub-packages, not separate Maven modules.

```
aisafe.<feature>.domain          ← aggregate roots, value objects, enums, invariants
aisafe.<feature>.application     ← use-case controllers (@UseCaseController)
aisafe.<feature>.repositories    ← repository interfaces
aisafe.infrastructure.persistence.inmemory   ← in-memory repository impls
aisafe.infrastructure.persistence.jpa        ← JPA repository impls + factory
aisafe.app.console               ← console presentation layer + bootstrap + main
aisafe.app.console.presentation  ← UI per feature (extends AbstractUI from EAPLI)
```

Dependencies flow inward: `presentation → application → domain`; `persistence → domain`. The application layer is the only entry point into the domain — UIs never instantiate aggregates directly.

## 2. Persistence strategy

NFR08 requires that persistence be selectable at runtime. The team implemented this through a factory indirection:

- `PersistenceContext.repositories()` returns the active `RepositoryFactory`.
- The factory is chosen from `application.properties` (`persistence.repositoryFactory=...`).
- Two implementations exist: `InMemoryRepositoryFactory` (no IO) and `JpaRepositoryFactory` (Hibernate + H2 in TCP server mode).
- Switching modes is scripted: `start-h2.sh` / `start-h2.bat` boots the H2 TCP server; `run-jpa.sh` / `run-jpa.bat` and `run-inmemory.sh` / `run-inmemory.bat` set the property and launch the JVM.

JPA configuration is in `src/main/resources/META-INF/persistence.xml`. `hbm2ddl.auto=update` creates/upgrades schema on the fly; no migration scripts exist yet. See [persistence.md](../persistence.md).

## 3. Authentication & authorization

EAPLI ships a complete authn/authz subsystem (`SystemUser`, `Role`, `AuthorizationService`, `UserManagementService`). The team plugs into it instead of reimplementing user management:

- The login flow uses EAPLI's `AuthenticationContext`.
- Each `@UseCaseController` calls `authz.ensureAuthenticatedUserHasAnyOf(role, ...)` at the top of every public method.
- Role constants live in `aisafe.usermanagement.domain.AiSafeRoles`.
- `AddUserController` delegates `SystemUser` creation to `UserManagementService.registerNewUser(...)` and only owns the AISafe-specific `User` aggregate.

The main menu (`MainMenu`) gates branches by role so that operators do not see options they cannot execute.

## 4. UI conventions

Every console screen extends EAPLI's `AbstractUI`. The team's conventions are:

- `doShow()` must return `false` to keep the menu loop alive. Returning `true` exits the loop and closes the current menu.
- Numeric and date inputs are read inside `do { ... } while(invalid)` retry loops; the user cannot escape an invalid value by pressing Enter.
- Validation errors from the domain are caught at the UI boundary and reported as `Validation Error: ...`; framework or persistence errors surface as `Error: ...`.

## 5. Domain validation strategy

Invariants are enforced in the aggregate's constructor and mutators. UIs perform **additional** input validation (e.g., regex for phone numbers, max digits, format) so the user gets early feedback, but the domain never trusts the UI and re-checks every constraint.

Where the database can enforce uniqueness without ambiguity (`@UniqueConstraint` on natural keys: ICAO, IATA, model name + maker), the team adds the constraint. Controllers do **not** pre-query for uniqueness — they let the database reject the insert and surface the error. This avoids race conditions between two simultaneous registrations.

## 6. C simulation subsystem

US100–US103 (SCOMP) are implemented in C, not Java, and live under `aisafe.base/simulation/`. The Java side does not call the C code; the two subsystems are independent and share data only through files and conventions.

The simulation forks one process per flight; the parent uses two pipes per child (control + telemetry) and signals (`SIGUSR1` for safety violations, `SIGTERM` for early termination). Step-by-step synchronization is enforced through a `G`/`S` (`Go`/`Stop`) protocol on the control pipe — see [us103/readme.md](../../user-stories/us103/readme.md).

The build script is `libs/scripts/build_c.sh`; there is no Makefile yet.

## 7. Testing strategy

- **Domain and value objects** — straightforward JUnit 5 unit tests, no mocks. The AAA convention is used.
- **Controllers** — exercised in integration with the in-memory factory where practical; when authorization or transactionality is awkward to set up, the AC is covered as a documented manual test.
- **C simulation** — currently covered only by manual scenarios (see `simulation/simulation.conf`).
- The target is ≥ 90 % coverage on `domain` + `controller` packages per NFR03.

## 8. Documentation conventions

- One folder per US under `docs/user-stories/usXXX/`.
- `readme.md` follows the 7-section template: Context, Requirements, Analysis, Design, Implementation, Integration/Demonstration, Observations.
- `tests.md` records the acceptance-test inventory and coverage notes.
- PUML source under `puml/`, exported SVG under `svg/`. Diagrams referenced in readme must be checked-in as both PUML and SVG.
- Global cross-cutting docs live in `docs/global-artifacts/`.

## 9. Deviations from the textbook

- **Maven modules.** The project is a single module (`aisafe.base`) rather than the canonical multi-module DDD layout. The team kept it flat to reduce Sprint 2 ceremony; modularization is on the Sprint 3 backlog.
- **DSL processing.** The Flight DSL grammar (US083) is compiled with the ANTLR Maven plugin in `aisafe.base` itself, not in a separate `dsl` module.
- **No CI matrix.** GitHub Actions runs a single Linux/Java 21 build; Windows is verified locally only.
