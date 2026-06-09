# US085 — Principles and Patterns Applied

## 1. DDD — Domain Driven Design

### 1.1 Aggregate Root

`FlightPlan` is the aggregate root. US085 only adds one query method to the repository and
calls an existing aggregate method (`markTested()`). No new aggregates or value objects are
introduced.

```java
public class FlightPlan implements AggregateRoot<FlightPlanDesignator> {
    public void markTested() {
        if (this.status != FlightPlanStatus.VALIDATED)
            throw new IllegalStateException("Cannot mark a flight plan as tested unless it is in VALIDATED status.");
        this.status = FlightPlanStatus.TESTED;
    }
}
```

---

### 1.2 Status Lifecycle as Domain Invariant

`markTested()` enforces the lifecycle invariant — only a `VALIDATED` plan may advance to
`TESTED`. If the guard fails, an `IllegalStateException` is thrown, keeping the invariant
inside the aggregate where it belongs.

```java
if (this.status != FlightPlanStatus.VALIDATED)
    throw new IllegalStateException(...);
this.status = FlightPlanStatus.TESTED;
```

This is consistent with `markValidated()`, which similarly guards against being called on
anything other than a `DRAFT` plan.

---

### 1.3 Repository Extension

`FlightPlanRepository` gains `findAllValidated()`, a domain-language query that expresses
intent clearly. The application layer uses the repository interface; neither the controller
nor the UI knows whether the store is JPA or in-memory.

```java
Iterable<FlightPlan> findAllValidated();
```

---

### 1.4 Low Coupling Between Aggregates

`TestFlightPlanController` does not navigate from `FlightPlan` to `Pilot` or `Aircraft`
during the test — it only reads `dslContent` from the plan and invokes the C binary. No
additional aggregates are loaded, and no cross-aggregate invariants are checked at this step
(the plan was already validated in US083).

---

## 2. GRASP — General Responsibility Assignment Software Patterns

### 2.1 Information Expert

`FlightPlan` owns `status` and is therefore the expert for the `markTested()` guard.
`FlightPlanJsonSerializer` owns the knowledge of how to convert a `FlightPlanAst` to JSON;
it is the expert for that serialisation format.

---

### 2.2 Controller

`TestFlightPlanController` is the application-layer controller for US085. It:
- Verifies authorization (`PILOT` role).
- Delegates listing to `findAllValidated()`.
- Guards status and DSL content before invoking the C binary.
- Re-parses `dslContent` via `FlightPlanParserFacade`.
- Serialises the AST via `FlightPlanJsonSerializer`.
- Invokes the C binary via `ProcessBuilder`.
- Interprets the JSON result and calls `markTested()` + `repo.save()` on PASS.

```java
@UseCaseController
public class TestFlightPlanController { ... }
```

---

### 2.3 Creator

`FlightPlanJsonSerializer` creates the temporary JSON file because it aggregates all the
data needed (the `FlightPlanAst`) and knows the target format (the C binary's JSON
contract). The controller delegates creation to it rather than doing the serialisation
inline.

---

### 2.4 Low Coupling

- The controller depends on `FlightPlanRepository` (interface), `FlightPlanParserFacade`,
  `FlightPlanJsonSerializer`, and `AppSettings` — no concrete persistence classes.
- The C binary is invoked by name; Java has no compile-time dependency on C code.
- `FlightPlanJsonSerializer` depends only on `FlightPlanAst`, `LegAst`, `SegmentAst`, and
  `CoordinateAst` — pure DSL package types, no domain or infrastructure imports.

---

### 2.5 High Cohesion

| Class | Single Focus |
|-------|-------------|
| `FlightPlanJsonSerializer` | Convert `FlightPlanAst` to a temp JSON file |
| `TestFlightPlanController` | Orchestrate the test flight plan use case |
| `TestFlightPlanUI` | Collect pilot selection and display test result |
| `AppSettings` | Load application configuration properties |

---

### 2.6 Protected Variations

- `FlightPlanRepository` as an interface protects the controller from persistence changes.
- `AppSettings.flightTesterBinary()` decouples the binary path from compiled Java — the
  path can change per environment without recompiling.
- The sentinel `VALIDATED` guard in `markTested()` protects against re-testing an already
  `TESTED` plan.

---

## 3. SOLID

### 3.1 Single Responsibility Principle (SRP)

