# US100 - Simulate Flights in a Given Area

## Context

US100 establishes the entry point for the simulation subsystem. The parent process loads
flight plans and spawns one child process per flight. Each child executes its route
step by step and reports positions back to the parent. The simulation accounts for:

- weather conditions
- safety thresholds

All required parameters should be validated.

## Acceptance Criteria

- The component must be implemented in C and must utilize processes, pipes, and signals.
- The system should fork a new process for each flight.
- Each flight process should execute its designated flight plan.
- Pipes should facilitate communication between the main process and each flight process.
- All required parameters should be validated.

## Implementation

**Location:** `aisafe.base/simulation/`

**Files:**

- `main.c` - starts and coordinates the simulation
- `flight_process.c` - runs the flight in the child process
- `ipc.c/h` - pipe communication
- `types.h` - simulation data structures
- `flight_data.h` - sample flight data
- `validation.c/h` - parameter validation
- `Makefile` - build, test, run, and clean targets

## How It Works

1. The parent process creates two pipes per flight (position pipe and control pipe).
2. The parent forks one child process per flight (three flights: FLIGHT_01, FLIGHT_02, FLIGHT_03).
3. Each child executes its flight plan step by step using a physics-driven loop.
4. After each step the child sends its current position through the position pipe and blocks waiting for a GO/STOP token on the control pipe.
5. The parent collects one position from every active flight, runs safety checks, then sends the token.
6. The simulation ends when all child processes have completed their routes or a safety violation threshold is reached.

Three flights are simulated on the OPO → MAD route, each offset by ±0.3° in latitude so their trajectories remain spatially separated.

## Build and Run

```bash
cd aisafe.base/simulation
make
make test
make run
```

## Current Status

| Feature | Status | Notes |
|---------|--------|-------|
| Single flight process | Done | Runs the sample flight |
| Pipe communication | Done | Parent and child exchange positions |
| Flight plan validation | Done | Validation tests are available |
| Multiple flights | Not implemented | Current version simulates one flight |
| Signal handling | Not implemented | Listed in the requirements, not yet implemented |
| Weather effects | Not implemented | Structures exist for future use |

## Notes

`make test` runs the validation tests, and `make clean` removes generated objects and binaries.


# US101 — Capture and Process Flight Movements

## 1. Context

US101 is the first SCOMP-facing user story in the simulation subsystem. It establishes the
mechanism by which the parent simulation process receives real-time position updates from
child flight processes via unnamed POSIX pipes, and stores a complete position history that
can later be used to detect and anticipate safety violations.

The implementation is written in C and lives entirely under `aisafe.base/simulation/`.
The architecture is deliberately aligned with the professor's canonical examples
(Luís Nogueira, ISEP SCOMP 2526), specifically **ex1-6.c** (one shared pipe, N children)
and **ex1-2.c** (waitpid + WIFEXITED pattern).

---

## 2. Requirements

**US101:** As a simulation process, I want to receive movement commands from flight
processes so that I can track aircraft positions over time.

### Acceptance Criteria

| ID | Criterion | Status |
|----|-----------|--------|
| AC1 | Each flight process must send position updates to the main process via a pipe | Done |
| AC2 | The main process should track aircraft positions | Done |
| AC3 | The system must store past positions to anticipate and detect potential safety violations | Done |

---

## 3. Analysis

### 3.1 Problem decomposition

The simulation involves N concurrent flights. Each flight follows a route divided into
segments (climb, cruise, descend). At each interpolation step within a segment the flight
process must report its current position, altitude, speed and heading to a central process
that aggregates the data.

The key design question is **how the child processes communicate with the parent**. Three
approaches were considered:

| Approach | Description | Decision |
|----------|-------------|----------|
| One pipe per flight + `select()` | Each child writes to its own pipe; parent uses `select()` to multiplex | Rejected — more complex, not aligned with professor's style |
| One shared pipe, N children | All children write to the same pipe; parent reads in a single loop | Initial US101 prototype (ex1-6.c), replaced in US102 |
| Two pipes per flight (TP5 pattern) | Each child has its own `pos_pipe` (child→parent) and `ctrl_pipe` (parent→child); parent reads sequentially with blocking reads | **Chosen** — required by US102 GO/STOP synchronisation protocol |

The two-pipe approach guarantees that positions compared in the safety check are always
contemporaneous (same simulation second). Each `write()` of `sizeof(aircraft_position_t)`
(≈ 128 bytes) is below PIPE_BUF (minimum 512 bytes on POSIX), so writes remain **atomic**
— no message framing is needed.

### 3.2 Flight model

Three flights are simulated on the OPO → MAD route, each with a distinct departure
latitude so their trajectories are spatially separated:

| Flight | Departure offset | Departure lat |
|--------|-----------------|---------------|
| FLIGHT_01 | exact OPO | 41.262891° N |
| FLIGHT_02 | +0.3° north | 41.562891° N |
| FLIGHT_03 | -0.3° south | 40.962891° N |

All three follow the same 3-segment altitude profile derived from `Flight_Plan_v0c.json`:

| # | Phase | Start coords | Start alt | End coords | End alt |
|---|-------|-------------|-----------|-----------|---------|
| 0 | climb | (41.262891, -8.68522) | 69 m | (42.0, -8.01) | 9 249 m |
| 1 | cruise | (42.0, -8.01) | 9 249 m | (41.0, -4.3) | 9 249 m |
| 2 | descend | (41.0, -4.3) | 9 249 m | (40.4895, -3.5643) | 610 m |

---

## 4. Design

### 4.1 Data Structures (`types.h`)

#### `aircraft_position_t`

Represents a single position snapshot sent from a flight process to the parent.

```c
typedef struct {
    double latitude;
    double longitude;
    double altitude_meters;
    double speed_knots;     /* needed to anticipate future position */
    double heading_deg;     /* needed to anticipate future position */
    double vz_mps;          /* vertical rate m/s (added in US102 for cylinder check) */
    time_t timestamp;
    char flight_id[64];
} aircraft_position_t;
```

`speed_knots` and `heading_deg` were added for AC3 — they allow the parent to project
a future position from the last known position, which is the foundation of safety-violation
anticipation. `vz_mps` was added in US102 and is populated by `lookup_perf()` in the
child process.

#### `segment_t`

Represents one phase of a leg (climb, cruise or descend).

```c
typedef struct {
    char mode[16];           /* "climb", "cruise", "descend" */
    coordinate_t from;
    coordinate_t to;
    double alt_from_meters;  /* altitude at start of segment */
    double alt_to_meters;    /* altitude at end of segment   */
    double width_meters;
    double wind_speed;
    double wind_direction;
} segment_t;
```

