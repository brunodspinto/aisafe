# US100 — Simulate Flights in a Given Area

## 1. User Story

**As** a Flight Control Operator, **I want** to simulate flights in a given area, **so that** I can track the position of each flight over time. Simulations are parameterised by time range, geographic area, included flights, weather conditions, safety thresholds, and performance settings. All required parameters should be validated.

---

## 2. Acceptance Criteria

The following are the acceptance criteria stated in the enunciado for US100:

| ID | Criterion | Status |
|----|-----------|--------|
| AC100.1 | Component implemented in C using processes, pipes, and signals | done |
| AC100.2 | System forks a new process for each flight | done |
| AC100.3 | Each flight process executes its designated flight plan | done |
| AC100.4 | Pipes facilitate communication between the main process and each flight process | done |
| AC100.5 | Main process tracks aircraft positions over time using an appropriate data structure | done |

> **Note:** ACA boundary filtering and entry/exit detection are implemented as part of **US101** (`aca_filter.c`). Safety cylinder violation detection and signal handling are part of **US102** (`us102.c`). Both are integrated into the `main.c` select loop.

### Known gaps / partial implementations

- **Parameter validation**: The enunciado states "all required parameters should be validated." Currently, the ACA boundary and `N_FLIGHTS` are compile-time constants in `main.c` rather than runtime-validated inputs. The `simulation_params_t` struct in `types.h` models the full parameter set but is not yet wired to the simulation entry point.
- **Weather conditions**: `segment_t` carries `wind_speed` and `wind_direction` fields, but `flight_process.c` does not yet apply wind to the position computation. This is a known stub for a future sprint.

---

## 3. Architecture Overview

The simulation follows a **multi-process, pipe-multiplexed** architecture:

```
main (parent)
  ├─ creates N pipes (one per flight)
  ├─ fork() × N  ──► child processes
  │     each child: execute_flight_process() → writes positions → pipe → exit()
  └─ select() loop
        reads from whichever pipe has data
        ACA filter (aca_filter.c)
        position history (ipc.c)
        safety cylinder check (us102.c)
        waitpid() when all pipes are closed
```

**Why one pipe per flight?**  
Using a single shared pipe would require flight-index tagging to identify the sender. One pipe per flight lets the parent identify the sender directly from the file-descriptor set returned by `select()`, keeping the parent logic simple and correct.

---

## 4. Process Model

### 4.1. Pipe creation

```c
int fd[2];
pipe(fd);
pipes[i].read_fd  = fd[0];
pipes[i].write_fd = fd[1];
```

`N_FLIGHTS` (currently 3) pipes are created before any `fork()` call so that every descriptor exists in the parent address space before it is inherited.

### 4.2. Fork and descriptor cleanup

After `fork()`, each child:

1. Closes **all** read ends (children never read from pipes).
2. Closes every write end **except its own** (each child writes to exactly one pipe).
3. Calls `execute_flight_process(pipes[i].write_fd, plans[i])`.

The parent closes all write ends immediately after the last `fork()`, ensuring EOF is delivered to the parent read loop when the last child exits.

### 4.3. Child execution (`flight_process.c`)

Each child simulates its flight plan segment by segment:

- **Speed by phase**: climb = 250 kt, cruise = 460 kt, descend = 220 kt.
- **Step count**: `dist_m / (speed_mps × 60)` (minimum 2 steps per segment). Each step represents 60 simulation seconds.
- **Position interpolation**: linear lat/lon/altitude between segment endpoints.
- **Heading**: true bearing derived from `atan2(Δlon, Δlat)`.
- Each step writes one `aircraft_position_t` struct to the pipe and sleeps 10 ms (real time) to pace the demo.

At the end of all segments the child closes its write end and calls `exit(0)`, which signals EOF to the parent.

### 4.4. Parent `select()` loop

```
while (open_pipes > 0)
    select(max_fd + 1, &read_fds, …)
    for each ready fd i:
        read aircraft_position_t
        → ACA filter + history (US101)
        → motion-vector update + safety check (US102)
        on EOF: close fd, decrement open_pipes
```

`select()` blocks until at least one child has data ready, preventing busy-waiting.

---

## 5. Data Structures (`types.h`)

| Type | Purpose |
|------|---------|
| `coordinate_t` | Latitude/longitude point |
| `segment_t` | One segment of a flight (mode, from/to coordinates, altitude range, wind) |
| `leg_t` | Ordered list of segments with departure/arrival airports |
| `flight_plan_t` | Named flight with one or more legs |
| `aircraft_position_t` | Single position snapshot (lat, lon, alt, speed, heading, timestamp, flight ID) |
| `geo_boundary_t` | Rectangular ACA boundary (north/south/east/west) |
| `aca_state_t` | `ACA_BEFORE`, `ACA_INSIDE`, `ACA_AFTER` — per-flight ACA lifecycle state |
| `flight_history_t` | Array of up to `MAX_POSITIONS` (1000) ACA positions for one flight |
| `simulation_params_t` | Full simulation configuration (time range, bounds, thresholds) |

---

## 6. Air Control Area (ACA)

The configured ACA corresponds to the **Lisbon FIR (western Iberian Peninsula)**:

```
latitude  : [36.0°N, 43.0°N]
longitude : [10.0°W,  6.0°W]
```

Porto (OPO, 41.26°N) lies inside the ACA; Madrid (MAD, 40.49°N / 3.56°W) lies outside. Flights therefore **enter the ACA at departure** and **exit during the cruise segment** — exercising both `ACA_BEFORE → ACA_INSIDE` and `ACA_INSIDE → ACA_AFTER` transitions.

