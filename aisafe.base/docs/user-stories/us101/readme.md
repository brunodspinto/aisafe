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

**US101:** As an Air Traffic Controller, I want the simulation to capture and process flight
movement updates so that the system can track aircraft positions in real time and store
enough data to anticipate future safety violations.

### Acceptance Criteria

| ID | Criterion | Status |
|----|-----------|--------|
| AC1 | Each flight process sends position updates to the parent via a POSIX pipe | Done |
| AC2 | The parent process reads and displays live position updates | Done |
| AC3 | The system stores past positions including speed and heading to enable future anticipation of safety violations | Done |
| AC5 | Only positions inside the Air Control Area (ACA) boundary are stored and displayed | Done |
| AC6 | Aircraft entry into and exit from the ACA are detected and logged at runtime | Done |
| AC7 | Simulation step count is derived from real physics (distance ÷ speed), giving a second-by-second model | Done |

---

## 3. Analysis

### 3.1 Problem decomposition

The simulation involves N concurrent flights. Each flight follows a route divided into
segments (climb, cruise, descend). At each interpolation step within a segment the flight
process must report its current position, altitude, speed and heading to a central process
that aggregates the data.

The key design question is **how the child processes communicate with the parent**. Two
approaches were considered:

| Approach | Description | Decision |
|----------|-------------|----------|
| One pipe per flight + `select()` | Each child writes to its own pipe; parent uses `select()` to multiplex | Rejected — more complex, not aligned with professor's style |
| One shared pipe, N children | All children write to the same pipe; parent reads in a single loop | **Chosen** — matches ex1-6.c exactly |

The one-shared-pipe approach is safe because each `write()` of `sizeof(aircraft_position_t)`
(≈ 128 bytes) is below PIPE_BUF (minimum 512 bytes on POSIX). Writes of this size are
**atomic** — the kernel guarantees they are never interleaved between processes, so no
framing or locking is needed.

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
    time_t timestamp;
    char flight_id[64];
} aircraft_position_t;
```

`speed_knots` and `heading_deg` were added for AC3 — they allow the parent to project
a future position from the last known position, which is the foundation of safety-violation
anticipation.

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

The architecture mirrors professor's **ex1-6.c** exactly:

```
main process
│
├── pipe(fd)          ← one shared pipe
│
├── fork() ──► FLIGHT_01 child
│               close(fd[0])
│               write(fd[1], &pos, sizeof(pos))  ×15
│               close(fd[1])
│               exit(0)
│
├── fork() ──► FLIGHT_02 child
│               (same pattern)
│
├── fork() ──► FLIGHT_03 child
│               (same pattern)
│
close(fd[1])          ← parent closes write end
│
while(read(fd[0], …) > 0)
│   store_position()
│   printf(…)
│
close(fd[0])
print_history()
│
for each child: waitpid() + WIFEXITED check
```

The `read()` loop in the parent exits naturally when all three children have closed their
end of the pipe (after `close(pipe_fd)` and `exit()` in each child), producing EOF.

### 4.3 Flight Process (`flight_process.c`)

`execute_flight_process(int pipe_fd, const flight_plan_t *plan)` is called from the
child after `close(fd[0])`. It:

1. Iterates over each leg and segment.
2. For each of `STEP_COUNT` (5) steps per segment, computes:

   **Position** — linear interpolation along the segment:
   ```c
   pos.latitude  = seg->from.latitude  + (seg->to.latitude  - seg->from.latitude)  * progress;
   pos.longitude = seg->from.longitude + (seg->to.longitude - seg->from.longitude) * progress;
   ```

   **Altitude** — linear interpolation between segment end-points:
   ```c
   pos.altitude_meters = seg->alt_from_meters
                       + (seg->alt_to_meters - seg->alt_from_meters) * progress;
   ```

   **Speed** — constant by flight phase:
   ```c
   if      (strcmp(seg->mode, "climb")  == 0) pos.speed_knots = 250.0;
   else if (strcmp(seg->mode, "cruise") == 0) pos.speed_knots = 460.0;
   else                                        pos.speed_knots = 220.0;
   ```

   **Heading** — true bearing derived from the segment vector:
   ```c
   double dlat = seg->to.latitude  - seg->from.latitude;
   double dlon = seg->to.longitude - seg->from.longitude;
   pos.heading_deg = atan2(dlon, dlat) * 180.0 / M_PI;
   if (pos.heading_deg < 0) pos.heading_deg += 360.0;
   ```

3. Writes the struct directly to the pipe:
   ```c
   write(pipe_fd, &pos, sizeof(aircraft_position_t));
   ```

4. Sleeps 50 ms between steps (simulated propagation delay).

5. On completion: `close(pipe_fd); exit(EXIT_SUCCESS);`

### 4.4 IPC Layer (`ipc.c` / `ipc.h`)

The IPC module is kept minimal. The `send_position()` / `recv_position()` wrapper
functions and the `pipe_t` helper struct have been removed — the professor's pattern
uses direct `write()` / `read()` calls without wrappers. Only two functions remain:

| Function | Signature | Purpose |
|----------|-----------|---------|
| `store_position` | `(flight_history_t *histories, int n, const aircraft_position_t *pos)` | Appends a position to the correct flight history slot |
| `print_history` | `(const flight_history_t *histories, int n)` | Prints the full recorded history after the simulation ends |

### 4.5 Main Loop (`main.c`)

The parent's receive loop follows ex1-6.c precisely:

```c
/* parent: close write end */
close(fd[1]);