The old flat `altitude_meters` field has been replaced with `alt_from_meters` and
`alt_to_meters` so the flight process can linearly interpolate altitude through the
segment instead of reporting a constant value.

#### `flight_history_t`

Accumulates all positions received for one flight.

```c
typedef struct {
    aircraft_position_t positions[MAX_POSITIONS];
    int count;
    char flight_id[64];
} flight_history_t;
```

Up to `MAX_POSITIONS` (1 000) records are kept per flight across all `MAX_FLIGHTS` (10) slots.

### 4.2 Process Architecture

The architecture uses **two unnamed POSIX pipes per flight** (updated in US102 to support
bidirectional synchronisation):

```
                        ┌──────────────┐
                        │    PARENT    │
                        │  (main.c)   │
                        └──────┬───────┘
          ┌───────────────────┼────────────────────┐
    pos_pipe[0]         pos_pipe[1]          pos_pipe[2]
    ctrl_pipe[0]        ctrl_pipe[1]         ctrl_pipe[2]
          │                   │                    │
    ┌─────▼──────┐      ┌─────▼──────┐      ┌─────▼──────┐
    │  FLIGHT_01 │      │  FLIGHT_02 │      │  FLIGHT_03 │
    │(child proc)│      │(child proc)│      │(child proc)│
    └────────────┘      └────────────┘      └────────────┘
```

| Pipe | Direction | Purpose |
|------|-----------|---------|
| `pos_pipe` | child → parent | sends `aircraft_position_t` each second |
| `ctrl_pipe` | parent → child | sends `'G'` (go) or `'S'` (stop) |

The parent reads positions from active flights sequentially (blocking read per flight,
TP5 pattern). Since all children block on `ctrl_read_fd` until they receive a token,
no child can advance past step T until the parent has read from every active flight and
sent the GO/STOP decision — this guarantees synchronised time steps.

When a child finishes, EOF on `pos_read_fd[i]` signals the parent, which then closes
`ctrl_write_fd[i]` and marks the flight inactive.

### 4.3 Flight Process (`flight_process.c`)

`execute_flight_process(int pos_write_fd, int ctrl_read_fd, const flight_plan_t *plan)`
is called from the child. It:

1. Installs a `SIGUSR1` handler (US102) with `sigfillset` + `SA_RESTART`.
2. Iterates over each leg and segment using a physics-driven `while(1)` loop (no fixed
   `STEP_COUNT`) — the loop exits when the segment's target altitude (climb/descend)
   or distance (cruise) is reached.
3. For each step computes:

   **Speed and vertical rate** — altitude-interpolated from the performance table:
   ```c
   lookup_perf(prof->climb, prof->climb_count, alt, &speed_kt, &vz);
   ```

   **Position** — advances horizontally at `speed_mps * STEP_SECONDS` along the
   segment direction vector:
   ```c
   lat += (dlat / dist_total_m) * (horiz_step_m / 110574.0);
   lon += (dlon / dist_total_m) * (horiz_step_m / (111320.0 * cos(lat_mid_rad)));
   ```

   **Altitude** — advances vertically by `vz * STEP_SECONDS`.

   **Heading** — true bearing from the segment vector:
   ```c
   pos.heading_deg = atan2(dlon, dlat) * 180.0 / M_PI;
   if (pos.heading_deg < 0) pos.heading_deg += 360.0;
   ```

4. Writes the struct to the position pipe:
   ```c
   write(pos_write_fd, &pos, sizeof(pos));
   ```

5. Blocks waiting for the parent's GO/STOP token:
   ```c
   char token = 0;
   read(ctrl_read_fd, &token, 1);
   if (collision_alert || token == 'S') { close(…); exit(1); }
   ```
   No `nanosleep()` — the pipe round-trip provides natural per-second pacing.

6. On completion: `close(pos_write_fd); close(ctrl_read_fd); exit(0);`

### 4.4 IPC Layer (`ipc.c` / `ipc.h`)

The IPC module is kept minimal. The `send_position()` / `recv_position()` wrapper
functions and the `pipe_t` helper struct have been removed — the professor's pattern
uses direct `write()` / `read()` calls without wrappers. Only two functions remain:

| Function | Signature | Purpose |
|----------|-----------|---------|
| `find_or_create_flight` | `(flight_history_t *histories, int n, const char *flight_id)` | Returns the index of the history slot for this flight (creates one if new) |
| `print_history` | `(const flight_history_t *histories, int n)` | Prints the full recorded history after the simulation ends |

### 4.5 Main Loop (`main.c`)

The parent's main loop uses sequential blocking reads per flight (TP5 pattern):

```c
while (n_active > 0) {
    /* Read one position from each active flight (blocks until data arrives) */
    for (i = 0; i < n_flights; i++) {
        if (!active[i]) continue;
        ssize_t n = read(pipes[i].pos_read_fd, &pos, sizeof(pos));
        if (n == sizeof(pos)) {
            /* US101: ACA filter + history update */
            int idx = find_or_create_flight(histories, MAX_FLIGHTS, pos.flight_id);
            /* store position, detect ENTERING/EXITING ACA … */
        } else {
            /* EOF: flight finished */
            close(pipes[i].pos_read_fd);
            close(pipes[i].ctrl_write_fd);
            active[i] = 0;  n_active--;
        }
    }
    /* US102: safety cylinder check → send 'G' or 'S' to all active flights */
    for (i = 0; i < n_flights; i++)
        if (active[i]) write(pipes[i].ctrl_write_fd, &token, 1);
}

print_history(histories, MAX_FLIGHTS);

for (i = 0; i < n_flights; i++) {
    waitpid(pids[i], &status, 0);
    if (WIFEXITED(status))
        printf("[FLIGHT_%02d] ended with code %d\n", i+1, WEXITSTATUS(status));
}
```

### 4.6 Alignment with Professor's Examples

The initial US101 implementation followed **ex1-6.c** (one shared pipe, N children).
US102 replaced it with the **TP5 bidirectional-pipe pattern** to support the GO/STOP
synchronisation protocol required for second-by-second safety monitoring.

