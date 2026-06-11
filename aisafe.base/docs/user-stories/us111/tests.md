# US111 — Tests and Coverage

## Scope

US111 covers generating a Flight Control Operator report from a flight simulation's results. The
report records the total number of flights and their execution statuses (AC111.2), the safety
violations with timestamps and positions (AC111.3), and the overall pass/fail result (AC111.4), and
is written to a file (AC111.1).

The automated tests cover the **domain** (the `Simulation` aggregate building a correct
`SimulationReport`, and the value-object invariants) and the **parser** (`TextSimulationResultsReader`
reading the C `simulation_report.txt`). The file export (reused US112 framework) and the FCO
authorization are validated by manual acceptance tests.

## Automated Tests

### `SimulationReportTest`

Location: `src/test/java/aisafe/simulation/domain/SimulationReportTest.java`

> Helper: `results(SimulationStatus status, int flights, int violations)` builds a
> `SimulationResults`; `simulation()` builds a `Simulation`. The pass/fail rule is derived from the
> results: PASS iff `COMPLETED` and no violations.

- `ensureReportCountsTotalFlights` (AC111.2) — total flights is carried into the report.
- `ensureReportHasOneExecutionStatusPerFlight` (AC111.2) — one `FlightExecutionStatus` per flight.
- `ensureReportPassesWhenCompletedAndNoViolations` (AC111.4) — `COMPLETED` + 0 violations ⇒ passed.
- `ensureReportFailsWhenViolationsExist` (AC111.3 + AC111.4) — violations ⇒ failed, and they are kept.
- `ensureReportFailsWhenSimulationAborted` (AC111.4) — `FAILED` status ⇒ not passed.
- `ensureBuildReportSetsSimulationStatusFromResults` — the aggregate adopts the results' status.
- `ensureBuildReportWithNullResultsThrows` — null results are rejected.

```java
@Test
void ensureReportFailsWhenViolationsExist() {
    final SimulationReport report = simulation().buildReport(results(SimulationStatus.COMPLETED, 3, 2));
    assertFalse(report.passed());
    assertEquals(2, report.safetyViolations().size());
}
```

---

### `SafetyViolationTest`

Location: `src/test/java/aisafe/simulation/domain/SafetyViolationTest.java`

- `ensureViolationCarriesTimestampAndPosition` (AC111.3) — timestamp, lat, lon, alt are preserved.
- `ensureTwoViolationsWithSameValuesAreEqual` — value-object equality and hash code.
- `ensureViolationsWithDifferentPositionAreNotEqual` — different position ⇒ not equal.
- `ensureBlankFlightDesignatorIsRejected`, `ensureNullTimestampIsRejected` — invariants.

---

### `FlightExecutionStatusTest`

Location: `src/test/java/aisafe/simulation/domain/FlightExecutionStatusTest.java`

- `ensureExecutionStatusKeepsDesignatorAndStatus` (AC111.2)
- `ensureTwoWithSameValuesAreEqual`, `ensureDifferentStatusAreNotEqual`
- `ensureBlankDesignatorIsRejected`, `ensureBlankStatusIsRejected`

---

### `TextSimulationResultsReaderTest`

Location: `src/test/java/aisafe/infrastructure/simulation/TextSimulationResultsReaderTest.java`

Parses sample report files under `src/test/resources/simulation/`, so it does **not** require running
the simulation. Verifies the Option A integration: parsing the real C `simulation_report.txt` format.

- `ensureAbortedReportIsParsed` — status `FAILED`, 3 flights, 3 execution statuses, and **2**
  `SafetyViolation`s (one C event = a pair = two domain violations).
- `ensureViolationCarriesTimestampAndPosition` (AC111.3) — the parsed violation has the right flight,
  timestamp and position; the pair maps to `FLIGHT_01` and `FLIGHT_02`.
- `ensureCleanReportIsParsed` — status `COMPLETED`, 2 flights, no violations.
- `ensureParsedResultsBuildACoherentReport` — integration: parser → `Simulation.buildReport` →
  correct `passed` and counts.

```java
@Test
void ensureAbortedReportIsParsed() throws IOException {
    final SimulationResults r = reader.parse(resource("/simulation/sample_simulation_report.txt"));
    assertEquals(SimulationStatus.FAILED, r.status());
    assertEquals(3, r.totalFlights());
    assertEquals(2, r.safetyViolations().size());   // one C event (a pair) -> two domain violations
}
```

---

## Coverage by Acceptance Criterion

- **AC111.1** (generate + store in file): the file is written by the reused US112 `ReportWriter`;
  validated by the manual acceptance test (the report file is produced in `target/reports/`).
- **AC111.2** (total flights + execution status): `ensureReportCountsTotalFlights`,
  `ensureReportHasOneExecutionStatusPerFlight`, `ensureExecutionStatusKeepsDesignatorAndStatus`,
  `ensureAbortedReportIsParsed`.
- **AC111.3** (violations with timestamp/position): `ensureReportFailsWhenViolationsExist`,
  `SafetyViolationTest.ensureViolationCarriesTimestampAndPosition`,
  `TextSimulationResultsReaderTest.ensureViolationCarriesTimestampAndPosition`.
- **AC111.4** (pass/fail): `ensureReportPassesWhenCompletedAndNoViolations`,
  `ensureReportFailsWhenViolationsExist`, `ensureReportFailsWhenSimulationAborted`.
- **Authorization** (FCO role): manual test — only a Flight Control Operator may generate the report.

---

## Acceptance Tests

Role enforcement (FCO) and the actual file export are validated by manual integration testing. Domain
report-assembly and parsing are fully covered by the automated tests above.

**Prerequisites:** a `simulation_report.txt` must exist at the configured path. Run the SCOMP/C
simulation, or copy a sample, e.g.
`copy src\test\resources\simulation\sample_simulation_report.txt simulation_report.txt`.

**Manual test — AC111.1 / AC111.2 / AC111.4 (report with no violations):**

1. Run `AiSafeConsoleApp` and login as a Flight Control Operator (`fco1` / `Password1`).
2. Use a clean simulation result (e.g. copy `sample_simulation_report_clean.txt` to `simulation_report.txt`).
3. Navigate to `Reports > Generate Simulation Report`.
4. Expected: the UI prints `Result : PASSED`, the total flights and the file path, and the file exists
   in `target/reports/` with one execution status per flight.

**Manual test — AC111.3 / AC111.4 (report with violations):**

1. Use a colliding simulation result (e.g. `sample_simulation_report.txt`).
2. Generate the report.
3. Expected: the UI prints `Result : FAILED`; the report file lists each safety violation with its
   timestamp and position.

**Manual test — Authorization (non-FCO):**

1. Login as a user without the Flight Control Operator role.
2. Expected: the Reports menu / Generate Simulation Report option is not available.
