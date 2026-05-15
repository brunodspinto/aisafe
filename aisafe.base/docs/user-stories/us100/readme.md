# US100 — Simulate Flights in a Given Area

## 1. Context

US100 is the top-level orchestration user story for the simulation subsystem. It is
responsible for launching all flight processes, wiring the inter-process communication,
and driving the main simulation loop. All other simulation user stories (US101–US103) are
integrated here: position capture and ACA filtering (US101), safety cylinder detection and
signal protocol (US102), and step-by-step synchronisation via GO/STOP (US103).

The implementation is written in C and lives entirely under `aisafe.base/simulation/`.
Runtime parameters (ACA bounds, safety thresholds, number of flights) are loaded from
`simulation.conf` and validated before the simulation starts.

---

## 2. Requirements

**US100:** As a Flight Control Operator, I want to simulate flights in a given area, so
that I can track the position of each flight over time. Simulations are parameterised by
time range, geographic area, included flights, weather conditions, safety thresholds, and
performance settings. All required parameters should be validated.

### Acceptance Criteria

| ID | Criterion | Status |
|----|-----------|--------|
| AC100.1 | Component implemented in C using processes, pipes, and signals | Done |
| AC100.2 | System forks a new process for each flight | Done |
| AC100.3 | Each flight process executes its designated flight plan | Done |
| AC100.4 | Pipes facilitate communication between the main process and each flight process | Done |
| AC100.5 | Main process tracks aircraft positions over time using an appropriate data structure | Done |

> **Note:** ACA boundary filtering and entry/exit detection are implemented as part of
> **US101** (`aca_filter.c`). Safety cylinder violation detection and signal handling are
> part of **US102** (`us102.c`). Step synchronisation is part of **US103** (GO/STOP
> control pipe). All are integrated into `main.c`.
>
> **Weather conditions:** `segment_t` carries `wind_speed` and `wind_direction` fields
> (part of the domain model). Applying wind to flight physics belongs to **US110**
> (future sprint — requires shared memory and environment thread).

---

## 3. Analysis

### 3.1 Problem decomposition

The simulation involves N concurrent flights. Each flight follows a route divided into
segments (climb, cruise, descend). The parent process must collect position updates from
every child, run safety checks across all active flights, and control whether each child
advances to the next second.

The key design question is **how to coordinate N concurrent child processes** so that:
- Positions compared by the safety check are contemporaneous (same simulation second).
- The parent can stop any child on demand.
- The architecture stays close to the professor's canonical IPC examples.

Two approaches were considered:

| Approach | Description | Decision |
|----------|-------------|----------|
| One shared pipe + `select()` | All children write to one pipe; parent multiplexes with `select()` | Rejected — `select()` adds complexity; children cannot be individually stopped without signals |
| Two pipes per flight (bidirectional) | One pipe child → parent (positions); one pipe parent → child (GO/STOP tokens) | **Chosen** — deterministic step control, individual child management, TP5 pattern |

### 3.2 Flight model

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

Performance tables contain 13 altitude points each for climb and descend (from
`Flight_Plan_v0c.json`). Cruise speed is 460 kt.

---

## 4. Design

### 4.1 Data structures (`types.h`)

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
| `simulation_params_t` | Full simulation configuration loaded from `simulation.conf` |

### 4.2 Process and pipe architecture

```
main (parent)
  ├─ creates 2 pipes per flight
  │     pipe A: child → parent  (aircraft_position_t structs)
  │     pipe B: parent → child  (single-byte 'G' or 'S' tokens)
  ├─ fork() × N  ──► child processes
  │     each child: execute_flight_process(pos_write_fd, ctrl_read_fd, plan)
  │       writes position → pipe A
  │       blocks on read(pipe B) waiting for 'G' or 'S'
  │       exits on 'S', SIGUSR1 collision alert, or plan completion
  └─ main loop: sequential blocking read() per active flight (TP5 pattern)
        reads one position from every active flight in order
        runs US101 (ACA filter + history)
        runs US102 (safety cylinder check) across all flight pairs
        sends 'G' to all if safe, 'S' + SIGTERM if critical violation
        waitpid() cleanup after loop
```

