# Project AIControl

## 1. Description of the Project

AISafe is a prototype flight control and management system developed for a startup targeting the air traffic control market. The system covers the back-office management of flight operations, providing functionality for aircraft and infrastructure registration, flight planning via a formal Domain Specific Language (DSL), weather data integration, and parallelised flight simulation with real-time safety violation detection.

## 2. Planning and Technical Documentation

[Planning and Technical Documentation](aisafe.base/docs/README.md)

## 3. How to Build

US005 defines Unix-compatible automation scripts under `aisafe.base/libs/scripts`.

Prerequisites:
- Bash (Linux/macOS/WSL)
- Maven (`mvn` in `PATH`)
- GCC (`gcc` in `PATH`) for C build step

From the repository root, run:

```bash
bash aisafe.base/libs/scripts/build.sh
```

This script executes:
- `aisafe.base/libs/scripts/clean.sh`
- Java build with Maven (`mvn install -DskipTests`)
- C build (`aisafe.base/libs/scripts/build_c.sh`)

## 4. How to Execute Tests

In US005 (Sprint 1), there is no dedicated test script yet.

To run Maven tests manually from the repository root:

```bash
mvn test
```

Note: the US005 build script skips tests (`-DskipTests`) by design.

## 5. How to Run

US005 provides a temporary run entry point only (no functional application runtime in Sprint 1):

```bash
bash aisafe.base/libs/scripts/run_placeholder.sh
```

This script exits successfully and documents that real runtime scripts are deferred to later sprints.

## 6. How to Install/Deploy into Another Machine (or Virtual Machine)

For US005 scope, deployment is limited to setting up a Unix-compatible environment and running scripts.

Minimum setup:
- Clone the repository.
- Install Bash, Maven, and GCC.
- Ensure Java 11+ is available if you need PlantUML generation.

Typical sequence from repository root:

```bash
bash aisafe.base/libs/scripts/clean.sh
bash aisafe.base/libs/scripts/build.sh
bash aisafe.base/libs/scripts/run_placeholder.sh
```

There is no application/database deployment script yet in Sprint 1.

## 7. How to Generate PlantUML Diagrams

To generate PlantUML diagrams for documentation, run:

```bash
bash aisafe.base/libs/scripts/generate-plantuml-diagrams.sh
```

Requirements:
- Java 11+
- `aisafe.base/libs/plantuml.jar` (auto-downloaded on first run if missing)


