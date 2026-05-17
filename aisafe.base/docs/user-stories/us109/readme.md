# US109 — Generate and Store Final Simulation Report

## 1. Context

US109 is the concluding phase of the Sprint 2 flight simulation pipeline. Once the active
simulation ends (either naturally when all flights complete their routes, or prematurely
due to critical safety violations from US102), the system must aggregate all captured data
and persist it into a final report.

The implementation is written in C and lives entirely under `aisafe.base/simulation/`. It
adheres strictly to the Sprint 2 constraints defined by the course coordination, specifically
adapting the concurrency model for report generation.

---

## 2. Requirements

**US109:** As a Flight Control Operator, I want a comprehensive report that details the
simulation outcomes — including flight execution statuses, safety violation events (with
timestamps, positions and velocity vectors), and overall validation results —, so that I
can assess the safety and performance of the flights post-simulation.

### Acceptance Criteria

| ID | Criterion | Status |
|----|-----------|--------|
| AC1 | The system must aggregate all flight data only after the main simulation loop concludes | Done |
| AC2 | The report includes the total number of flights, individual execution statuses, and detailed safety violation events | Done |
| AC3 | **Course Specific:** The report generation must be executed by a dedicated **process** (not a thread) during Sprint 2 | Done |
| AC4 | The final validation result (pass/fail) is clearly indicated and the complete report is saved to a file for future reference | Done |

---

## 3. Analysis

### 3.1 The "Thread vs. Process" Constraint

The original project requirements specify: *"The report generation thread aggregates data
once the simulation concludes."* However, following the explicit clarification from Professor
Luís Nogueira on the SCOMP Moodle forum:

> *"na US109, onde se lê 'The report generation thread...', deve ser interpretado no Sprint 2
> como 'process'. Só no próximo Sprint [...] deverão ser usadas threads."*

Consequently, the architecture delegates the report generation to a **child process** created
via `fork()` after the simulation ends, rather than using `pthread_create()`.

### 3.2 Data Availability and Memory Copy

The parent process (Controller) spends the entire simulation populating the
`flight_history_t histories[]` array and tracking `total_violations`. Because the report
generator is spawned via `fork()` at the *end* of the simulation, the OS's Copy-on-Write
(COW) semantics provide the new child process with a perfect, fully populated replica of
the parent's memory space.

This completely eliminates the need for Pipes or Shared Memory to transfer the history
arrays to the reporting process — a design that would otherwise require serialising complex
nested structs across an IPC boundary.

### 3.3 Report Content Structure

The final report provides both a high-level summary and per-flight detail:

1. **Simulation Status:** Natural completion (`COMPLETED`) vs. aborted (`ABORTED — MAX_VIOLATIONS`).
2. **Global Metrics:** Total flights, total positions captured, total safety violations.
3. **Per-Flight Data:** Trajectory snapshots (lat/lon/alt/spd/hdg), ACA entry/exit state, and
   individual safety violation events with timestamps and velocity vectors.

---

## 4. Design

### 4.1 Process Architecture (End-of-Simulation Phase)

```
                        ┌──────────────┐
                        │    PARENT    │
                        │  (main.c)   │
                        └──────┬───────┘
                               │ (After while loop & waitpid for flights)
                               │
                      fork()   │
                ┌──────────────┴──────────────┐
                │                             │
        ┌───────▼──────┐               ┌──────▼──────┐
        │ REPORT PROC. │               │   PARENT    │
        │ (child_pid)  │               │             │
        │ writes file  │               │ wait(child) │
        │ exit(0)      │               │ exit(0)     │
        └──────────────┘               └─────────────┘
```

The parent blocks on `waitpid(report_pid, …, 0)` before releasing heap memory via
`free_flight_plan()`. This prevents the parent from destroying its data structures before
the child has finished reading them.

### 4.2 Data Flow and File I/O

The reporting process uses buffered standard C file I/O (`fopen`, `fprintf`, `fclose`)
rather than unbuffered POSIX I/O (`open`, `write`).

