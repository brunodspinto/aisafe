# US111 — Generate a Simulation Report

## 1. Context

This US is designed in Sprint 3. It allows a **Flight Control Operator (FCO)** to obtain a summary report of a flight simulation's results, in order to decide whether the programmed flights are safe to run.

The flight simulation itself is a SCOMP/C component (US100–US109): it forks one process per flight, tracks aircraft positions, detects safety violations and, at the end, produces its own raw output (US109 writes `simulation_report.txt`). US111 is the **EAPLI/Java** use case that turns that simulation outcome into a domain `SimulationReport`, persists it through the `Simulation` aggregate, and exports a human-readable report file for the FCO.

The `Simulation` aggregate (`Simulation`, `SimulationReport`, `FlightExecutionStatus`, `SafetyViolation`, `SimulationStatus`) is already defined in **Domain Model V10**. US111 populates and reports on it — it adds no new domain concept, only the application logic that builds and exports the report.

---

## 2. Requirements

**US111** As a Flight Control Operator, I want to receive a summary of the simulation results so that I can determine if the programmed flights are safe to run.

**Acceptance Criteria:**

- **AC111.1** The system must generate a report and store it in a file.
- **AC111.2** The report should include the total number of flights and their execution status.
- **AC111.3** If safety violations occur, the report must list their timestamps and positions.
- **AC111.4** The report should indicate whether the scheduled flights plan passed or failed validation.

**Dependencies/References:**

- US030 — Authentication and Authorization (only an authenticated Flight Control Operator may generate the report).
- US100–US109 — Flight simulation (SCOMP/C). The simulation produces the results that US111 reports on; US109 writes the raw `simulation_report.txt`.
- US050 — Register an Air Control Area (a simulation covers one air control area).
- US080 — Create a Flight Plan (the simulated flights are instances of planned flights).

---

## 3. Analysis

The report content required by the acceptance criteria maps directly onto the `Simulation` aggregate already defined in Domain Model V10:

| AC                                           | Domain element                                                                                  |
|----------------------------------------------|-------------------------------------------------------------------------------------------------|
| AC111.1 (file)                               | `SimulationReportExporter` writes the formatted `SimulationReport` to a file                    |
| AC111.2 (total flights + execution status)   | `SimulationReport.totalFlights` and a `FlightExecutionStatus` per flight                        |
| AC111.3 (violations with timestamp/position) | a `SafetyViolation` per detected conflict (`timestamp`, `latitude`, `longitude`, `altitude`, …) |
| AC111.4 (pass/fail)                          | `SimulationReport.passed`                                                                       |

### Data source — integration with the SCOMP simulation

The simulation runs as a separate C program. US111 does **not** re-run the physics; it **consumes the simulation's results**. The integration point is a results file produced by the simulation (the `simulation_report.txt` of US109, or a structured results file). A `SimulationResultsReader` (infrastructure adapter) parses that output and yields the data needed to build the domain `SimulationReport`. This keeps the Java domain decoupled from the C implementation — only the adapter knows the file format.

> **Design note.** This mirrors the boundary used elsewhere in the project: the application layer depends on an interface (`SimulationResultsReader`), not on the C program. If the simulation output format changes, only the adapter changes — the controller and the domain are unaffected (Protected Variations / DIP).

### Main classes involved

| Class                                | Type                    | Responsibility                                                                                          |
|--------------------------------------|-------------------------|---------------------------------------------------------------------------------------------------------|
| `Simulation`                         | Entity / Aggregate Root | Holds the simulation parameters and `SimulationStatus`; produces a `SimulationReport`                   |
| `SimulationReport`                   | Entity                  | Holds `totalFlights`, `passed`, `generatedAt`; records `SafetyViolation`s and `FlightExecutionStatus`es |
| `FlightExecutionStatus`              | Value Object            | Execution status of one flight (`flightDesignator`, `status`)                                           |
| `SafetyViolation`                    | Value Object            | One safety violation (`timestamp`, position, velocity vector, involved flight)                          |
| `SimulationStatus`                   | Enum                    | `PENDING` / `RUNNING` / `COMPLETED` / `FAILED`                                                          |
| `SimulationRepository`               | Repository Interface    | Persistence contract for the `Simulation` aggregate                                                     |
| `SimulationResultsReader`            | Adapter Interface       | Reads the simulation's raw output and exposes it to the controller                                      |
| `SimulationReportExporter`           | Service                 | Writes the formatted `SimulationReport` to a file (AC111.1)                                             |
| `GenerateSimulationReportController` | Application Controller  | Orchestrates the use case; enforces the FCO role                                                        |
| `GenerateSimulationReportUI`         | UI                      | Lets the FCO trigger report generation and shows the result                                             |

The following domain model excerpt shows the aggregate structure:

![Domain Model](svg/US111-domain-model.svg)