| Class | Responsibility |
|-------|---------------|
| `FlightPlan.markTested()` | Enforce the VALIDATED→TESTED lifecycle transition |
| `FlightPlanJsonSerializer` | Serialise a `FlightPlanAst` to a temp JSON file |
| `TestFlightPlanController` | Orchestrate auth, list, invoke binary, update status |
| `TestFlightPlanUI` | Console I/O for the test flight plan use case |
| `AppSettings.flightTesterBinary()` | Read the C binary path from configuration |
| `JpaFlightPlanRepository.findAllValidated()` | Query VALIDATED plans in the database |
| `InMemoryFlightPlanRepository.findAllValidated()` | Query VALIDATED plans in memory |

---

### 3.2 Open/Closed Principle (OCP)

Adding US085 did not require modifying the `FlightPlan` aggregate (the `markTested()`
method was already there). The repository interface was extended with one new method
following the open/closed principle — existing clients of `FlightPlanRepository` are
unaffected.

---

### 3.3 Interface Segregation Principle (ISP)

`FlightPlanRepository` exposes only what this use case needs: `findAllValidated()`,
`ofIdentity()`, and `save()`. No unrelated methods are forced on clients.

---

### 3.4 Dependency Inversion Principle (DIP)

`TestFlightPlanController` depends on the `FlightPlanRepository` interface, not on
`JpaFlightPlanRepository` or `InMemoryFlightPlanRepository`. The concrete implementation
is injected at runtime by `PersistenceContext`.

```java
private final FlightPlanRepository repository =
        PersistenceContext.repositories().flightPlans();
```

---

## 4. GoF — Gang of Four Design Patterns

### 4.1 Facade

`TestFlightPlanController` acts as a Facade — it hides the complexity of authorization,
DSL re-parsing, JSON serialisation, process invocation, output parsing, and domain status
update behind a single `testFlightPlan(designator)` method.

---

### 4.2 Factory Method

`PersistenceContext.repositories().flightPlans()` is a factory method that returns the
correct `FlightPlanRepository` implementation (JPA or in-memory) based on the runtime
configuration, without the controller knowing which one.

`Files.createTempFile("aisafe-fp-", ".json")` is the standard library's factory for
creating temporary files.

---

### 4.3 Template Method (via EAPLI)

`JpaFlightPlanRepository` extends `JpaAutoTxRepository`, which provides the algorithm
skeleton for `save()`, `ofIdentity()`, `findAll()`, and `match()`. The new
`findAllValidated()` override calls the inherited `match()` template slot.

```java
@Override
public Iterable<FlightPlan> findAllValidated() {
    final Map<String, Object> params = new HashMap<>();
    params.put("status", FlightPlanStatus.VALIDATED);
    return match("e.status = :status", params);
}
```

---

### 4.4 Strategy (C/Java boundary)

The C binary is selected at runtime via `AppSettings.flightTesterBinary()`. Swapping in a
different tester binary (e.g., a mock for testing, or a production-tuned binary) requires
only a property change, not a code change — consistent with the Strategy pattern for
algorithm substitution.

---

## 5. Summary Table

| Principle / Pattern | Category | Where in US085 |
|---------------------|----------|----------------|
| Aggregate Root | DDD | `FlightPlan.markTested()` enforces VALIDATED→TESTED |
| Status Lifecycle Invariant | DDD | `markTested()` guards against invalid state |
| Repository Extension | DDD | `findAllValidated()` added to interface |
| Low Coupling | DDD | Controller reads `dslContent`; no extra aggregates loaded |
| Information Expert | GRASP | `FlightPlan` owns status; `FlightPlanJsonSerializer` owns format |
| Controller | GRASP | `TestFlightPlanController` orchestrates the use case |
| Creator | GRASP | `FlightPlanJsonSerializer` creates the temp JSON file |
| Low Coupling | GRASP | Repository interface; C binary path from config |
| High Cohesion | GRASP | Each class has a single, focused responsibility |
| Protected Variations | GRASP | Repository interface; `flightTesterBinary()` config; `markTested()` guard |
| SRP | SOLID | Each class has one reason to change |
| OCP | SOLID | `FlightPlan` unchanged; repository extended with new method |
| ISP | SOLID | `FlightPlanRepository` exposes only needed methods |
| DIP | SOLID | Controller depends on repository interface |
| Facade | GoF | `TestFlightPlanController` hides use case complexity |
| Factory Method | GoF | `PersistenceContext.repositories().flightPlans()` |
| Template Method | GoF | `JpaAutoTxRepository.match()` provides query skeleton |
| Strategy | GoF | C binary path configurable via `AppSettings` |
