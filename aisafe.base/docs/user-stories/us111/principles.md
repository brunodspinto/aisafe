# US111 — Principles and Patterns Applied

## 1. DDD — Domain Driven Design

### 1.1 Aggregate Root

`Simulation` is the aggregate root of the `Simulation` aggregate. The report is produced through it — `Simulation.buildReport(results)` assembles and holds the `SimulationReport`, so all state changes pass through the aggregate root.

```java
public class Simulation implements AggregateRoot<SimulationId> {
    public SimulationReport buildReport(final SimulationResults results) { ... }
}
```

---

### 1.2 Entity inside the Aggregate

`SimulationReport` is an entity owned by `Simulation` (`Simulation "1" *-- "1" SimulationReport`). It is never manipulated outside the aggregate boundary — the controller obtains it only through the aggregate root.

---

### 1.3 Value Objects

`FlightExecutionStatus` and `SafetyViolation` are value objects: they are immutable, compared by value, and have no identity of their own. A `SafetyViolation` bundles the violation's `timestamp`, position (`latitude`, `longitude`, `altitude`) and velocity vector (`speed`, `heading`) — exactly the data AC111.3 requires. `SimulationResults` is also an immutable value object that carries the parsed simulation outcome into the aggregate.

---

### 1.4 Reuse of an existing framework (instead of a new Repository)

US111 needs only to produce a *file*, so it does **not** introduce a `Simulation` repository. Instead of an ad-hoc exporter, it **reuses the US112 reporting framework** (`ReportData` / `OperationalReportFormatter` / `ReportWriter`). This keeps US111 small and gives a consistent report structure across the system — a possible `SimulationRepository` for persisting runs is left as a future extension.

---

### 1.5 Low Coupling between Layers (Anti-Corruption boundary)

The simulation is a C program; US111 must not depend on its file format inside the domain or controller. The `SimulationResultsReader` adapter isolates that knowledge: it reads the raw simulation output and exposes a clean `SimulationResults` to the application layer. The domain stays pure Java with no awareness of the C side.

---

## 2. GRASP — General Responsibility Assignment Software Patterns

### 2.1 Information Expert

`Simulation` builds its own `SimulationReport` because it owns the report and the status — the pass/fail rule is derived exactly as the simulation computes it:

```java
public SimulationReport buildReport(final SimulationResults results) {
    this.status = results.status();
    final boolean passed = results.status() == SimulationStatus.COMPLETED
            && results.safetyViolations().isEmpty();
    return new SimulationReport(results.totalFlights(), passed, LocalDateTime.now(),
            results.executionStatuses(), results.safetyViolations());
}
```

`SimulationReport` is the information expert for the report's own invariants (it rejects a negative flight count, a null timestamp, etc.).

---

### 2.2 Controller

`GenerateSimulationReportController` is the application-layer controller. It verifies the FCO role, resolves the operator's area, obtains the results, asks the aggregate to build the report, and writes the file via the reused US112 framework — without containing any parsing or formatting logic.

---

### 2.3 Pure Fabrication

`SimulationResultsReader` (and its implementation `TextSimulationResultsReader`) is a **pure fabrication** — it does not represent a domain concept; it exists to keep file parsing out of the domain, giving high cohesion and low coupling. The report file writing is likewise delegated to the US112 `ReportWriter` (a pure-fabrication service), not to the domain.

---

### 2.4 Low Coupling

- The controller depends on the `SimulationResultsReader` **interface**, not on its concrete adapter.
- The controller reuses the US112 framework classes rather than re-implementing report writing.
- The domain (`Simulation`, `SimulationReport`) has no dependency on files, JPA, or the C simulation.

---

### 2.5 High Cohesion

| Class | Single Focused Responsibility |
|-------|------------------------------|
| `Simulation` | Hold simulation status and build/own its report |
| `SimulationReport` | Hold the report data and its invariants |
| `GenerateSimulationReportController` | Orchestrate the generate-report use case |
| `SimulationResultsReader` / `TextSimulationResultsReader` | Read and parse the simulation output |
| `GenerateSimulationReportUI` | Collect the FCO request and display the result |
| `OperationalReportFormatter` / `ReportWriter` *(US112)* | Format and write the report file |

