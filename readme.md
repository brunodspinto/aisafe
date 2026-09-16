# AISafe

AISafe is a prototype flight control and management system for the air traffic control market. It covers back-office management of flight operations: registering aircraft and aviation infrastructure, planning flights through a Domain Specific Language (DSL), integrating weather data, and running a parallel flight simulation in C that detects safety violations as they happen.

## Features

**Back-office management (Java console application)**
- Users and roles: login, password change, add/list users, enable/disable accounts
- Air transport companies, their collaborators and their pilot rosters
- Aircraft fleet: engine models, makers, aircraft models (add/remove engines), adding and decommissioning aircraft
- Aviation infrastructure: air control areas, airports and flight routes

**Flight planning**
- Create flight plans through a form or by importing a `.dsl` file
- Flight plan DSL written with an ANTLR 4 grammar (`FlightPlanDsl.g4`), with lexical, syntactic and semantic validation
- Test a flight plan by exporting it to JSON and running it through the native `flight_tester`

**Weather data**
- Register, consult and bulk-import weather data (CSV)
- Attach weather data to a flight

**Parallel flight simulation (C)**
- One child process per flight (`fork`), synchronised with the parent through POSIX shared memory and named semaphores
- Coordinator, safety, environment (wind) and report threads (`pthread` mutexes and condition variables)
- Filters positions by air control area and checks trajectories against a horizontal/vertical safety cylinder
- Signals flights on safety violations and stops the simulation after a configurable number of violations
- Writes a final simulation report and a live violation log

**Reporting and networking**
- Monthly operational reports and simulation reports
- TCP server (port 9999) for pilot, weather-person and collaborator client applications
- Remote access logging server: receives UDP events (port 9090) and exposes them over HTTP (port 8080)

## Tech Stack

| Area | Technologies |
|---|---|
| Back end | Java 21, Maven, EAPLI framework (DDD building blocks, authentication/authorisation) |
| Persistence | JPA / Hibernate 6, H2 (in-memory and TCP server modes), PostgreSQL JDBC driver |
| DSL | ANTLR 4.13 |
| Simulation | C99, POSIX processes, threads, shared memory, semaphores and signals, cJSON |
| Testing | JUnit 5, JaCoCo, C unit tests |
| Documentation | Markdown, PlantUML |
| CI | GitHub Actions |

## Getting Started

### Prerequisites

- Java 21 and Maven
- GCC and Make on Linux, macOS or WSL (the simulator uses POSIX IPC, so it does not build with native Windows toolchains)
- Bash, to run the helper scripts

### Build and test (Java)

From the repository root:

```bash
mvn -f aisafe.base/pom.xml clean install   # compile, run tests, generate JaCoCo report
mvn -f aisafe.base/pom.xml test            # tests only
```

The coverage report is generated at `aisafe.base/target/site/jacoco/`.

To build everything (Java, skipping tests, plus the C simulation) in one step:

```bash
bash aisafe.base/libs/scripts/build.sh   # clean.sh removes build outputs
```

### Build and test the simulation (C)

```bash
cd aisafe.base/simulation
make          # builds flight_simulator and flight_tester
make test     # builds and runs the C unit tests
make clean
```

### Run the simulation

The simulator reads `simulation.conf` and `flight_plans.json` from the current directory:

```bash
cd aisafe.base/simulation
./flight_simulator               # simulates every plan in flight_plans.json
./flight_simulator --collision   # collision-test mode
```

`flight_plans.json` defines the flights. `simulation.conf` sets the air control area bounds, safety separation distances and the violation limit. An optional `AISAFE_WIND="speed,dir"` environment variable sets the wind. Results are written to `simulation_report.txt` and `simulation_violation_log.txt`.

### Run the back-office application

Run it from `aisafe.base/`, so the application can find the native `simulation/flight_tester` binary (build it first with `make`):

```bash
cd aisafe.base
./run-inmemory.sh   # in-memory persistence (Windows: run-inmemory.bat)
```

Demo credentials: `admin` / `Password1`.

To keep data between runs, use JPA with an H2 TCP server:

```bash
cd aisafe.base
./start-h2.sh       # terminal 1: H2 server on port 9093, data in aisafe.base/db/
./run-bootstrap.sh  # terminal 2: seed demo data (run once)
./run-jpa.sh        # terminal 2: start the application
```

`run-inmemory` and `run-jpa` choose the persistence mode by rewriting `src/main/resources/application.properties`. See [`aisafe.base/docs/persistence.md`](aisafe.base/docs/persistence.md) for details.

**PostgreSQL:** the PostgreSQL JDBC driver is already a dependency. To use it, set the JDBC URL, user and password in `aisafe.base/src/main/resources/META-INF/persistence.xml`.

### Client and server applications

While the back-office application is running (it starts the TCP server):

```bash
cd aisafe.base
./run-pilot-client.sh      # pilot TCP client
./run-weather-client.sh    # weather-person TCP client
./run-us91.sh              # remote access logging server (UDP 9090, HTTP 8080)
```

### Generate PlantUML diagrams

```bash
bash aisafe.base/libs/scripts/generate-plantuml-diagrams.sh
```

Requires Java 11 or later. If `plantuml.jar` is missing, the script downloads it to `aisafe.base/libs/`. The SVGs are written to the `svg/` folder next to each `puml/` folder under `aisafe.base/docs/`.

## Project Structure

```
aisafe.base/
├── pom.xml                  # Maven build
├── src/main/antlr4/         # Flight plan DSL grammar
├── src/main/java/aisafe/
│   ├── app/                 # Console app, TCP clients, logging server
│   ├── <bounded contexts>/  # aircraft, airport, flightplan, weatherdata, ... (domain/application/repositories)
│   ├── dsl/                 # DSL parser, AST and JSON serialisation
│   ├── infrastructure/      # Persistence (in-memory, JPA) and simulation integration
│   └── tcpserver/           # TCP server and session handlers
├── src/test/                # JUnit tests and DSL/CSV fixtures
├── simulation/              # C simulator, flight tester, Makefile and C tests
├── libs/scripts/            # build.sh, clean.sh, build_c.sh, generate-plantuml-diagrams.sh
└── docs/                    # Requirements, domain model, design and per-user-story documentation
```

Technical documentation: [`aisafe.base/docs`](aisafe.base/docs/README.md).

## Team

- Bruno Pinto
- Hugo Pereira
- Joana Braga
- Jorge Rocha
- Marcos Menezes

## Academic Context

Developed as the Integrative Project (LAPR4) of the 2nd year, 2nd semester of the Degree in Informatics Engineering at ISEP – Polytechnic of Porto, 2025/2026, integrating EAPLI, LPROG, SCOMP and RCOMP, across 3 sprints.
