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

> **Note:** ACA boundary filtering and entry/exit detection are implemented as part of **US101** (`aca_filter.c`). Safety cylinder violation detection and signal handling are part of **US102** (`us102.c`). Step synchronisation is part of **US103** (GO/STOP control pipe). All are integrated into the `main.c` select loop.

### Known gaps / partial implementations

- **Parameter validation**: The enunciado states "all required parameters should be validated." Currently, the ACA boundary and `N_FLIGHTS_NORMAL` are compile-time constants in `main.c` rather than runtime-validated inputs. The `simulation_params_t` struct in `types.h` models the full parameter set but is not yet wired to the simulation entry point.
- **Weather conditions**: `segment_t` carries `wind_speed` and `wind_direction` fields, but `flight_process.c` does not yet apply wind to the position computation.

---

## 3. Architecture Overview

The simulation follows a **multi-process, bidirectional-pipe, select-multiplexed** architecture:

```
main (parent)
  ├─ creates 2 pipes per flight
  │     pipe A: child → parent  (position updates)
  │     pipe B: parent → child  (GO 'G' / STOP 'S' tokens)
  ├─ fork() × N  ──► child processes
  │     each child: execute_flight_process()
  │       writes position → pipe A
  │       blocks on read(pipe B) waiting for 'G' or 'S'
  │       exits on 'S', SIGUSR1 collision alert, or plan completion
  └─ select() loop (reads pipe A of all active flights)
        collects positions until every active flight has reported
        runs US101 (ACA filter + history)
        runs US102 (safety cylinder check) across all flight pairs
        sends 'G' to all if safe, 'S' + SIGTERM if critical violation
        waitpid() cleanup after loop
```

**Why bidirectional pipes?**
The position pipe (child → parent) carries `aircraft_position_t` structs. The control pipe (parent → child) carries single-byte tokens (`'G'` or `'S'`). Together they implement a synchronised time-step: no flight advances past step T+1 until the parent has verified safety at step T across every active flight.

---

## 4. Process Model

### 4.1. Pipe creation

Two `pipe()` calls are made per flight before any `fork()`:

```c
typedef struct {
    int pos_read_fd;   /* parent reads positions from child */
    int pos_write_fd;  /* child writes positions to parent  */
    int ctrl_write_fd; /* parent writes 'G'/'S' to child   */
    int ctrl_read_fd;  /* child reads  'G'/'S' from parent */
} flight_pipes_t;
```

All descriptors exist in the parent address space before `fork()` so every child inherits them all.

### 4.2. Fork and descriptor cleanup

After `fork()`, each child:

1. Closes all position read ends and all control write ends (children never use these).
2. Closes the position write ends and control read ends of every other flight.
3. Retains only its own `pos_write_fd` and `ctrl_read_fd`.
4. Calls `execute_flight_process(pos_write_fd, ctrl_read_fd, plan)`.

The parent closes `pos_write_fd` and `ctrl_read_fd` for all flights immediately after the last `fork()`.

### 4.3. Child execution (`flight_process.c`)

Each child installs a `SIGUSR1` handler before entering the simulation loop:

```c
act.sa_handler = handle_sigusr1;
act.sa_flags   = SA_RESTART;
sigfillset(&act.sa_mask);   /* block all signals while handler runs */
sigaction(SIGUSR1, &act, NULL);
```

The handler sets `volatile sig_atomic_t collision_alert = 1` using only `write()` (async-signal-safe).

**Per-step physics (`STEP_SECONDS = 1`):**

- Speed and vertical rate are looked up from altitude-indexed performance tables (`flight_profile_t`) via linear interpolation (`lookup_perf()`).
- Climb/descend: vertical rate (`vz_mps`) from the table; horizontal advance proportional to speed.
- Cruise: constant `cruise_speed_knots`; horizontal advance only.
- Heading: true bearing from `atan2(Δlon, Δlat)`.

After writing each position struct to `pos_write_fd`, the child blocks:

```c
ssize_t r = read(ctrl_read_fd, &token, 1);
if (collision_alert || r <= 0 || token == 'S') { exit(1); }
/* token == 'G': safe to continue */
```

At end of plan: closes both fds and calls `exit(0)`.

### 4.4. Parent `select()` loop

