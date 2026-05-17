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
future_lat = lat + speed * cos(heading) * dt
future_lon = lon + speed * sin(heading) * dt
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