The `flight_pipes_t` struct groups both pipe ends per flight:

```c
typedef struct {
    int pos_read_fd;   /* parent reads positions from child */
    int pos_write_fd;  /* child writes positions to parent  */
    int ctrl_write_fd; /* parent writes 'G'/'S' to child   */
    int ctrl_read_fd;  /* child reads  'G'/'S' from parent */
} flight_pipes_t;
```

### 4.3 Fork and descriptor cleanup

After `fork()`, each child:

1. Closes all `pos_read_fd` and `ctrl_write_fd` ends (children never use these).
2. Closes `pos_write_fd` and `ctrl_read_fd` of every other flight.
3. Retains only its own `pos_write_fd` and `ctrl_read_fd`.
4. Calls `execute_flight_process(pos_write_fd, ctrl_read_fd, plan)`.

The parent closes `pos_write_fd` and `ctrl_read_fd` for all flights immediately after
the last `fork()`.

### 4.4 Main loop — collect all, then decide (`main.c`)

```c
while (n_active > 0) {
    for (i = 0; i < n_flights; i++) {
        if (!active[i]) continue;
        ssize_t n = read(pipes[i].pos_read_fd, &pos, sizeof(pos));
        if (n == sizeof(pos)) {
            /* US101: ACA filter + entry/exit detection + history update */
            /* US102: slide motion-vector window (prev/current positions) */
        } else {
            /* EOF: flight finished */
            close(pipes[i].pos_read_fd);
            close(pipes[i].ctrl_write_fd);
            active[i] = 0; n_active--;
        }
    }
    if (n_active == 0) break;

    /* Advisory: predict future segment collisions (once per pair, log only) */
    /* US102: monitor_safety_violations() across all active pairs */

    if (abort_sim) {
        for (i = 0; i < n_flights; i++) {
            if (active[i]) { write(pipes[i].ctrl_write_fd, "S", 1); }
        }
        break;
    }
    for (i = 0; i < n_flights; i++) {
        if (active[i]) { write(pipes[i].ctrl_write_fd, "G", 1); }
    }
}
```

### 4.5 Configuration (`simulation.conf`)

All runtime parameters are loaded from a key=value file by `load_config()` and validated
by `validate_config()` before the simulation starts. Missing or invalid values cause an
error on stderr and exit code 1.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `aca_north` | double | 43.0 | ACA north latitude boundary (decimal degrees) |
| `aca_south` | double | 36.0 | ACA south latitude boundary (must be < `aca_north`) |
| `aca_east` | double | -6.0 | ACA east longitude boundary |
| `aca_west` | double | -10.0 | ACA west longitude boundary (must be < `aca_east`) |
| `n_flights` | int | 3 | Number of flights to simulate (1 to `MAX_FLIGHTS`) |
| `safe_dist_horiz_nm` | double | 8.0 | Horizontal separation in nautical miles |
| `safe_dist_vert_m` | double | 600.0 | Vertical separation in meters |
| `max_violations` | int | 5 | Violation count before early termination |

---

## 5. Implementation — Files

| File | Role |
|------|------|
| `main.c` | Entry point: config load, fork/pipe setup, main loop, waitpid |
| `flight_process.c` | Child: plan execution, perf-table physics, SIGUSR1 handler, GO/STOP sync |
| `flight_data.c` | Flight plan factory (4 OPO→MAD variants with full performance tables) |
| `aca_filter.c` | `is_in_aca()` — rectangular ACA boundary predicate (US101) |
| `ipc.c` | `find_or_create_flight()` + `print_history()` — position history management |
| `config.c` | `load_config()` / `validate_config()` / `print_config()` |
| `us102.c` | `monitor_safety_violations()` + `predict_future_collisions()` (US102) |
| `types.h` | All shared data structures |
| `simulation.conf` | Runtime parameters (ACA bounds, n_flights, safety thresholds) |