| Pattern | TP5 (reference) | Current implementation |
|---------|----------------|------------------------|
| Two pipes per flight | `pipe(pos_fd); pipe(ctrl_fd);` | Identical |
| Child closes unused ends | `close(pos_fd[0]); close(ctrl_fd[1]);` | Identical |
| Child writes position | `write(pos_write_fd, &pos, sizeof(pos))` | Identical |
| Child reads token | `read(ctrl_read_fd, &token, 1)` | Identical |
| Parent sequential read | `read(pos_read_fd[i], …)` per active flight | Identical |
| Parent sends token | `write(ctrl_write_fd[i], &g, 1)` | Identical |
| Parent waits | `waitpid(pids[i], &status, 0)` + `WIFEXITED` | Identical |

---

## 5. Implementation

### 5.1 Files Modified

| File | Change summary |
|------|---------------|
| `simulation/types.h` | Added `speed_knots`, `heading_deg` to `aircraft_position_t`; replaced `altitude_meters` in `segment_t` with `mode[16]`, `alt_from_meters`, `alt_to_meters`; **added `geo_boundary_t`, `aca_state_t`; expanded `flight_history_t` with `aca_state`** |
| `simulation/flight_data.h` | Changed declaration from `create_sample_flight_plan(void)` to `create_flight_plan(int index)` |
| `simulation/flight_data.c` | Implemented 3 distinct OPO→MAD flight plans from `Flight_Plan_v0c.json`; fixed OPO→LIS destination bug |
| `simulation/flight_process.c` | Signature changed to `(pos_write_fd, ctrl_read_fd, plan)`; physics-driven `while(1)` loop replaces fixed `STEP_COUNT`; `lookup_perf()` replaces constant speed-by-phase; `nanosleep()` removed; reads ctrl token after each write; SIGUSR1 handler added (US102) |
| `simulation/ipc.c` | Removed `send_position`, `recv_position`, `create_pipe`, `close_pipe`, `pipe_t`; **`store_position` replaced by `find_or_create_flight`**; `print_history` updated to show ACA state |
| `simulation/ipc.h` | **`store_position` replaced by `find_or_create_flight`** |
| `simulation/main.c` | Full rewrite — two pipes per flight (pos + ctrl), sequential blocking reads (TP5 pattern), `waitpid` + `WIFEXITED`; ACA boundary filter + ENTERING/EXITING detection; GO/STOP control protocol (US102) |
| `simulation/aca_filter.c` | **New — `is_in_aca()` rectangular boundary check** |
| `simulation/aca_filter.h` | **New — public interface for ACA filter** |
| `libs/scripts/build_c.sh` | Added `-Wall -Wextra -g -lm` flags |

### 5.2 Key Implementation Details

**Atomic writes** — Each `write(pipe_fd, &pos, sizeof(aircraft_position_t))` transfers
one struct (≈ 128 bytes). POSIX guarantees atomicity for writes ≤ PIPE_BUF (512 bytes
minimum), so no message framing or synchronisation primitives are needed.

**EOF signalling** — When a child calls `close(pipe_fd)` and `exit()`, the kernel
decrements the open-writer count on the pipe. Once all three children have exited and
closed their write ends, the count reaches zero and `read()` in the parent returns 0
(EOF), ending the loop cleanly.

**Child memory isolation** — After `fork()`, each child inherits copies of all three
`flight_plan_t*` pointers. They only use `plans[i]` (their own plan) and never touch
the others. The parent calls `free_flight_plan()` after `waitpid()` to release memory
on its side.

**`-lm` linker flag** — Required because `flight_process.c` calls `atan2()` and `M_PI`
from `<math.h>`. On Linux/macOS these are not part of libc and must be linked explicitly.

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

### Expected output

```
=== Flight Simulation (US101 & US102) ===
Mode: normal
ACA: lat [36.0, 43.0]  lon [-10.0, -6.0]

Flight loaded: FLIGHT_01
Flight loaded: FLIGHT_02
Flight loaded: FLIGHT_03

=== Live Position Updates (ACA only) ===
[FLIGHT_01] >>> ENTERING ACA
[FLIGHT_01] lat=41.2629 lon=-8.6852 alt=81m spd=210kt hdg=42.5 vz=12.0m/s
[FLIGHT_02] >>> ENTERING ACA
[FLIGHT_02] lat=41.5629 lon=-8.6852 alt=81m spd=210kt hdg=42.5 vz=12.0m/s
[FLIGHT_03] >>> ENTERING ACA
[FLIGHT_03] lat=40.9629 lon=-8.6852 alt=81m spd=210kt hdg=42.5 vz=12.0m/s
...
[FLIGHT_02] <<< EXITING ACA
...
[FLIGHT_01] <<< EXITING ACA
...
[FLIGHT_03] <<< EXITING ACA

=== Position History (ACA only) ===
Flight FLIGHT_01: 25 positions inside ACA [exited ACA]
  [0] lat=41.2629 lon=-8.6852 alt=81m spd=210kt hdg=42.5 vz=12.0m/s
  ...
Flight FLIGHT_02: 22 positions inside ACA [exited ACA]
  ...
Flight FLIGHT_03: 29 positions inside ACA [exited ACA]
  ...
[FLIGHT_01] ended with code 0
[FLIGHT_02] ended with code 0
[FLIGHT_03] ended with code 0
```

Step counts per flight differ because they are derived from real distance and altitude-
interpolated speed (AC3): climb starts at 210 kt (table entry at 0 m), increasing as
altitude rises; cruise steps end when the aircraft crosses lon = −6.0° (the ACA's eastern
boundary). Positions from the descent segment (approaching MAD at −3.56°E) are outside
the ACA and never stored. All three flights show ENTERING and EXITING events.

---

## 7. Observations

**Anticipation of safety violations (AC3)** — The `speed_knots` and `heading_deg` fields
stored in every `aircraft_position_t` record enable a future process to project each
aircraft's position forward in time using simple dead-reckoning:

```
heading_rad = heading_deg * (π / 180)
future_lat  = lat + speed * cos(heading_rad) * dt
future_lon  = lon + speed * sin(heading_rad) * dt
```

This data is available in the history from this user story. The detection and alerting
logic is the subject of US102 / US103.

**Ordering of live updates** — The interleaving order of messages from the three
processes varies between runs depending on OS scheduler decisions. This is expected and
correct — the pipe's atomic-write guarantee ensures no struct is ever partially written
or corrupted, but it does not impose ordering between independent writers.

**ACA filtering (AC2/AC3)** — The parent calls `is_in_aca()` (in `aca_filter.c`) for
every position received from the pipe. Only positions whose latitude/longitude fall within
the configured `geo_boundary_t` rectangle are stored in the history or printed. Positions
outside the ACA are processed only to update the flight's ACA state.

