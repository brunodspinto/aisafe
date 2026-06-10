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

`FlightExecutionStatus` and `SafetyViolation` are value objects: they are immutable, compared by value, and have no identity of their own. A `SafetyViolation` bundles the violation's `timestamp`, position (`latitude`, `longitude`, `altitude`) and velocity vector (`speed`, `heading`) — exactly the data AC111.3 requires.

---

### 1.4 Repository

`SimulationRepository` is the domain interface for persisting and retrieving the `Simulation` aggregate. The controller depends only on this interface, never on a concrete JPA or in-memory implementation.

```java
public interface SimulationRepository extends DomainRepository<SimulationId, Simulation> {
    Iterable<Simulation> findByStatus(SimulationStatus status);
}
```

---

### 1.5 Low Coupling between Layers (Anti-Corruption boundary)

The simulation is a C program; US111 must not depend on its file format inside the domain or controller. The `SimulationResultsReader` adapter isolates that knowledge: it reads the raw simulation output and exposes a clean `SimulationResults` to the application layer. The domain stays pure Java with no awareness of the C side.

---

## 2. GRASP — General Responsibility Assignment Software Patterns

### 2.1 Information Expert

`Simulation` builds its own `SimulationReport` because it owns the report and the status:

```java
public SimulationReport buildReport(final SimulationResults results) {
    final boolean passed = results.violations().isEmpty()
            && this.status == SimulationStatus.COMPLETED;
    return new SimulationReport(results.totalFlights(), passed,
            results.executionStatuses(), results.violations());
}
```

`SimulationReport` is the information expert for the report's own invariants (a report cannot have a negative flight count, etc.).

---

### 2.2 Controller

`GenerateSimulationReportController` is the application-layer controller. It verifies the FCO role, obtains the results, asks the aggregate to build the report, persists it and triggers the export — without containing any parsing or formatting logic.

---

### 2.3 Pure Fabrication

`SimulationResultsReader` and `SimulationReportExporter` are **pure fabrications** — they do not represent domain concepts; they exist to keep file parsing and file writing out of the domain, giving high cohesion and low coupling.

---

### 2.4 Low Coupling

- The controller depends on the `SimulationRepository`, `SimulationResultsReader` and `SimulationReportExporter` **interfaces** — not on their implementations.
- The domain (`Simulation`, `SimulationReport`) has no dependency on files, JPA, or the C simulation.

---

### 2.5 High Cohesion

| Class | Single Focused Responsibility |
|-------|------------------------------|
| `Simulation` | Hold simulation data and build/own its report |
| `SimulationReport` | Hold the report data and its invariants |
| `GenerateSimulationReportController` | Orchestrate the generate-report use case |
| `SimulationResultsReader` | Parse the simulation output |
| `SimulationReportExporter` | Write the formatted report to a file |
| `GenerateSimulationReportUI` | Collect the FCO request and display the result |

---

### 2.6 Protected Variations

`SimulationResultsReader` shields the use case from changes to the simulation output format; `SimulationReportExporter` shields it from changes to the report file format. If either format changes, only the respective adapter changes — the controller, the aggregate and the UI are unaffected.

---

## 3. SOLID

### 3.1 Single Responsibility Principle (SRP)

| Class | Single Responsibility |
|-------|----------------------|
| `Simulation` | Hold simulation state and build its report |
| `SimulationReport` | Represent the report |
| `GenerateSimulationReportController` | Orchestrate the use case |
| `SimulationResultsReader` | Read/parse the simulation output |
| `SimulationReportExporter` | Export the report to a file |
| `GenerateSimulationReportUI` | Present the use case to the FCO |

---

### 3.2 Interface Segregation Principle (ISP)

`SimulationResultsReader` exposes only `read(simulationId)`, and `SimulationReportExporter` only `export(report)`. Each interface is minimal and focused, so the controller is offered exactly the operations it needs.

---

### 3.3 Dependency Inversion Principle (DIP)

`GenerateSimulationReportController` depends on abstractions (`SimulationRepository`, `SimulationResultsReader`, `SimulationReportExporter`); the concrete `JpaSimulationRepository`, `FileSimulationResultsReader` and file exporter are injected at runtime. High-level policy does not depend on low-level detail.

---

## 4. GoF — Gang of Four Design Patterns

### 4.1 Adapter

`FileSimulationResultsReader` is an **Adapter** between the C simulation's output file and the Java application's `SimulationResults` abstraction — it converts an external representation into the form the domain expects.

---

### 4.2 Strategy

`SimulationReportExporter` is a **Strategy** for writing the report: a plain-text exporter today, but a different format (PDF, HTML) could be added without touching the controller or the domain — the same way US112 envisions multiple report formats with consistent structure.

---

### 4.3 Factory Method

`PersistenceContext.repositories().simulations()` is a factory method that returns the correct `SimulationRepository` implementation (in-memory or JPA) based on runtime configuration.

---

## 5. Summary Table

| Principle / Pattern | Category | Where in US111 |
|---------------------|----------|----------------|
| Aggregate Root | DDD | `Simulation`; exposes `buildReport()` |
| Entity inside aggregate | DDD | `SimulationReport` owned by `Simulation` |
| Value Object | DDD | `FlightExecutionStatus`, `SafetyViolation` |
| Repository | DDD | `SimulationRepository` |
| Low Coupling between layers | DDD | `SimulationResultsReader` isolates the C output |
| Information Expert | GRASP | `Simulation` builds its own report |
| Controller | GRASP | `GenerateSimulationReportController` |
| Pure Fabrication | GRASP | `SimulationResultsReader`, `SimulationReportExporter` |
| Low Coupling | GRASP | controller depends on interfaces only |
| High Cohesion | GRASP | each class has one focused responsibility |
| Protected Variations | GRASP | adapters shield from format changes |
| SRP | SOLID | one reason to change per class |
| ISP | SOLID | minimal reader/exporter interfaces |
| DIP | SOLID | controller depends on abstractions |
| Adapter | GoF | `FileSimulationResultsReader` |
| Strategy | GoF | `SimulationReportExporter` |
| Factory Method | GoF | `PersistenceContext.repositories().simulations()` |