```
while (n_active > 0):
    select() — wait for any active flight that has not yet reported this step
    for each ready fd:
        read aircraft_position_t
        → US101: ACA filter + entry/exit detection + history update
        → slide motion-vector window (prev/current positions)
        mark flight as received for this step
        on EOF: close fds, decrement n_active

    if not all active flights have reported yet: continue

    run US102: monitor_safety_violations() for every active flight pair
    if abort_sim:
        send 'S' to all active flights, set n_active = 0, break
    else:
        send 'G' to all active flights
        reset received[] flags for next step
```

`select()` only watches flights that are still active and have not yet reported for the current step, avoiding spurious wakeups.

---

## 5. Data Structures (`types.h`)

| Type | Purpose |
|------|---------|
| `coordinate_t` | Latitude/longitude point |
| `perf_point_t` | One row of a performance table (altitude, speed, vertical rate) |
| `flight_profile_t` | Climb and descend performance tables + cruise speed |
| `segment_t` | One segment (mode, from/to coordinates, altitude range, wind) |
| `leg_t` | Ordered segments with departure/arrival airports and a `flight_profile_t` |
| `flight_plan_t` | Named flight with one or more legs |
| `aircraft_position_t` | Position snapshot (lat, lon, alt, speed, heading, `vz_mps`, timestamp, flight ID) |
| `geo_boundary_t` | Rectangular ACA boundary (north/south/east/west) |
| `aca_state_t` | `ACA_BEFORE`, `ACA_INSIDE`, `ACA_AFTER` — per-flight ACA lifecycle state |
| `flight_history_t` | Up to `MAX_POSITIONS` (1000) ACA-only positions per flight |
| `simulation_params_t` | Full simulation configuration (time range, bounds, thresholds) — not yet wired |

---

## 6. Air Control Area (ACA)

The configured ACA corresponds to the **Lisbon FIR (western Iberian Peninsula)**:

```
latitude  : [36.0 N, 43.0 N]
longitude : [10.0 W,  6.0 W]
```

Porto (OPO, 41.26 N) lies inside the ACA; Madrid (MAD, 40.49 N / 3.56 W) lies outside. Flights enter the ACA at departure and exit during the cruise segment — exercising both `ACA_BEFORE → ACA_INSIDE` and `ACA_INSIDE → ACA_AFTER` transitions.

The ACA predicate (`aca_filter.c`) uses inclusive bounds (`<=` / `>=`).

---

## 7. Flight Plans (`flight_data.c`)

Four OPO → MAD flights are built from the same 3-segment profile with a latitude offset:

| Flight | Lat offset | Purpose |
|--------|-----------|---------|
| FLIGHT_01 | 0.0° | Base trajectory |
| FLIGHT_02 | +0.3° | Slightly north |
| FLIGHT_03 | −0.3° | Slightly south |
| FLIGHT_04 | +0.02° | ~2.2 km north of FLIGHT_01 — triggers cylinder violation |

Normal mode uses flights 01–03. `--collision` mode adds FLIGHT_04.

Each flight has one leg with three segments:

| # | Mode | From | To | Alt range |
|---|------|------|----|-----------|
| 0 | climb | OPO (41.26 N, 8.69 W) | (42.0 N, 8.01 W) | 69 m → 9 249 m |
| 1 | cruise | (42.0 N, 8.01 W) | (41.0 N, 4.30 W) | 9 249 m constant |
| 2 | descend | (41.0 N, 4.30 W) | MAD (40.49 N, 3.56 W) | 9 249 m → 610 m |

Performance tables contain 13 altitude points each for climb and descend (from the `Flight_Plan_v0c.json` profile). Cruise speed is 460 kt.

---

## 8. Safety Cylinder — US102 Integration

After every active flight has reported for a step, `monitor_safety_violations()` (`us102.c`) compares each updated flight against all others with at least two positions.

### Safety cylinder parameters

| Parameter | Value |
|-----------|-------|
| Horizontal separation | 14 816 m (8 NM) |
| Vertical separation | 600 m |
| Sub-steps for intersection | 10 |
| Max violations before abort | 5 |

### Trajectory intersection

Both aircraft are linearly interpolated over `SUB_STEPS` sub-steps. At each sub-step the cylinder distances are computed. If both distances fall below their thresholds simultaneously, a violation is detected.

### Signal and control protocol

| Event | Action |
|-------|--------|
| Violation detected (below limit) | `SIGUSR1` to both implicated flights; parent still sends `'G'` this step |
| Violation limit (5) reached | `SIGTERM` to all active flights; parent sends `'S'` and terminates loop |