**Entry/exit detection** — Each flight history slot carries an `aca_state_t` field
(`ACA_BEFORE`, `ACA_INSIDE`, `ACA_AFTER`). The parent transitions the state on the first
in-boundary position ("ENTERING") and on the first out-of-boundary position after having
been inside ("EXITING"). If a flight is never seen inside the ACA, `print_history`
reports "never entered ACA".

**Physics-derived step count (AC3)** — `flight_process.c` no longer uses a fixed
`STEP_COUNT`. For each segment it runs a `while(1)` loop using `lookup_perf()` to
interpolate speed and vertical rate from the altitude-indexed performance table.
`STEP_SECONDS = 1` (one second per step). This ensures that a longer cruise segment
generates proportionally more position reports than a short climb segment — matching
a second-by-second model. The `timestamp` in every `aircraft_position_t` advances by
`STEP_SECONDS` per step.

**TP5 pattern replaces ex1-6.c** — The initial US101 implementation used a single shared
pipe (ex1-6.c pattern). US102 replaced it with two pipes per flight (TP5 pattern) so
the parent can send GO/STOP tokens back to each child, ensuring all positions compared in
the safety check are from the same simulation second.
# US102 — Monitor Safety Violations Between Flights

## 1. Context

US102 builds on top of US101's process-and-pipe architecture to add **safety cylinder
collision detection** between concurrent flights. The parent simulation process, after
collecting a position update from every active flight, checks whether any pair of
aircraft has crossed inside the safety cylinder defined by the air traffic controller.

The implementation is written in C and lives entirely under `aisafe.base/simulation/`.


---

## 2. Requirements

**US102:** As a simulation system, I want to continuously monitor aircraft positions for
overlaps so that I can identify and report safety violations.

### Acceptance Criteria

| ID | Criterion | Status |
|----|-----------|--------|
| AC1 | The system must detect when two or more aircraft may eventually violate safety rules | Done |
| AC2 | Upon detecting a violation, the system should log the event and notify the involved aircraft via signals | Done |
| AC3 | Each flight process must handle the received signal and notify the system user with a message | Done |
| AC4 | When a flight process receives a SIGUSR1 (violation detected), it should block other signals while handling it | Done |
| AC5 | The system should allow early termination if safety violations exceed a predefined threshold by sending termination signals to aircrafts | Done |
| AC6 | Flight processes properly handle termination signals and perform any necessary cleanup | Done |

---

## 3. Analysis

### 3.1 Safety Cylinder

The International Civil Aviation Organization (ICAO) minimum separation standard used in
this simulation as default values is:

| Dimension | Default value | Config key |
|-----------|--------------|------------|
| Horizontal | 8 Nautical Miles ≈ 14 816 m | `safe_dist_horiz_m` |
| Vertical | 600 m | `safe_dist_vert_m` |

Both thresholds are read from `simulation.conf` at startup via `simulation_params_t` and
passed down through `monitor_safety_violations`. A violation occurs when **both** conditions
are breached simultaneously: `d_horiz < safe_dist_horiz_m` AND `d_vert < safe_dist_vert_m`.

### 3.2 Motion Vector Approach

Because the simulation advances one second at a time, two aircraft can in theory "jump
over" each other between consecutive time steps if their speed is high enough. To prevent
missed intersections, the check interpolates the displacement vector of each aircraft into
11 sub-steps (indices 0 to `SUB_STEPS` inclusive, where `#define SUB_STEPS 10`) and
evaluates the safety cylinder at each sub-step position.

### 3.3 Synchronised Step-by-Step Simulation

The key architectural requirement of US102 SCOMP is that **the parent decides whether it
is safe to advance** before any child moves to the next second. This requires bidirectional
communication:

```
CHILD sends position → PARENT collects ALL positions → PARENT checks safety
                                                              ↓
                                              safe: sends GO to each child
                                              unsafe: sends STOP + SIGUSR1
```

Without synchronisation (previous design), a child could be several seconds ahead of the
slowest child when the parent ran the check, making the comparison of non-contemporaneous
positions meaningless.

### 3.4 Flight Profile Performance Table

The flight plan JSON (`Flight_Plan_v0c.json`) defines altitude-indexed tables for climb and
descent phases:

- **Climb table**: for each altitude band (0 m … 12 000 m), gives horizontal speed (knots)
  and rate of climb (m/s).
- **Descend table**: same structure; vertical rate is negative.
- **Cruise**: single constant speed (460 kt), vertical rate 0.

At each simulation second the child looks up its current altitude in the relevant table and
linearly interpolates to obtain the exact speed and vertical rate, giving a physically
realistic trajectory.

---

## 4. Design

### 4.1 Process and Pipe Architecture

```
                        ┌──────────────┐
                        │    PARENT    │
                        │  (main.c)   │
                        └──────┬───────┘
          ┌───────────────────┼────────────────────┐
    pos_pipe[0]         pos_pipe[1]          pos_pipe[2]
    ctrl_pipe[0]        ctrl_pipe[1]         ctrl_pipe[2]
          │                   │                    │
    ┌─────▼──────┐      ┌─────▼──────┐      ┌─────▼──────┐
    │  FLIGHT_01 │      │  FLIGHT_02 │      │  FLIGHT_03 │
    │(child proc)│      │(child proc)│      │(child proc)│
    └────────────┘      └────────────┘      └────────────┘
```

Each flight has **two** unnamed POSIX pipes:

| Pipe | Direction | Purpose |
|------|-----------|---------|
| `pos_pipe` | child → parent | sends `aircraft_position_t` each second |
| `ctrl_pipe` | parent → child | sends `'G'` (go) or `'S'` (stop) |

The parent reads positions from active flights sequentially (blocking read per flight,
TP5 pattern). Since all children block on `ctrl_read_fd` until they receive a token,
no child can advance past step T until the parent has read from every active flight and
sent the GO/STOP decision — this guarantees synchronised time steps without requiring
`select()`.

### 4.2 Data Structures (types.h)

```c
/* One row of the altitude-indexed performance table */
typedef struct {
    double altitude_m;
    double speed_knots;
    double vertical_rate_mps;   /* positive = climb, negative = descend */
} perf_point_t;

/* Flight Profile: Climb/Descend tables + cruise speed */
typedef struct {
    perf_point_t climb[MAX_PERF_POINTS];
    int          climb_count;
    perf_point_t descend[MAX_PERF_POINTS];
    int          descend_count;
    double       cruise_speed_knots;
} flight_profile_t;
```

`leg_t` was extended with a `flight_profile_t profile` field.

