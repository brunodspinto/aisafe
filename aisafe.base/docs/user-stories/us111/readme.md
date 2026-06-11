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

| Class                                | Type                      | Responsibility                                                                                          |
|--------------------------------------|---------------------------|---------------------------------------------------------------------------------------------------------|
| `Simulation`                         | Entity / Aggregate Root   | Holds the simulation parameters and `SimulationStatus`; produces a `SimulationReport`                   |
| `SimulationReport`                   | Entity                    | Holds `totalFlights`, `passed`, `generatedAt`; records `SafetyViolation`s and `FlightExecutionStatus`es |
| `FlightExecutionStatus`              | Value Object              | Execution status of one flight (`flightDesignator`, `status`)                                           |
| `SafetyViolation`                    | Value Object              | One safety violation (`timestamp`, position, velocity vector, involved flight)                          |
| `SimulationStatus`                   | Enum                      | `PENDING` / `RUNNING` / `COMPLETED` / `FAILED`                                                          |
| `SimulationRepository`               | Repository Interface      | Persistence contract for the `Simulation` aggregate                                                     |
| `SimulationResultsReader`            | Adapter Interface         | Reads the simulation's raw output and exposes it to the controller                                      |
| `SimulationReportExporter`           | Service                   | Writes the formatted `SimulationReport` to a file (AC111.1)                                             |
| `GenerateSimulationReportController` | Application Controller    | Orchestrates the use case; enforces the FCO role                                                        |
| `GenerateSimulationReportUI`         | UI                        | Lets the FCO trigger report generation and shows the result                                             |

The following domain model excerpt shows the aggregate structure:

![Domain Model](svg/US111-domain-model.svg)

---

## 4. Design

### 4.1. Realization

1. The FCO selects "Generate Simulation Report" in the console UI (`GenerateSimulationReportUI`).
2. The UI asks the controller to generate the report for the latest (or selected) simulation run.
3. The controller calls `authz.ensureAuthenticatedUserHasAnyOf(FLIGHT_CONTROL_OPERATOR)`.
4. The controller obtains the simulation results through `SimulationResultsReader` (which parses the simulation output file).
5. The controller builds a `SimulationReport`: total number of flights (AC111.2), one `FlightExecutionStatus` per flight (AC111.2), the `SafetyViolation`s with their timestamps and positions (AC111.3), and the overall `passed` result (AC111.4).
6. The `Simulation` aggregate stores the report and its `SimulationStatus` transitions to `COMPLETED` (or `FAILED`).
7. The controller persists the `Simulation` via `SimulationRepository.save(...)`.
8. The controller exports the report to a file via `SimulationReportExporter` (AC111.1).
9. The UI confirms: `Simulation report generated: <file> — result: PASSED/FAILED`.

The following sequence diagram illustrates the flow:

![Sequence Diagram](svg/US111-SD.svg)

The following class diagram shows the classes involved:

![Class Diagram](svg/US111-class-diagram.svg)

### 4.2. Acceptance Tests

All automated tests and manual acceptance scripts are documented in [tests.md](tests.md).

---

## 5. Implementation

The implementation is distributed across the following packages in `aisafe.base`:

| Package                                      | Class                                | Role                                                                                      |
|----------------------------------------------|--------------------------------------|-------------------------------------------------------------------------------------------|
| `aisafe.simulation.domain`                   | `Simulation`                         | Aggregate root; holds parameters, `SimulationStatus`, and the produced `SimulationReport` |
| `aisafe.simulation.domain`                   | `SimulationReport`                   | Report entity (`totalFlights`, `passed`, `generatedAt`, violations, execution statuses)   |
| `aisafe.simulation.domain`                   | `FlightExecutionStatus`              | Value object — per-flight execution status                                                |
| `aisafe.simulation.domain`                   | `SafetyViolation`                    | Value object — violation with timestamp, position and velocity vector                     |
| `aisafe.simulation.repositories`             | `SimulationRepository`               | Repository interface for the `Simulation` aggregate                                       |
| `aisafe.simulation.application`              | `GenerateSimulationReportController` | Use case orchestrator; enforces the FCO role                                              |
| `aisafe.simulation.application`              | `SimulationResultsReader`            | Interface that yields parsed simulation results to the controller                         |
| `aisafe.simulation.application`              | `SimulationReportExporter`           | Writes the formatted report to a file                                                     |
| `aisafe.infrastructure.persistence.jpa`      | `JpaSimulationRepository`            | JPA persistence for `Simulation`                                                          |
| `aisafe.infrastructure.persistence.inmemory` | `InMemorySimulationRepository`       | In-memory persistence for `Simulation`                                                    |
| `aisafe.infrastructure.simulation`           | `FileSimulationResultsReader`        | Reads/parses the SCOMP simulation output (`simulation_report.txt`)                        |
| `aisafe.app.console.presentation.simulation` | `GenerateSimulationReportUI`         | Console UI for the FCO                                                                    |

The controller orchestrates the use case without containing report-formatting or parsing logic — it delegates to the reader and the exporter:

```java
@UseCaseController
public class GenerateSimulationReportController {

    public SimulationReport generate(final SimulationId simulationId) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.FLIGHT_CONTROL_OPERATOR);

        final SimulationResults results = resultsReader.read(simulationId);   // from the C output
        final Simulation simulation = simulationRepository.ofIdentity(simulationId)
                .orElseThrow(() -> new IllegalArgumentException("Simulation not found."));

        final SimulationReport report = simulation.buildReport(results);      // domain builds the report
        simulationRepository.save(simulation);
        reportExporter.export(report);                                        // AC111.1 — store in file
        return report;
    }
}
```

> The `Simulation.buildReport(...)` method (Information Expert) is responsible for assembling the
> `SimulationReport` from the results — total flights, per-flight `FlightExecutionStatus`,
> `SafetyViolation`s and the `passed` flag — because `Simulation` owns its report.

---

## 6. Integration/Demonstration

**Prerequisites:** Run from the `aisafe.base` directory with Maven 3.9+ and Java 21. A simulation must have been run (SCOMP/C component, US100–US109) so that its results file exists.

**To generate a simulation report:**

1. Login with Flight Control Operator (FCO) credentials (e.g., username: `fco1`, password: `Password1`).
2. Run the flight simulation (SCOMP component) so that the results file is produced.
3. Select **Simulation > Generate Simulation Report** from the main menu.
4. The system reads the simulation results, builds and stores the report, and confirms:
   ```
   Simulation report generated: simulation_report_<id>.txt — result: PASSED
   Total flights: 3 | Safety violations: 0
   ```
5. If safety violations occurred, the report file lists each one with its timestamp and position, and the result is `FAILED`.

---

## 7. Observations

- US111 introduces **no new domain concept** — the `Simulation` aggregate and its `SimulationReport`, `FlightExecutionStatus`, `SafetyViolation` and `SimulationStatus` are already defined in Domain Model V10. US111 only adds the application logic to build, persist and export the report.
- The report's data comes from the **SCOMP/C simulation** (US100–US109), not from a Java re-simulation. US111 consumes the simulation output through the `SimulationResultsReader` adapter, keeping the Java domain decoupled from the C implementation.
- US109 (SCOMP) and US111 (EAPLI) are **complementary**: US109 is the C program's own end-of-run report; US111 is the FCO-facing report produced and persisted by the main application from the same results.
- The pass/fail result (AC111.4) is derived from the presence/absence of safety violations and the simulation's terminal status (`COMPLETED` vs `FAILED`/aborted by threshold).
- **Status:** this document describes the analysis and design of US111. The Implementation and Tests sections describe the intended structure; they must be kept aligned with the code as it is built.
