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



