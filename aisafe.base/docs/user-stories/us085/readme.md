# US085 — Test/Validate Flight Plan

## 1. Context

This US is being implemented in Sprint 3. It allows a Pilot to run a simulation test against
a VALIDATED flight plan, transitioning its status from `VALIDATED` to `TESTED`.

The test component is implemented in C using POSIX APIs (processes, pipes, signals, shared
memory, pthreads, mutexes, condition variables, and semaphores), as required by the SCOMP
subject. The Java application layer invokes the C binary via `ProcessBuilder`, passing the
flight plan data as a temporary JSON file, and processes the result to update the domain.

The `FlightPlan` aggregate already defines the `TESTED` status and the `markTested()` method
(established when US080/US081 modelled the lifecycle). This US completes that lifecycle.

### 1.1 List of issues

- **Analysis:** Define which flight plans are testable (only DSL-based plans with full
  coordinate data), identify the C/Java integration contract, and confirm no new aggregates
  are needed.
- **Design:** Specify the C binary architecture (parent/child/pipe/semaphore/shared-memory
  topology), the JSON exchange format, and the Java controller/UI flow.
- **Implement (C):** Build `flight_tester` binary — fork, pipe, shared memory, semaphore,
  pthread with mutex + condition variable, SIGUSR1 signal handling, JSON result output.
- **Implement (Java):** `FlightPlanJsonSerializer`, `TestFlightPlanController`,
  `TestFlightPlanUI`, `findAllValidated()` on repository, menu integration.
- **Test:** C unit tests for the tester binary; Java unit tests for the controller (mocked
  process); manual end-to-end acceptance tests.

---

## 2. Requirements

**US085** As a Pilot, I want to test/validate a flight plan I've made.

> *(Enunciado, p. 19):* "The validation must include validation of the flight plan described
> using the DSL and its test. The test component must be implemented in the C language."

**Acceptance Criteria:**

| AC | Description |
|----|-------------|
| AC085.1 | Only an authenticated user with the `PILOT` role may trigger a flight plan test. |
| AC085.2 | Only flight plans in `VALIDATED` status may be tested. |
| AC085.3 | Only DSL-based flight plans (those with non-null `dslContent`) can be tested, as they embed the full coordinate/altitude data required by the simulator. |
| AC085.4 | The test component must be implemented in C using POSIX APIs: `fork`, pipes, `shm_open` (shared memory), named semaphores (`sem_open`), `pthread_create`, `pthread_mutex_t`, `pthread_cond_t`, and `SIGUSR1` signal handling. |
| AC085.5 | On a successful test, the flight plan status transitions from `VALIDATED` to `TESTED` and is persisted. |
| AC085.6 | On a failed test, the flight plan status remains `VALIDATED` and the failure reason is reported to the user. |
| AC085.7 | The Java layer may not implement the flight simulation logic — it only invokes the C binary and processes its JSON result. |

**Dependencies/References:**

| Dependency | Reason |
|-----------|--------|
| US030 | Authentication and authorisation (`PILOT` role) must be in place. |
| US081 | Creates DSL-based flight plans (the input for US085). |
| US083 | Validates DSL plans and transitions them to `VALIDATED` — prerequisite state for US085. |
| US086 | US085 must eventually be exposed remotely via the TCP pilot client (`PilotSessionHandler`). Not implemented in the current sprint for the TCP path — the console path is the focus. |

**Out of scope for this user story:**

- Testing form-based flight plans (US080) — they lack route coordinates and cannot be
  simulated by the C tester.
- Multi-flight collision detection — that is US102. This tester validates a single plan in
  isolation.
- Full simulation report with graphical output — that is US114.
- Weather data integration in the simulation — that is US082.