### ACA filter (`aca_filter.c`)

```c
int is_in_aca(const aircraft_position_t *pos, const geo_boundary_t *aca)
```

Returns 1 if the position is within or on the boundary of the rectangle (bounds are inclusive).

Only positions for which `is_in_aca()` returns 1 are stored in `flight_history_t`. Entry and exit events are logged by comparing the previous `aca_state` with the new one after each received position.

---

## 7. Flight Plans (`flight_data.c`)

Three OPO → MAD flights are generated from the same 3-segment profile, offset by a small latitude shift to produce distinct trajectories:

| Flight | Lat offset | Route |
|--------|-----------|-------|
| FLIGHT_01 | 0.0° | Base OPO → MAD |
| FLIGHT_02 | +0.3° | Slightly north |
| FLIGHT_03 | −0.3° | Slightly south |

Each flight has one leg with three segments:

| # | Mode | From | To | Alt range |
|---|------|------|----|-----------|
| 0 | climb | OPO (41.26°N, 8.69°W) | (42.0°N, 8.01°W) | 69 m → 9 249 m |
| 1 | cruise | (42.0°N, 8.01°W) | (41.0°N, 4.30°W) | 9 249 m → 9 249 m |
| 2 | descend | (41.0°N, 4.30°W) | MAD (40.49°N, 3.56°W) | 9 249 m → 610 m |

---

## 8. Safety Cylinder — US102 Integration

After each position update, `monitor_safety_violations()` (`us102.c`) is called from the parent `select()` loop to check the updated flight against every other flight that already has at least two positions (a motion vector).

### Safety cylinder parameters

| Parameter | Value |
|-----------|-------|
| Horizontal separation | 14 816 m (8 NM) |
| Vertical separation | 600 m |
| Sub-steps for intersection | 10 |
| Max violations before abort | 5 |

### Trajectory intersection check

Both aircraft are linearly interpolated over `SUB_STEPS` sub-steps from their previous to their current position. At each sub-step the cylinder distances are computed. If **both** horizontal and vertical distances fall below their thresholds simultaneously, a violation is detected.

### Signal protocol

| Event | Signal |
|-------|--------|
| Violation detected | `SIGUSR1` → both implicated flights |
| Violation limit (5) reached | `SIGTERM` → all active flights |

When `SIGTERM` is sent, the parent sets `open_pipes = 0` and exits the `select()` loop, then proceeds to `waitpid()` cleanup.

---

## 9. IPC Utilities (`ipc.c`)

| Function | Description |
|----------|-------------|
| `find_or_create_flight()` | Looks up a flight by ID in `flight_history_t[]`; allocates a slot and initialises `aca_state = ACA_BEFORE` on first use |
| `print_history()` | Prints all recorded ACA positions per flight after the simulation ends |

---

## 10. Build and Run

The simulation has no external dependencies beyond a POSIX-compliant C compiler (`gcc`) and the standard math library.

```bash
cd aisafe.base/simulation

# Compile
gcc -Wall -Wextra -o simulation \
    main.c flight_process.c flight_data.c aca_filter.c ipc.c us102.c \
    -lm

# Run
./simulation
```

Expected output (abbreviated):

```
=== Flight Simulation (US101 & US102) ===
ACA: lat [36.0, 43.0]  lon [-10.0, -6.0]

Flight loaded: FLIGHT_01
Flight loaded: FLIGHT_02
Flight loaded: FLIGHT_03

=== Controlador Aereo Live (US101 & US102) ===
[FLIGHT_01] >>> ENTERING ACA
[FLIGHT_01] lat=41.2629 lon=-8.6852 alt=69m spd=250kt hdg=...
...
[FLIGHT_01] <<< EXITING ACA
...
=== Position History (ACA only) ===
Flight FLIGHT_01: N positions inside ACA [exited ACA]
  [0] lat=... lon=... alt=...m spd=...kt hdg=...
  ...
```

---

## 11. File Structure

```
aisafe.base/simulation/
├── main.c            — US100 entry point: fork/pipe/select orchestration
├── flight_process.c  — Child process: flight plan execution, pipe writes
├── flight_data.c     — Flight plan factory (OPO→MAD, 3 variants)
├── aca_filter.c      — ACA boundary predicate (is_in_aca)
├── ipc.c             — Position history management, history printer
├── us102.c           — Safety cylinder violation detection + signals
├── types.h           — All shared data structures
├── flight_process.h  — execute_flight_process() declaration
├── flight_data.h     — create_flight_plan() / free_flight_plan() declarations
├── aca_filter.h      — is_in_aca() declaration
├── ipc.h             — find_or_create_flight() / print_history() declarations
└── us102.h           — monitor_safety_violations() declaration
```

---

## 12. Design Decisions

**One pipe per flight over a shared pipe.**  
A single shared pipe would mix position records from different children. The parent would need an embedded flight index in every record to route it correctly. One dedicated pipe per flight eliminates this bookkeeping and makes the `select()` read loop straightforward: the position source is identified by which file descriptor fired.

**Physics-derived step count.**  
`compute_steps()` divides the real segment distance by `(speed × 60 s)` rather than using a fixed step count. This means longer segments and slower speeds produce proportionally more position updates, keeping simulation time roughly proportional to real flight time.

**Motion-vector window in the parent, not in child memory.**  
Because safety cylinder checks require comparing two flights simultaneously, the check must happen in the single entity that can observe all flights — the parent. Each received position slides a two-slot window (`prev_positions[i]`, `current_positions[i]`) forward in the parent.
