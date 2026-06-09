# US111 — Tests and Coverage

## Scope

US111 covers generating a Flight Control Operator report from a flight simulation's results. The
report records the total number of flights, each flight's execution status, the safety violations
(with timestamps and positions), and the overall pass/fail result, and is stored in a file.

The automated tests focus on the **domain** (the `Simulation` aggregate building a correct
`SimulationReport`, and the value-object invariants) and on the **report assembly logic**, using a
fake `SimulationResultsReader` so no real simulation or file is required. The file export and the
FCO authorization are validated by manual acceptance tests.

> Note: the snippets below describe the planned tests; align the exact names and signatures with
> the code as US111 is implemented.

## Automated Tests

### `SimulationReportTest`

Location: `src/test/java/aisafe/simulation/domain/SimulationReportTest.java`

> Helper: `validResults(int flights, int violations)` builds a `SimulationResults` with the given
> number of flights and violations.

**Test:** `ensureReportCountsTotalFlights` (AC111.2)

```java
@Test
void ensureReportCountsTotalFlights() {
    final SimulationReport report = simulation.buildReport(validResults(3, 0));
    assertEquals(3, report.totalFlights());
}
```

**Test:** `ensureReportHasOneExecutionStatusPerFlight` (AC111.2)

```java
@Test
void ensureReportHasOneExecutionStatusPerFlight() {
    final SimulationReport report = simulation.buildReport(validResults(3, 0));
    assertEquals(3, report.executionStatuses().size());
}
```

**Test:** `ensureReportPassesWhenNoViolationsAndCompleted` (AC111.4)

```java
@Test
void ensureReportPassesWhenNoViolationsAndCompleted() {
    final SimulationReport report = completedSimulation.buildReport(validResults(3, 0));
    assertTrue(report.passed());
}
```

**Test:** `ensureReportFailsWhenViolationsExist` (AC111.3 + AC111.4)

```java
@Test
void ensureReportFailsWhenViolationsExist() {
    final SimulationReport report = completedSimulation.buildReport(validResults(3, 2));
    assertFalse(report.passed());
    assertEquals(2, report.safetyViolations().size());
}
```

**Test:** `ensureReportFailsWhenSimulationAborted` (AC111.4)

```java
@Test
void ensureReportFailsWhenSimulationAborted() {
    final SimulationReport report = abortedSimulation.buildReport(validResults(3, 0));
    assertFalse(report.passed());
}
```

---

### `SafetyViolationTest`

Location: `src/test/java/aisafe/simulation/domain/SafetyViolationTest.java`

**Test:** `ensureViolationCarriesTimestampAndPosition` (AC111.3)

```java
@Test
void ensureViolationCarriesTimestampAndPosition() {
    final SafetyViolation v = new SafetyViolation("proximity", "TP100",
            LocalDateTime.of(2026, 6, 1, 12, 0), 250.0, 42.5, 41.2, -8.6, 9000.0);
    assertEquals(LocalDateTime.of(2026, 6, 1, 12, 0), v.timestamp());
    assertEquals(41.2, v.latitude());
    assertEquals(-8.6, v.longitude());
    assertEquals(9000.0, v.altitude());
}
```

**Test:** `ensureTwoViolationsWithSameValuesAreEqual`

```java
@Test
void ensureTwoViolationsWithSameValuesAreEqual() {
    assertEquals(violation(), violation());
}
```

---

### `FlightExecutionStatusTest`

Location: `src/test/java/aisafe/simulation/domain/FlightExecutionStatusTest.java`

**Test:** `ensureExecutionStatusKeepsDesignatorAndStatus` (AC111.2)

```java
@Test
void ensureExecutionStatusKeepsDesignatorAndStatus() {
    final FlightExecutionStatus s = new FlightExecutionStatus("TP100", "COMPLETED");
    assertEquals("TP100", s.flightDesignator());
    assertEquals("COMPLETED", s.status());
}
```

---

## Coverage by Acceptance Criterion

- **AC111.1** (generate + store in file): `SimulationReportExporter` writing — validated by manual acceptance test (the report file is produced and contains the expected content)
- **AC111.2** (total flights + execution status): `ensureReportCountsTotalFlights`, `ensureReportHasOneExecutionStatusPerFlight`, `ensureExecutionStatusKeepsDesignatorAndStatus`
- **AC111.3** (violations with timestamp/position): `ensureReportFailsWhenViolationsExist`, `ensureViolationCarriesTimestampAndPosition`
- **AC111.4** (pass/fail): `ensureReportPassesWhenNoViolationsAndCompleted`, `ensureReportFailsWhenViolationsExist`, `ensureReportFailsWhenSimulationAborted`
- **Authorization** (FCO role): manual test — only a Flight Control Operator may generate the report

---

## Acceptance Tests

Role enforcement (FCO), the actual file export, and the parsing of the real simulation output are
infrastructure concerns validated by manual integration testing. Domain report-assembly is fully
covered by the automated unit tests above.

**Manual test — AC111.1 / AC111.2 / AC111.4 (successful report, no violations):**

1. Run `AiSafeApp` and login as a Flight Control Operator (e.g., `fco1` / `Password1`).
2. Run the flight simulation (SCOMP component) with a non-colliding scenario.
3. Navigate to `Simulation > Generate Simulation Report`.
4. Expected: the system confirms `Simulation report generated: ... — result: PASSED`, the report file
   exists, and it lists the total flights and one execution status per flight.

**Manual test — AC111.3 / AC111.4 (report with violations):**

1. Run the simulation with a colliding scenario (so safety violations occur).
2. Generate the report.
3. Expected: the result is `FAILED`, and the report file lists each safety violation with its
   timestamp and position.

**Manual test — Authorization (non-FCO):**

1. Login as a user without the Flight Control Operator role.
2. Expected: the Generate Simulation Report option is not available / the action is rejected.
