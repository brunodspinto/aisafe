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
this simulation is:

| Dimension | Value |
|-----------|-------|
| Horizontal | 8 Nautical Miles = 14 816 m |
| Vertical | 600 m |

A violation occurs when **both** conditions are breached simultaneously: `d_horiz < 14816 m`
AND `d_vert < 600 m`.

### 3.2 Motion Vector Approach

Because the simulation advances one second at a time, two aircraft can in theory "jump
over" each other between consecutive time steps if their speed is high enough. To prevent
missed intersections, the check interpolates the displacement vector of each aircraft into
10 sub-steps and evaluates the safety cylinder at each sub-step position.

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
                                     select() until ALL active children
                                     have written their position for step T
read(ctrl_read_fd, &token)  ◄────────
                                     US101: ACA filter + history update
                                     US102 current: check safety cylinder
                                     US102 future:  predict future segments (advisory)
                                     ─────────────────────────────────
if safe:                             write('G', ctrl_write_fd[i]) for each child
  token == 'G' → next step ◄────────
                                     ─────────────────────────────────
if violation:                        write('S', ctrl_write_fd[i]) for each child
  token == 'S' → exit(1)  ◄────────  kill(pid_violator, SIGUSR1)
```

### 4.4 Safety Cylinder Check Algorithm (safety_monitor.c)

```
for each updated flight i:
  for each other active flight j (j ≠ i, has ≥ 2 positions):
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
  If `token == 'S'`, the child closes its file descriptors and calls `exit(1)`.
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
  1. Inner `select()` loop runs until every active flight has delivered its position for
     the current step.
  2. US101 (ACA filter + history) and US102 (cylinder check) are run once all positions
     are in.
  3. If safe: `write('G', …)` to every active flight, reset `received[]`.
  4. If violation limit reached: `write('S', …)` + `SIGUSR1` to violating pair,
     break out of main loop.
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