`aircraft_position_t` was extended with `double vz_mps` (vertical rate at the reported
second, sent to the parent for logging).

### 4.3 Synchronisation Protocol (sequence per step T)

```
CHILD_i                              PARENT
──────────────────────────────────────────────────────────────
compute position at step T
write(pos_write_fd, &pos)  ─────────►
                                     sequential blocking read() for each
                                     active flight (TP5 pattern) until ALL
                                     have written their position for step T
read(ctrl_read_fd, &token)  ◄────────
                                     US101: ACA filter + history update
                                     US102 current: check safety cylinder
                                     US102 future:  predict future segments (advisory)
                                     ─────────────────────────────────
if safe:                             write('G', ctrl_write_fd[i]) for each child
  token == 'G' → next step ◄────────
                                     ─────────────────────────────────
if violation limit reached:          kill(pids[k], SIGTERM) for ALL flights
                                     write('S', ctrl_write_fd[i]) for each child
  token == 'S' → exit(1)  ◄────────
if single violation:                 kill(pid_i, SIGUSR1); kill(pid_j, SIGUSR1)
                                     write('G', ctrl_write_fd[k]) for each child
```

### 4.4 Safety Cylinder Check Algorithm (safety_monitor.c)

```
for each updated flight i:
  for each other active flight j (j > i, has ≥ 2 positions):
    for sub_step in 0..SUB_STEPS (10):
      t = sub_step / SUB_STEPS
      p1_interp = prev[i] + t * (curr[i] - prev[i])
      p2_interp = prev[j] + t * (curr[j] - prev[j])
      d_horiz = equirectangular_distance(p1_interp, p2_interp)
      d_vert  = |alt1 - alt2|
      if d_horiz < 14816 AND d_vert < 600:
        → VIOLATION
```

### 4.5 Future Collision Prediction (safety_monitor.c)

```c
int predict_future_collisions(flight_plan_t *const *plans,
                              int flight_a, int current_seg_a,
                              int flight_b, int current_seg_b);
```

For each pair of remaining segments `(sa ≥ current_seg_a, sb ≥ current_seg_b)`, the
start/end positions of the two segments are treated as motion vectors and passed to
`check_trajectory_intersection()`. If any future pair intersects the cylinder, a warning
is printed. This check is **advisory only** — it logs a prediction but does not trigger
STOP or SIGUSR1.

---

## 5. Implementation — Files Modified

### `simulation/types.h`

- Added `#define MAX_PERF_POINTS 20`
- Added `perf_point_t` struct
- Added `flight_profile_t` struct
- `leg_t`: added `flight_profile_t profile`
- `aircraft_position_t`: added `double vz_mps`

### `simulation/flight_data.c`

- All segment waypoints (not only the departure point) are offset by `lat_offset`, so
  the 3 normal flights fly truly parallel routes and never get within 8 NM of each other.
- Each leg is populated with the full 13-point Climb and Descend performance tables taken
  directly from `Flight_Plan_v0c.json` (hardcoded; no parser needed).
- Added `case 3` returning `FLIGHT_04` at `lat_offset = 0.02°` (~2.2 km from FLIGHT_01)
  for the collision test scenario.

### `simulation/flight_process.h` / `flight_process.c`

- Signature changed:
  ```c
  void execute_flight_process(int pos_write_fd, int ctrl_read_fd,
                              const flight_plan_t *plan);
  ```
- Added `lookup_perf()`: linear interpolation in the performance table at the current
  altitude, returning horizontal speed (knots) and vertical rate (m/s).
- Loop changed from fixed step count to a `while` loop that runs until the segment's
  target altitude (climb/descend) or target position (cruise) is reached.
- After each `write(pos_write_fd, …)` the child blocks on `read(ctrl_read_fd, &token)`.
  Exit condition: `collision_alert || r <= 0 || token == 'S'` — covers three cases: SIGUSR1
  set the flag, the pipe was closed (parent exited), or an explicit STOP token arrived.
- `nanosleep()` removed — the pipe round-trip provides natural pacing.
- Step size: `STEP_SECONDS = 1` (one simulation second per pipe round-trip).

### `simulation/main.c`

- Flight count is read from `simulation.conf` (`params.n_flights`) for normal mode; a single
  `#define N_FLIGHTS_COLLISION 4` in `main.c` covers the collision-test scenario:
  ```c
  #define N_FLIGHTS_COLLISION 4   /* adds FLIGHT_04 (~2km from FLIGHT_01) */
  ```
- `argc`/`argv` parsed: `--collision` flag overrides the config value with `N_FLIGHTS_COLLISION`.
- `flight_pipes_t` struct extended with `ctrl_write_fd` / `ctrl_read_fd`.
- Fork setup: each child closes all file descriptors except its own `pos_write_fd[i]`
  and `ctrl_read_fd[i]`.
- Parent main loop rewritten as **collect-all-then-decide**:
    1. Sequential blocking `read()` per active flight (TP5 pattern) until every active
       flight has delivered its position for the current step. No `select()` is used.
    2. US101 (ACA filter + history) and US102 (cylinder check) are run once all positions
       are in.
    3. If safe: `write('G', …)` to every active flight.
    4. If a single violation is detected (below threshold): `kill(SIGUSR1)` to both
       violating flights; `write('G', …)` to all active flights so the step continues.
    5. If violation limit reached: `kill(SIGTERM)` to all flights (inside
       `monitor_safety_violations`), then `write('S', …)` to every active flight via
       the control pipe, break out of main loop.
- EOF on `pos_read_fd[i]` closes both `pos_read_fd[i]` and `ctrl_write_fd[i]` and marks
  `active[i] = 0`.

### `simulation/safety_monitor.h` / `safety_monitor.c`

- Added `predict_future_collisions()` (see §4.5).
- Existing `monitor_safety_violations()` and `check_trajectory_intersection()` unchanged.

---

## 6. How to Build and Run

```bash
# From repository root
bash aisafe.base/libs/scripts/build_c.sh

# Normal simulation — 3 parallel flights, no collision
./aisafe.base/bin/simulation

# Collision test — 4 flights; FLIGHT_04 triggers cylinder alert immediately
./aisafe.base/bin/simulation --collision
```

### Expected Output — Normal Mode

