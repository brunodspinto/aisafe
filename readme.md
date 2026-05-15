# Project AISafe

## 1. Description of the Project

AISafe is a prototype flight control and management system developed for a startup targeting the air traffic control market. The system covers the back-office management of flight operations, providing functionality for aircraft and infrastructure registration, flight planning via a formal Domain Specific Language (DSL), weather data integration, and parallelised flight simulation with real-time safety violation detection.

## 2. Planning and Technical Documentation

[Planning and Technical Documentation](aisafe.base/docs/README.md)

## 3. How to Build

Prerequisites:
- Bash (Linux/macOS/WSL)
- Maven (`mvn` in `PATH`) — Java 21
- GCC (`gcc` in `PATH`) for the C simulation component

From the repository root, run:

```bash
bash aisafe.base/libs/scripts/build.sh
```

This script executes in order:
1. `aisafe.base/libs/scripts/clean.sh` — removes previous build artefacts
2. Maven build (`mvn install -DskipTests`) — compiles all Java modules
3. `aisafe.base/libs/scripts/build_c.sh` — compiles the C simulation into `aisafe.base/bin/simulation`

To build only the C component manually:

```bash
cd aisafe.base/simulation
gcc -Wall -Wextra -o ../bin/simulation \
    main.c flight_process.c flight_data.c aca_filter.c ipc.c config.c us102.c -lm
```

## 4. How to Execute Tests

Run all Java unit tests from the repository root:

```bash
mvn -f aisafe.base/pom.xml test
```

Test coverage is enforced via JaCoCo (minimum 90% on controller and domain packages). Reports are generated at `aisafe.base/target/site/jacoco/`.

## 5. How to Run

### Backoffice Console Application (Java)

From the repository root:

```bash
mvn -f aisafe.base/pom.xml exec:java
```

This launches the interactive console (`AiSafeConsoleApp`). Bootstrap data (admin user, default air control areas, airports, companies) is loaded automatically on first run.

Default admin credentials: `admin` / `Password1`

### Flight Simulation (C — US100/101/102/103)

After building, run from the `aisafe.base/bin/` directory (the build script copies `simulation.conf` there automatically):

```bash
cd aisafe.base/bin

# Normal mode: 3 flights (FLIGHT_01, FLIGHT_02, FLIGHT_03)
./simulation

# Collision test mode: adds FLIGHT_04 (~2.2 km from FLIGHT_01)
./simulation --collision
```

Simulation parameters (ACA bounds, safety thresholds, number of flights) are read from `simulation.conf` in the same directory as the binary.

## 6. How to Install/Deploy into Another Machine (or Virtual Machine)

Minimum setup:
- Clone the repository
- Install Bash, Maven (Java 21), and GCC
- From the repository root, run the build script and then start the application

```bash
bash aisafe.base/libs/scripts/clean.sh
bash aisafe.base/libs/scripts/build.sh
mvn -f aisafe.base/pom.xml exec:java
```

The system uses H2 (embedded) by default for development. To use PostgreSQL, configure `aisafe.base/src/main/resources/META-INF/persistence.xml` with the appropriate JDBC URL and credentials.

## 7. How to Generate PlantUML Diagrams

```bash
bash aisafe.base/libs/scripts/generate-plantuml-diagrams.sh
```

Requirements:
- Java 11+
- `aisafe.base/libs/plantuml.jar` (auto-downloaded on first run if missing)
