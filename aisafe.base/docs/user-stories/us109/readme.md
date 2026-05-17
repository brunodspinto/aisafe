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
| AC2 | The report includes the total number of flights, individual execution statuses (ACA entry/exit), and total safety violations count. Individual violation events (pair, timestamp, position) are logged to stdout during the simulation by US102. | Done |
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

1. **Simulation Status:** Natural completion (`COMPLETED (Normal)`) vs. aborted (`ABORTED (Threshold Reached)`).
2. **Global Metrics:** Total flights tracked, total safety violations detected.
3. **Per-Flight Data:** ACA entry/exit state, position count, and trajectory snapshots
   with lat/lon/alt/speed/heading/vz (velocity vector) for every position logged inside the ACA.

> **Note:** Individual safety violation events (which pair violated, at what exact timestamp
> and position) are emitted to stdout in real time by `safety_monitor.c` (US102) during the
> simulation. They are not stored in `flight_history_t` and therefore not repeated in the
> file report. `total_violations` is the only aggregate carried into the report.

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

* **No changes required** — the script compiles all `.c` files via `"$C_DIR"/*.c` glob,
  so `report.c` is picked up automatically.

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
[SYSTEM US109] Parent process (PID: 1234) waiting for report process...
[REPORT US109] Child process (PID: 1235) generating report...
[SYSTEM US109] Report process ended successfully with exit value: 0
```

### Expected File Output (`simulation_report.txt`)

```
==================================================
        FLIGHT SIMULATION - FINAL REPORT
==================================================
Generated on: Sun May 17 12:00:00 2026
Simulation End Status: COMPLETED (Normal)
Total Safety Violations Detected: 0
Total Aircraft Records: 3

--------------------------------------------------
Aircraft Identifier: FLIGHT_01
ACA Status: Exited ACA
Logged Snapshots inside ACA: 25
--------------------------------------------------
  [001] Lat: 41.2629 | Lon: -8.6852 | Alt: 69m | Spd: 250kt | Hdg: 42.5 | Vz: 12.0m/s
  ...
  [025] Lat: 41.9800 | Lon: -8.1000 | Alt: 9249m | Spd: 460kt | Hdg: 42.5 | Vz: 0.0m/s

--------------------------------------------------
Aircraft Identifier: FLIGHT_02
...

--------------------------------------------------
Aircraft Identifier: FLIGHT_03
...

=================== END OF REPORT ===================
```

In the collision scenario (`--collision`), the header reads:

```
Simulation End Status: ABORTED (Threshold Reached)
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