```
=== Flight Simulation (US101 & US102) ===
Mode: normal
...
[FLIGHT_01] >>> ENTERING ACA
[FLIGHT_01] lat=41.2629 lon=-8.6852 alt=81m spd=210kt hdg=42.5 vz=12.0m/s
[FLIGHT_02] >>> ENTERING ACA
[FLIGHT_02] lat=41.5629 lon=-8.6852 alt=81m spd=210kt hdg=42.5 vz=12.0m/s
[FLIGHT_03] >>> ENTERING ACA
[FLIGHT_03] lat=40.9629 lon=-8.6852 alt=81m spd=210kt hdg=42.5 vz=12.0m/s
...
[FLIGHT_01] <<< EXITING ACA
[FLIGHT_02] <<< EXITING ACA
[FLIGHT_03] <<< EXITING ACA
```

No `CYLINDER ALERT` is printed. All children exit with code 0.

### Expected Output — Collision Mode (`--collision`)

```
=== Flight Simulation (US101 & US102) ===
Mode: COLLISION TEST
...
[FLIGHT_04] >>> ENTERING ACA
[CYLINDER ALERT] Intersection risk detected at ...
Flights: FLIGHT_01 and FLIGHT_04 crossed paths (H < 8NM and V < 600m).
...
[FLIGHT_01] terminou o voo.
[FLIGHT_04] terminou o voo.
```

FLIGHT_01 and FLIGHT_04 receive SIGUSR1 and terminate. FLIGHT_02 and FLIGHT_03 continue
(they are 33 km apart from either violating flight).

---

## 7. Key Design Decisions

| Decision | Reason |
|----------|--------|
| Bidirectional pipes (pos + ctrl per flight) | Required so parent controls simulation tempo — children must not advance before the parent verifies safety |
| Collect-all-then-decide | Ensures positions compared are contemporaneous (same simulation second); reacting per-flight would compare flight A at step T+5 with flight B at step T |
| Sequential blocking read per flight (TP5 pattern) | Children cannot advance until they receive a ctrl token — the pipe back-pressure provides natural synchronisation; `select()` is not needed and would add complexity |
| `STEP_SECONDS = 1` | "Second-by-second" as specified; the climb table entry at 0 m gives vz=12 m/s → altitude increases 12 m per step, matching the JSON table |
| lat_offset applied to all waypoints | Keeps the 3 normal flights on truly parallel routes; applying it only to the departure caused them to converge to the same cruise waypoint and trigger false alerts |
| Performance table (lookup_perf) | Speed and vertical rate vary with altitude, matching the Flight Profile JSON; removes the unrealistic constant-speed-per-phase approximation |
| `predict_future_collisions` advisory | Prediction ≠ current violation; aborting on prediction would be over-engineered and is not required by the US |
| SIGUSR1 (not SIGTERM) per violating pair | Allows the rest of the simulation to continue; SIGTERM is reserved for when MAX_VIOLATIONS is reached |
# US103 — Synchronise Simulation Steps via GO/STOP Control Pipe

## 1. Context

US103 adds the step synchronisation layer on top of US101's position pipe. Without
synchronisation, each flight child would run through its entire plan at full speed —
positions from different children sent to the parent would represent different simulation
seconds and the safety check in US102 would compare non-contemporaneous positions, making
collision detection meaningless.

The solution is a **second control pipe per flight** (parent → child) that carries a
single-byte token: `'G'` (go — safe, advance to next second) or `'S'` (stop — abort).
Every child blocks on `read(ctrl_read_fd)` after writing its position, so no flight can
advance past second T until the parent has finished all safety checks for second T across
every active flight.

The implementation is written in C and lives entirely under `aisafe.base/simulation/`.

---

## 2. Requirements

**US103:** As a simulation engine, I want to synchronize aircraft movements based on
time steps so that I can accurately simulate real-world execution.

### Acceptance Criteria

| ID | Criterion | Status |
|----|-----------|--------|
| AC1 | The simulation must progress step by step | Done |
| AC2 | Each flight process should send position updates at defined intervals | Done |
| AC3 | The main process must ensure all updates for a given time step are processed before advancing to the next step | Done |

---

## 3. Analysis

### 3.1 Problem decomposition

The core issue is **ordering**: if children write positions freely, the parent may receive
FLIGHT_01 at second 120 and FLIGHT_02 at second 80. Comparing those two positions would
give incorrect horizontal separation values.

Two approaches were considered:

| Approach | Description | Decision |
|----------|-------------|----------|
| Shared timer / sleep | Children sleep N seconds per step; parent polls periodically | Rejected — no hard guarantee of contemporaneity, OS scheduling drift |
| Blocking control pipe | Each child waits for a single-byte token before advancing | **Chosen** — deterministic, zero-polling, natural backpressure |

The blocking-pipe approach transforms the simulation into a **lockstep protocol**: the
parent holds the time token. No flight can move to second T+1 until the parent distributes
`'G'` for second T, and the parent only distributes `'G'` after reading all active flight
positions for second T.

### 3.2 Sequential vs. select() in the parent

The parent reads positions **sequentially** (blocking `read()` on each active flight in
turn, TP5 pattern). This is safe because every child is blocked on `read(ctrl_read_fd)` at
the same time — they cannot advance until the parent sends `'G'`, so the parent will
always find data waiting on each active flight's pipe.

A `select()`-based parent would also work but is unnecessary: the lockstep protocol
guarantees that all children are paused and have data ready before the parent starts its
collection round.

---

## 4. Design

### 4.1 Pipe topology

Each flight has two unnamed POSIX pipes:

| Pipe | Direction | Payload | Purpose |
|------|-----------|---------|---------|
| `pos_pipe` | child → parent | `aircraft_position_t` (≈128 B) | position at second T |
| `ctrl_pipe` | parent → child | single byte `'G'` or `'S'` | advance or abort |

```
FLIGHT_i child                          parent (main.c)
──────────────────────────────────────────────────────────────
write(pos_write_fd, &pos)   ──────────►
                                        blocking read() for each active flight i
                                        US101: ACA filter + history
                                        US102: safety cylinder check
                                        ─────────────────────────────
                                        if safe: write('G') to each child
read(ctrl_read_fd, &token)  ◄──────────
token == 'G' → next step
                                        ─────────────────────────────
                                        if violation limit reached: write('S') to each child
read(ctrl_read_fd, &token)  ◄──────────
token == 'S' → close fds, exit(1)
```

### 4.2 Child synchronisation point (`flight_process.c`)

After each `write(pos_write_fd, &pos)`, the child executes:

```c
char token = 0;
ssize_t r = read(ctrl_read_fd, &token, 1);
if (collision_alert || r <= 0 || token == 'S') {
    close(pos_write_fd);
    close(ctrl_read_fd);
    exit(1);
}
/* token == 'G': safe to continue */
```

Three conditions cause the child to exit early:

| Condition | Meaning |
|-----------|---------|
| `collision_alert == 1` | `SIGUSR1` was received (US102 cylinder alert) |
| `r <= 0` | Parent closed the ctrl pipe (parent exited or pipe broken) |
| `token == 'S'` | Parent sent explicit STOP (violation limit reached, US102 AC4) |

At normal plan completion the child calls `close(pos_write_fd); close(ctrl_read_fd); exit(0)` without blocking.

### 4.3 Parent GO/STOP logic (`main.c`)

```c
while (n_active > 0) {
    /* Step 1: collect one position from each active flight (blocking read) */
    for (i = 0; i < n_flights; i++) {
        if (!active[i]) continue;
        ssize_t n = read(pipes[i].pos_read_fd, &pos, sizeof(pos));
        if (n == sizeof(pos)) {
            /* US101 + US102 state update */
        } else {
            /* EOF: flight finished */
            close(pipes[i].pos_read_fd);
            close(pipes[i].ctrl_write_fd);
            active[i] = 0; n_active--;
        }
    }

    if (n_active == 0) break;

    /* Step 2: run US102 safety checks */
    int abort_sim = monitor_safety_violations(...);

    /* Step 3: send GO or STOP */
    if (abort_sim) {
        for (i = 0; i < n_flights; i++) {
            if (active[i]) {
                char s = 'S';
                write(pipes[i].ctrl_write_fd, &s, 1);
                close(pipes[i].ctrl_write_fd);
                active[i] = 0;
            }
        }
        n_active = 0;
        break;
    }
    for (i = 0; i < n_flights; i++) {
        if (active[i]) {
            char g = 'G';
            write(pipes[i].ctrl_write_fd, &g, 1);
        }
    }
}
```

### 4.4 SIGUSR1 interaction

US102 sends `SIGUSR1` to the two violating flights before the parent sends `'G'` or `'S'`
for that step. The child's `handle_sigusr1` sets `collision_alert = 1`. On the next
synchronisation check (`read(ctrl_read_fd)`) the child inspects `collision_alert` first
and exits regardless of the token value. This means:

- A child that receives `SIGUSR1` exits **no later than the next step boundary** — it
  does not abort mid-step, which keeps the pipe protocol clean.
- The parent still sends `'G'` for that step (the violation limit has not been reached
  yet); the `write()` returns `EPIPE` or succeeds depending on race timing. `SIGPIPE` is
  ignored in the parent so either outcome is harmless.

---

## 5. Implementation — Files Modified

### `simulation/flight_process.c`

- Signature changed from `execute_flight_process(int pipe_fd, ...)` to
  `execute_flight_process(int pos_write_fd, int ctrl_read_fd, ...)`.
- Added the blocking `read(ctrl_read_fd, &token, 1)` after every `write(pos_write_fd, ...)`.
- Early exit on `collision_alert`, `r <= 0`, or `token == 'S'`.
- Closes both file descriptors before any `exit()`.

### `simulation/flight_process.h`

- Updated declaration to match new two-fd signature.

### `simulation/main.c`

- `flight_pipes_t` struct extended with `ctrl_write_fd` and `ctrl_read_fd` fields.
- Pipe creation loop now creates two pipes per flight (one for position, one for control).
- Fork setup: each child closes all `pos_read_fd` and `ctrl_write_fd` ends, and all
  pipe ends belonging to other flights.
- Parent closes `pos_write_fd` and `ctrl_read_fd` for all flights after last fork.
- Main loop: sequential blocking read → US101 + US102 → write `'G'` or `'S'`.

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
...
[FLIGHT_01] >>> ENTERING ACA
[FLIGHT_01] lat=41.2629 lon=-8.6853 alt=81m spd=210.0kt hdg=42.5 vz=12.0m/s
[FLIGHT_02] >>> ENTERING ACA
...
[FLIGHT_01] <<< EXITING ACA
[FLIGHT_02] <<< EXITING ACA
[FLIGHT_03] <<< EXITING ACA

=== Position History (ACA only) ===
...
[FLIGHT_01] ended with code 0
[FLIGHT_02] ended with code 0
[FLIGHT_03] ended with code 0
```

Each flight advances one second at a time. No flight's position for second T+1 is ever
sent before the parent has verified safety at second T and returned `'G'`.

### Expected Output — Collision Mode (`--collision`)

```
=== AISafe Flight Simulation ===
Mode: COLLISION TEST
...
[CYLINDER ALERT] Intersection risk detected at ...
Flights: FLIGHT_01 and FLIGHT_04 crossed paths (H < 8NM and V < 600m).
...
[FLIGHT_01] ended with code 1
[FLIGHT_04] ended with code 1
```

FLIGHT_01 and FLIGHT_04 receive `SIGUSR1` and exit with code 1 at their next step
boundary. The simulation aborts when the violation limit (`max_violations`) is reached.

---

## 7. Key Design Decisions

| Decision | Reason |
|----------|--------|
| Blocking `read(ctrl_read_fd)` in child | Zero-polling, deterministic: child cannot advance until parent explicitly permits it |
| Sequential blocking reads in parent | Safe because every child is already paused at `read(ctrl_read_fd)` — data is always waiting; no `select()` needed |
| Single-byte token (`'G'`/`'S'`) | Minimal overhead; a full struct would waste bandwidth on a per-step round-trip that happens thousands of times per simulation |
| `SIGPIPE` ignored in parent | When US102 sends `SIGTERM` to children before the parent writes `'S'`, the pipe has no reader; `SIG_IGN` makes `write()` return `EPIPE` instead of killing the parent |
| Child closes both fds before `exit()` | Prevents EOF on the parent's `pos_read_fd` arriving earlier than expected; also prevents the control pipe fd leaking into grandchild processes |
| `SIGUSR1` exits at step boundary, not immediately | Keeps the pipe protocol clean — the child completes its current `write()`/`read()` round-trip; the parent is never left blocked on a `read()` for a child that has already died |
# US109 - Generate and Store Final Simulation Report

## 1. Context

US109 is the final reporting step of the C flight simulation pipeline. After the
coordinator finishes the simulation, the Flight Control Operator needs a persisted report
with the overall outcome, each flight execution status, the safety violation events, and
the final validation result.

In Sprint 3 the parent process is already multi-threaded (US105/US106). Therefore US109 is
implemented by the dedicated `report_thread`: it waits on the report condition variable,
drains any live safety violation events, and then writes the final report file.

---

## 2. Requirements

**US109:** As a Flight Control Operator, I want a comprehensive report that details the
simulation outcomes, including flight execution statuses, safety violation events with
timestamps, positions and velocity vectors, and overall validation results, so that I can
assess the safety and performance of the flights post-simulation.

### Acceptance Criteria

| ID | Criterion | Status |
|----|-----------|--------|
| AC1 | The report generation thread aggregates data once the simulation concludes. | Done |
| AC2 | The report includes the total number of flights and individual execution statuses. | Done |
| AC3 | The report includes detailed safety violation events with timestamp, positions and velocity vectors. | Done |
| AC4 | The final validation result is clearly indicated as PASS or FAIL. | Done |
| AC5 | The complete report is saved to a file for future reference. | Done |

---

## 3. Analysis

### 3.1 Data Produced by Earlier Simulation Steps

US109 consumes data produced by the previous C simulation stories:

| Source | Data Used by US109 |
|--------|--------------------|
| US101 ACA tracking | `flight_history_t histories[]`, including each flight id, ACA state and position snapshots inside the ACA |
| US102 Safety Cylinder | `total_violations`, abort decision, and each recorded `violation_event_t` |
| US105 Shared memory and report synchronization | `sim_shm_t`, `g_notification_mutex`, `g_report_cond`, and `g_sim_done` |
| US106 Function-specific threads | `report_thread` runs separately from coordinator and safety monitoring |
| US107 Live violation notifications | live violation queue and dropped-event counter |

### 3.2 Why the Report Thread Owns the Final File

The report thread already has one responsibility: wait for simulation data and produce the
reporting output. It blocks on a condition variable instead of polling. When the coordinator
sets `g_sim_done`, the report thread finishes draining the live violation queue, snapshots
the final shared-memory counters, and writes `simulation_report.txt`.

This satisfies Sprint 3 directly: the report is generated by a dedicated thread, not by a
new process.

### 3.3 Final Validation Rule

The final validation result is:

| Result | Condition |
|--------|-----------|
| `PASS` | The simulation completed normally and no safety violations were detected |
| `FAIL` | The simulation aborted or at least one safety violation was detected |

The report also keeps the more operational `Simulation End Status` line, so the operator can
distinguish a normal completion with violations from a threshold-triggered abort.

---

## 4. Design

### 4.1 End-of-Simulation Synchronization

```
coordinator_thread                         report_thread
------------------                         -------------
collects positions
coordinates safety_thread
simulation loop ends
lock(g_notification_mutex)
  shm->total_violations = ...
  shm->sim_aborted = ...
  g_sim_done = 1
  signal(g_report_cond)    -------------> wakes