while (read(fd[0], &pos, sizeof(aircraft_position_t)) > 0) {
    printf("[%s] lat=%.4f lon=%.4f alt=%.0fm spd=%.0fkt hdg=%.1f\n", …);
    store_position(histories, MAX_FLIGHTS, &pos);
}
close(fd[0]);

print_history(histories, MAX_FLIGHTS);

for (i = 0; i < N_FLIGHTS; i++) {
    waitpid(pids[i], &status, 0);
    if (WIFEXITED(status))
        printf("[FLIGHT_%02d] ended with code %d\n", i+1, WEXITSTATUS(status));
}
```

### 4.6 Alignment with Professor's Examples

| Pattern | ex1-6.c (reference) | US101 implementation |
|---------|--------------------|-----------------------|
| Pipe creation | `int fd[2]; pipe(fd);` | Identical |
| Child closes read end | `close(fd[0]);` | `close(fd[0]);` before `execute_flight_process` |
| Child writes data | `write(fd[1], &data, sizeof)` | `write(pipe_fd, &pos, sizeof(pos))` |
| Child closes + exits | `close(fd[1]); exit(0);` | `close(pipe_fd); exit(EXIT_SUCCESS);` |
| Parent closes write | `close(fd[1]);` | Identical |
| Parent read loop | `while(read(fd[0], &d, sizeof) > 0)` | Identical |
| Parent waits | `waitpid(pids[i], &status, 0)` + `WIFEXITED` | Identical |

---

## 5. Implementation

### 5.1 Files Modified

| File | Change summary |
|------|---------------|
| `simulation/types.h` | Added `speed_knots`, `heading_deg` to `aircraft_position_t`; replaced `altitude_meters` in `segment_t` with `mode[16]`, `alt_from_meters`, `alt_to_meters`; **added `geo_boundary_t`, `aca_state_t`; expanded `flight_history_t` with `aca_state`** |
| `simulation/flight_data.h` | Changed declaration from `create_sample_flight_plan(void)` to `create_flight_plan(int index)` |
| `simulation/flight_data.c` | Implemented 3 distinct OPO→MAD flight plans from `Flight_Plan_v0c.json`; fixed OPO→LIS destination bug |
| `simulation/flight_process.c` | Added altitude interpolation, speed-by-phase, heading calculation; replaced `send_position()` with direct `write()`; added `close(pipe_fd)` before `exit()`; **AC7: step count derived from distance/speed physics** |
| `simulation/ipc.c` | Removed `send_position`, `recv_position`, `create_pipe`, `close_pipe`, `pipe_t`; **replaced `store_position` with `find_or_create_flight`; updated `print_history` to show ACA state and "never entered ACA"** |
| `simulation/ipc.h` | **Replaced `store_position` with `find_or_create_flight`** |
| `simulation/main.c` | Full rewrite — one shared pipe, no `select()`, `waitpid` + `WIFEXITED`; **AC5/AC6: ACA boundary filter + ENTERING/EXITING detection** |
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
=== Flight Simulation (US101) ===
ACA: lat [36.0, 43.0]  lon [-10.0, -6.0]

Flight loaded: FLIGHT_01
Flight loaded: FLIGHT_02
Flight loaded: FLIGHT_03

=== Live Position Updates (ACA only) ===
[FLIGHT_01] >>> ENTERING ACA
[FLIGHT_01] lat=41.2629 lon=-8.6852 alt=69m spd=250kt hdg=42.5
[FLIGHT_02] >>> ENTERING ACA
[FLIGHT_02] lat=41.5629 lon=-8.6852 alt=69m spd=250kt hdg=57.1
[FLIGHT_03] >>> ENTERING ACA
[FLIGHT_03] lat=40.9629 lon=-8.6852 alt=69m spd=250kt hdg=33.1
...
[FLIGHT_02] <<< EXITING ACA
...
[FLIGHT_01] <<< EXITING ACA
...
[FLIGHT_03] <<< EXITING ACA

=== Position History (ACA only) ===
Flight FLIGHT_01: 25 positions inside ACA [exited ACA]
  [0] lat=41.2629 lon=-8.6852 alt=69m spd=250kt hdg=42.5
  ...
Flight FLIGHT_02: 22 positions inside ACA [exited ACA]
  ...
Flight FLIGHT_03: 29 positions inside ACA [exited ACA]
  ...
[FLIGHT_01] ended with code 0
[FLIGHT_02] ended with code 0
[FLIGHT_03] ended with code 0
```