---

## 6. How to Build and Run

```bash
# From repository root
bash aisafe.base/libs/scripts/build_c.sh

# Normal simulation (3 flights, no guaranteed collision)
./aisafe.base/bin/simulation

# Collision test (4 flights; FLIGHT_04 triggers cylinder alert)
./aisafe.base/bin/simulation --collision
```

### Expected Output — Normal Mode

```
=== AISafe Flight Simulation ===
Mode: normal
--- Simulation Parameters ---
  ACA: lat [36.00, 43.00]  lon [-10.00, -6.00]
  Flights: 3
  Safety cylinder: 14816 m horiz (8.00 NM)  /  600 m vert
  Max violations: 5
-----------------------------
Flight loaded: FLIGHT_01
Flight loaded: FLIGHT_02
Flight loaded: FLIGHT_03

=== Air Traffic Control Live Feed ===
[FLIGHT_01] >>> ENTERING ACA
[FLIGHT_01] lat=41.2629 lon=-8.6853 alt=81m spd=210.0kt hdg=42.5 vz=12.0m/s
[FLIGHT_02] >>> ENTERING ACA
...
[FLIGHT_01] <<< EXITING ACA
[FLIGHT_02] <<< EXITING ACA
[FLIGHT_03] <<< EXITING ACA

=== Position History (ACA only) ===
Flight FLIGHT_01: N positions inside ACA [exited ACA]
  [0] lat=41.2629 lon=-8.6853 alt=81m spd=210.0kt hdg=42.5
  ...
[FLIGHT_01] ended with code 0
[FLIGHT_02] ended with code 0
[FLIGHT_03] ended with code 0
```

No `CYLINDER ALERT` lines appear. All children exit with code 0.

### Expected Output — Collision Mode (`--collision`)

```
=== AISafe Flight Simulation ===
Mode: COLLISION TEST
...
[US102 CYLINDER ALERT] Intersection risk detected at ...
Flights: FLIGHT_01 and FLIGHT_04 crossed paths (H < 8NM and V < 600m).
...
[FLIGHT_01] ended with code 1
[FLIGHT_04] ended with code 1
```

FLIGHT_01 and FLIGHT_04 receive `SIGUSR1` and exit with code 1 at their next step
boundary. FLIGHT_02 and FLIGHT_03 are unaffected.

---

## 7. Key Design Decisions

| Decision | Reason |
|----------|--------|
| Two pipes per flight (bidirectional) | Unidirectional pipe delivers positions but cannot stop a child; the second pipe carries GO/STOP tokens, enabling deterministic step control without relying solely on signals |
| Sequential blocking reads in parent (TP5) | Every child is paused at `read(ctrl_read_fd)` — data is always ready on each position pipe; no `select()` needed, simpler and aligned with professor's pattern |
| Collect-all-then-decide | Safety checks compare one position per flight per step; sending GO to one flight before all others have reported would compare non-contemporaneous positions and produce incorrect results |
| Performance tables (`lookup_perf`) | Speed and vertical rate vary with altitude, matching the Flight Profile JSON; removes the unrealistic constant-speed-per-phase approximation from earlier iterations |
| Motion-vector window in parent | Only the parent observes all flights simultaneously; keeping `prev_positions[i]` / `current_positions[i]` in the parent avoids shared memory and keeps all comparison logic in one place |
| `SIGPIPE` ignored in parent | When US102 sends `SIGTERM` to children before the parent writes `'S'`, the pipe has no reader; `SIG_IGN` makes `write()` return `EPIPE` instead of killing the parent |
| `prediction_done[MAX_FLIGHTS][MAX_FLIGHTS]` | Future-collision advisory is printed once per flight pair; the matrix is sized to `MAX_FLIGHTS` (not the active count) to avoid buffer overflows when `n_flights` approaches the compile-time limit |