unlock                                    drains violation_events[]
                                          reads final counters
                                          writes simulation_report.txt
```

The report thread uses a `while` predicate around `pthread_cond_wait`, matching the POSIX
condition-variable pattern already used by US105/US106.

### 4.2 Report File Structure

`simulation_report.txt` is overwritten on every run and contains:

1. Header with generation timestamp.
2. Simulation end status.
3. Final validation result (`PASS` or `FAIL`).
4. Total number of flights.
5. Total safety violations, stored event count, dropped event count, and live log file path.
6. Detailed safety violation events, including:
   - event timestamp;
   - flight pair;
   - horizontal and vertical separation;
   - position and velocity vector for each aircraft (`lat`, `lon`, `alt`, `speed`, `heading`, `vertical_rate`).
7. Per-flight execution status:
   - aircraft identifier;
   - execution status (`COMPLETED` or `STOPPED (simulation aborted)`);
   - ACA status (`Never entered ACA`, `Inside ACA`, `Exited ACA`);
   - number of snapshots logged inside the ACA;
   - recorded trajectory snapshots with speed, heading and vertical rate.

---

## 5. Implementation - Files Modified

| File | Change Summary |
|------|----------------|
| `simulation/main.c` | `report_thread` now passes the final violation event array and count to the final report writer; the system message now states that the report thread is generating the final report. |
| `simulation/report.h` | `generate_final_report` signature extended with `violation_event_t` array and event count. |
| `simulation/report.c` | Removed the obsolete `fork()`-based report process; `generate_final_report` now writes the file directly from `report_thread` and embeds detailed violation events plus PASS/FAIL validation result. |

### Key Code Path

```c
while (processed_events < ctx->shm->violation_event_count || !*ctx->g_sim_done) {
    while (processed_events >= ctx->shm->violation_event_count && !*ctx->g_sim_done)
        pthread_cond_wait(ctx->g_report_cond, ctx->g_notification_mutex);
    ...
}

generate_final_report(ctx->histories, ctx->n_flights,
                      violation_events, violation_event_count,
                      total_violations, dropped_events, sim_aborted);
```

The report file is written with standard C file I/O (`fopen`, `fprintf`, `fclose`) because
the report writer runs in a normal thread context, not inside a signal handler.

---

## 6. Integration / Testing

### Build

```bash
cd aisafe.base/simulation
make clean && make
```

Expected result: the simulator compiles with `-Wall -Wextra -std=c99`.

### Normal Simulation

```bash
./flight_simulator
```

Expected result:

- the report thread wakes after the simulation concludes;
- `simulation_report.txt` is created;
- `Final Validation Result: PASS` when no safety violations occur;
- all flights appear in the `FLIGHT EXECUTION STATUSES` section.

### Collision Simulation

```bash
./flight_simulator --collision
```

Expected result:

- safety violation events are recorded;
- `simulation_report.txt` contains a `SAFETY VIOLATION EVENTS` section with timestamps,
  positions and velocity vectors;
- `Final Validation Result: FAIL`;
- `Simulation End Status` indicates abort if the configured threshold is reached.

### Environment Note

The C simulator requires a native POSIX toolchain with `make`, `gcc`, POSIX shared memory,
named semaphores and pthreads. On the current Windows Codex environment, `make` and `gcc`
are not installed, so compilation must be executed in the target Linux/WSL environment.

---

## 7. Design Decisions

| Decision | Reason |
|----------|--------|
| Use `report_thread` instead of `fork()` | Sprint 3 requires the report generation thread to aggregate final data after simulation conclusion. |
| Keep live violation log and final report | US107 benefits from a live append-only log, while US109 needs the complete final summary file. |
| Store detailed violation events in the final report | Directly satisfies the acceptance criterion for timestamped events with positions and velocity vectors. |
| Write PASS/FAIL explicitly | Gives the Flight Control Operator a quick validation result without inferring it from counters. |
| Overwrite `simulation_report.txt` each run | Ensures the saved report always corresponds to the latest simulation execution. |