Step counts per flight differ (22–29) because they are derived from real distance and
speed (AC7): climb ~12 steps, cruise steps end when the aircraft crosses lon = −6.0°
(the ACA's eastern boundary). Positions from the descent segment (approaching MAD at
−3.56°E) are outside the ACA and never stored (AC5). All three flights show ENTERING
and EXITING events (AC6).

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

**`select()` removed** — The previous implementation used `select()` over three separate
pipes. While functionally correct, it diverges from the professor's teaching material.
The single-pipe model eliminates the multiplexing complexity while remaining correct
because of POSIX atomic-write semantics.

**AC5 — ACA filtering** — The parent calls `is_in_aca()` (in `aca_filter.c`) for every
position received from the pipe. Only positions whose latitude/longitude fall within the
configured `geo_boundary_t` rectangle are stored in the history or printed. Positions
outside the ACA are processed only to update the flight's ACA state (AC6).

**AC6 — Entry/exit detection** — Each flight history slot carries an `aca_state_t`
field (`ACA_BEFORE`, `ACA_INSIDE`, `ACA_AFTER`). The parent transitions the state on
the first in-boundary position ("ENTERING") and on the first out-of-boundary position
after having been inside ("EXITING"). If a flight is never seen inside the ACA,
`print_history` reports "never entered ACA".

**AC7 — Physics-derived step count** — `flight_process.c` no longer uses a fixed
`STEP_COUNT`. For each segment it computes the equirectangular distance in metres,
converts speed from knots to m/s, and divides by `STEP_SECONDS` (60 s) to get the
number of steps. This ensures that a longer cruise segment generates proportionally
more position reports than a short climb segment — matching a second-by-second model.
The `timestamp` in every `aircraft_position_t` advances by `STEP_SECONDS` per step.