`SIGPIPE` is ignored in the parent (`signal(SIGPIPE, SIG_IGN)`) so that writing `'S'` to a pipe whose child was already killed by `SIGTERM` returns `EPIPE` rather than crashing the parent.

`predict_future_collisions()` (`us102.c`) provides an advisory pre-flight check by comparing segment endpoint pairs of two flight plans for cylinder overlap.

---

## 9. IPC Utilities (`ipc.c`)

| Function | Description |
|----------|-------------|
| `find_or_create_flight()` | Looks up a flight by ID in `flight_history_t[]`; allocates slot and sets `aca_state = ACA_BEFORE` on first use; returns -1 if full |
| `print_history()` | Prints all ACA-recorded positions per flight after simulation ends |

---

## 10. Build and Run

```bash
cd aisafe.base/simulation

gcc -Wall -Wextra -o simulation \
    main.c flight_process.c flight_data.c aca_filter.c ipc.c us102.c \
    -lm

# Normal mode (3 flights, no guaranteed collision)
./simulation

# Collision test mode (adds FLIGHT_04 ~2.2 km from FLIGHT_01)
./simulation --collision
```

Expected output (abbreviated, normal mode):

```
=== Flight Simulation (US101 & US102) ===
Mode: normal
ACA: lat [36.0, 43.0]  lon [-10.0, -6.0]

Flight loaded: FLIGHT_01
Flight loaded: FLIGHT_02
Flight loaded: FLIGHT_03

=== Controlador Aereo Live (US101 & US102) ===
[FLIGHT_01] >>> ENTERING ACA
[FLIGHT_01] lat=41.2629 lon=-8.6853 alt=81m spd=210.0kt hdg=... vz=12.0m/s
...
[FLIGHT_01] <<< EXITING ACA
...
=== Position History (ACA only) ===
Flight FLIGHT_01: N positions inside ACA [exited ACA]
  [0] lat=... lon=... alt=...m spd=...kt hdg=...
  ...
[FLIGHT_01] ended with code 0
```

---

## 11. File Structure

```
aisafe.base/simulation/
├── main.c            — US100 entry point: fork/pipe/select/GO-STOP orchestration
├── flight_process.c  — Child: plan execution, perf-table physics, SIGUSR1 handler
├── flight_data.c     — Flight plan factory (OPO→MAD, 4 variants with profiles)
├── aca_filter.c      — ACA boundary predicate (is_in_aca)
├── ipc.c             — Position history management + printer
├── us102.c           — Safety cylinder violation detection + predict_future_collisions
├── types.h           — All shared data structures
├── flight_process.h  — execute_flight_process() declaration (3 params)
├── flight_data.h     — create_flight_plan() / free_flight_plan() declarations
├── aca_filter.h      — is_in_aca() declaration
├── ipc.h             — find_or_create_flight() / print_history() declarations
└── us102.h           — monitor_safety_violations() / predict_future_collisions() declarations
```

---

## 12. Design Decisions

**Bidirectional pipes (2 per flight) instead of 1.**
A unidirectional pipe carries positions from child to parent. The second pipe (parent to child) carries GO/STOP tokens. Without it, the parent has no way to tell a child to stop short of a signal, and synchronisation would rely entirely on asynchronous signals rather than the deterministic blocking read.

**Synchronised time steps via GO/STOP.**
After collecting one position from every active flight, the parent runs all safety checks before issuing GO. This guarantees that no flight advances to step T+1 until the parent has verified safety at step T across all pairs. Pure signal-based approaches do not provide this guarantee.

**Performance tables instead of constant speeds.**
`lookup_perf()` interpolates speed and vertical rate from altitude-indexed tables derived from `Flight_Plan_v0c.json`. This makes climb and descent profiles realistic: speed and climb rate decrease with altitude, matching real aircraft behaviour.

**Motion-vector window in the parent.**
Safety cylinder checks compare two flights simultaneously. Only the parent can observe all flights at once. Each received position slides a two-slot window (`prev_positions[i]`, `current_positions[i]`) in the parent, keeping all comparison logic out of the child processes.

**`SIGPIPE` ignored in the parent.**
When the violation limit is reached, `us102.c` sends `SIGTERM` to all children before returning. The parent then tries to write `'S'` on the control pipes. If a child dies before the write, the pipe has no reader and a default `SIGPIPE` would kill the parent. Ignoring `SIGPIPE` lets `write()` return `-1`/`EPIPE` harmlessly instead.