---

### 2.6 Protected Variations

`SimulationResultsReader` shields the use case from changes to the simulation output format — if the C report format changes, only `TextSimulationResultsReader` changes. Reusing the US112 framework shields US111 from changes to the report file layout — if the branding/structure changes, only the US112 formatter changes. The controller, aggregate and UI are unaffected.

---

## 3. SOLID

### 3.1 Single Responsibility Principle (SRP)

| Class | Single Responsibility |
|-------|----------------------|
| `Simulation` | Hold simulation state and build its report |
| `SimulationReport` | Represent the report |
| `GenerateSimulationReportController` | Orchestrate the use case |
| `TextSimulationResultsReader` | Read/parse the simulation output |
| `GenerateSimulationReportUI` | Present the use case to the FCO |

---

### 3.2 Interface Segregation Principle (ISP)

`SimulationResultsReader` exposes only `read(Path)`. It is a minimal, focused interface, so the controller is offered exactly the one operation it needs and nothing else.

---

### 3.3 Dependency Inversion Principle (DIP)

`GenerateSimulationReportController` depends on the `SimulationResultsReader` **abstraction**; the concrete `TextSimulationResultsReader` is wired in the controller's no-argument constructor (the composition root). The repositories it uses (collaborators, air control areas) are obtained from `PersistenceContext`, which injects the configured implementation.

---

## 4. GoF — Gang of Four Design Patterns

### 4.1 Adapter

`TextSimulationResultsReader` is an **Adapter** between the C simulation's output file (`simulation_report.txt`) and the Java application's `SimulationResults` abstraction — it converts an external text representation into the form the domain expects.

---

### 4.2 Reuse of the US112 reporting framework

The report file is produced by reusing US112's `ReportData` → `OperationalReportFormatter` → `ReportWriter` pipeline. The framework provides a consistent, branded structure for every report type; US111 supplies its own `ReportData` (sections) without re-implementing formatting or file writing. (US111 deliberately does **not** implement the US112 `ReportGenerationStrategy`, whose `generate(YearMonth, AirControlArea, …)` signature is specific to monthly reports.)

---

### 4.3 Factory Method

`PersistenceContext.repositories().collaborators()` and `.airControlAreas()` are factory methods — they return the correct repository implementation (in-memory or JPA) based on runtime configuration, without the controller knowing which one.

---

## 5. Summary Table

| Principle / Pattern | Category | Where in US111 |
|---------------------|----------|----------------|
| Aggregate Root | DDD | `Simulation`; exposes `buildReport()` |
| Entity inside aggregate | DDD | `SimulationReport` owned by `Simulation` |
| Value Object | DDD | `FlightExecutionStatus`, `SafetyViolation`, `SimulationResults` |
| Framework reuse | DDD | US112 `ReportWriter` instead of a new repository/exporter |
| Low Coupling between layers | DDD | `SimulationResultsReader` isolates the C output |
| Information Expert | GRASP | `Simulation` builds its own report |
| Controller | GRASP | `GenerateSimulationReportController` |
| Pure Fabrication | GRASP | `SimulationResultsReader`; US112 `ReportWriter` |
| Low Coupling | GRASP | controller depends on the reader interface; reuses US112 |
| High Cohesion | GRASP | each class has one focused responsibility |
| Protected Variations | GRASP | adapter + framework shield from format changes |
| SRP | SOLID | one reason to change per class |
| ISP | SOLID | minimal `read(Path)` interface |
| DIP | SOLID | controller depends on the reader abstraction |
| Adapter | GoF | `TextSimulationResultsReader` |
| Framework reuse | GoF-ish | US112 `ReportData`/`OperationalReportFormatter`/`ReportWriter` |
| Factory Method | GoF | `PersistenceContext.repositories().collaborators()` / `.airControlAreas()` |