| I/O choice | Reason |
|------------|--------|
| `fprintf` | Safe in a sequential process context; far better suited to formatting tabular float/string data than raw `write()` |
| `write()` | Required only inside signal handlers (US102), where async-signal-safety rules apply; not relevant here |
| `"w"` open mode | Truncates the file on each run, ensuring the report always reflects the latest simulation |

---

## 5. Implementation — Files Modified

### `simulation/report.h`

* **New** — Public interface defining the `generate_final_report` function signature:

```c
void generate_final_report(const flight_history_t *histories, int n_flights,
                           int total_violations, int aborted);
```

### `simulation/report.c`

* **New** — Iterates through `flight_history_t` and writes formatted output to
  `simulation_report.txt`.
* Opens the file in `"w"` (write/truncate) mode to ensure fresh data per run.
* Writes per-flight trajectory rows including timestamp, lat, lon, alt, speed, heading,
  and vertical rate — covering the enunciado's requirement for velocity vectors in safety
  violation event logs.

### `simulation/main.c`

* **Modified** — Added the final `fork()` block after the `waitpid()` loop that collects
  the flight processes.
* Parent calls `waitpid(report_pid, …, 0)` specifically for the report process before
  calling `free_flight_plan`, preventing premature heap release.

### `libs/scripts/build_c.sh`

* Included `report.c` in the compilation list.

---

## 6. Integration / Demonstration

### Build

```bash
cd aisafe.base/libs/scripts
./build_c.sh
```

Expected output:
```
[INFO] Compiling C components...
[SUCCESS] C components built in .../aisafe.base/bin/
```

Zero compiler warnings (enforced by `-Wall -Wextra`).

### Run

```bash
../../bin/simulation
```

### Expected Console Output (End of Simulation)

```
[FLIGHT_01] ended with code 0
[FLIGHT_02] ended with code 0
[FLIGHT_03] ended with code 0

[SYSTEM] Simulation concluded. Spawning report generation process...
[REPORT] Report successfully saved to 'simulation_report.txt'.
[SYSTEM] Shutting down gracefully.
```

### Expected File Output (`simulation_report.txt`)

```
==================================================
        FLIGHT SIMULATION FINAL REPORT
==================================================
Simulation Status: COMPLETED
Total Safety Violations Detected: 0
Total Flights Tracked: 3

--------------------------------------------------
FLIGHT ID: FLIGHT_01
ACA Status: Exited ACA
Total Positions Logged: 25
Route Log:
  T+0:  lat=41.2629 lon=-8.6852 alt=69m   spd=250kt hdg=42.5 vz=12.0m/s
  ...
  T+24: lat=41.9800 lon=-8.1000 alt=9249m spd=460kt hdg=42.5 vz=0.0m/s

--------------------------------------------------
FLIGHT ID: FLIGHT_02
ACA Status: Exited ACA
...

--------------------------------------------------
FLIGHT ID: FLIGHT_03
ACA Status: Exited ACA
...
```

In the collision scenario (`--collision`), the header reads:

```
Simulation Status: ABORTED — MAX_VIOLATIONS REACHED
Total Safety Violations Detected: 3
```

---

## 7. Key Design Decisions

| Decision | Reason |
|----------|--------|
| **Process instead of Thread** | Strict compliance with the course coordinator's directive for Sprint 2. Threads will replace this process in Sprint 3 (US106). |
| **Forking AFTER the simulation loop** | Leverages OS Copy-on-Write (COW) semantics: the child inherits a fully populated `histories` array instantly, avoiding complex IPC (Shared Memory or massive pipe writes) entirely. |
| **`fprintf` instead of `write`** | Unlike the signal handlers in US102 which required `write()` due to async-signal-safety, the report process is a standard sequential program. `fprintf` is safe and far better suited to formatting tabular data. |
| **Parent waits for the Report Process** | Prevents the parent from destroying heap allocations (`free_flight_plan`) or exiting before the report is safely on disk, which would orphan the reporting process. |
| **`"w"` open mode for the report file** | Ensures every simulation run produces a fresh, authoritative report rather than appending stale data from previous runs. |
