# US110 — Integrate Environmental Influences into Simulation

> **Status:** implemented. The acceptance criteria are marked *Done* and all
> files listed in §5 have been created/modified.

## 1. Context

US110 extends the flight simulation (SCOMP/C, US100–US109) so that **climatic
conditions influence the path actually taken by each aircraft**. Until now each
flight child process moved strictly along its planned route; the wind data that
already exists in the flight plan was parsed-but-ignored. US110 makes the wind
**deviate the aircraft laterally** as the simulation advances.

Per the Sprint 3 assignment, US110 is *"only for groups with 5 or more students"*
and is **evaluated as an integral part of US107**: the value of the feature is
demonstrated through real-time safety detection — wind pushes aircraft off their
planned track, which can bring two flights inside the Safety Cylinder (US102), and
that violation is detected, logged and signalled in real time by the US107
notification channel.

The implementation is written in C and lives under `aisafe.base/simulation/`. A
dedicated **parent "environment" thread** loads the wind from a weather service and
writes it into the shared-memory segment each simulation step; each flight child then
reads that shared wind and applies a lateral drift to its position, one step at a
time, consistently with the step-by-step synchronisation of US108. The drift maths is
isolated in the `environment` module and the weather-service source in
`weather_service`, so the shared `flight_process.c` gains only a small read-and-drift block.

---

## 2. Requirements

**US110 — Integrate environmental influences into simulation.**

> As a PO, I want the simulation to incorporate environmental factors such as wind
> into the simulation, so that the flight paths become more realistic and adapt to
> dynamic conditions.

### Acceptance Criteria (from the assignment, p.22)

| ID | Criterion (assignment) | Where met | Status |
|----|------------------------|-----------|--------|
| AC110.1 | The parent process spawns an additional **"environment" thread** at simulation start. | `main.c` — `environment_thread` created as the 4th parent thread alongside coordinator/safety/report; joined after the coordinator. | Done |
| AC110.2 | This thread **loads environmental configuration (wind speed/direction) from a weather service**. | `weather_service.{c,h}` — `weather_service_fetch()` is the weather-service source (reads `AISAFE_WIND="speed,dir"`, calm `0,0` if unset). | Done |
| AC110.3 | Environment data is **written into the shared memory segment at each time step**. | `environment_thread` writes `shm->environment` (+`env_step`) under the `/aisafe_env` mutex semaphore, signalled once per step by `coordinator_thread`. | Done |

**How the wind influences the path:** each flight child reads `shm->environment`
(under the `/aisafe_env` mutex semaphore) every step and applies a lateral drift via
`apply_wind_drift_values()` (`environment.c`) — affecting `lat`/`lon` only, never the
progress variables, so every segment still terminates (US108 lockstep preserved). When
the weather service reports calm, the child falls back to the per-segment wind from the
flight plan, keeping older plans backward-compatible. Wind-drifted positions flow
through the existing Safety Cylinder pipeline (US102/US107) unchanged, so weather-induced
conflicts are detected and logged in real time — the integration point on which US110 is
evaluated.

### Dependencies / References

- **US100/US105** — flight simulation environment (one process per flight; shared
  memory). The wind is applied in the flight child process.
- **US108** — step-by-step synchronisation; the drift is integrated per timestep.
- **US102 / US107** — Safety Cylinder detection and real-time notification, through
  which US110 is demonstrated and evaluated.
- **US082** — *Insert weather data in a flight* (Java side): the domain source of
  the wind data that the flight plan carries.
- **Domain Model V10** — `FlightSegment.windDirection` / `windSpeed`, `WeatherData`.

---

## 3. Analysis

### 3.1 Starting point and the gap

| Concern | State before US110 | Gap addressed by US110 |
|---------|--------------------|------------------------|
| Wind data in the flight plan | `wind_dir_deg`/`wind_speed_mps` present in `flight_plans.json`; `segment_t` has `wind_speed`/`wind_direction` | The parser does **not** read them and the physics does **not** use them (`explanation.md`: *"Weather effects — Not implemented; structures exist for future use"*) |
| Aircraft movement | Moves strictly along the planned route (`flight_process.c`) | No environmental deviation |
| Safety detection | Detects conflicts on reported positions (US102/US107) | Conflicts can only arise from the planned routes, never from weather |

### 3.2 Decomposition decision

The assignment is prescriptive: the wind must come from a **parent "environment"
thread** that loads it from a weather service and writes it into shared memory each
step (AC110.1–3). The drift maths is isolated in the `environment` module; the
weather-service source is isolated in `weather_service`.

| Concern | Mechanism | Rationale |
|---------|-----------|-----------|
| Environment as a parent thread (AC110.1) | `environment_thread` in `main.c` | Required by the assignment; runs concurrently with coordinator/safety/report. |
| Weather-service source (AC110.2) | `weather_service_fetch()` (`weather_service.{c,h}`) | Isolates "where wind comes from" (env var now; a real feed later) from the drift physics. |
| Per-step publication to shm (AC110.3) | `shm->environment` guarded by the named semaphore `/aisafe_env` (value 1) as a mutex; coordinator ticks the env thread each step | A **named semaphore** is the professor's cross-process mutual-exclusion primitive (`ex2-6.c`) and the project's existing convention (`/aisafe_pos_*`, `/aisafe_ctrl_*`); it works across the parent thread and the **forked** child processes. |
| Drift maths | `apply_wind_drift_values()` (`environment.c`) | Stateless; affects lat/lon only (termination invariant). |

### 3.3 Alignment with Domain Model V10

US110 introduces **no new domain concept**; it consumes wind that the model
already represents.

| Domain Model V10 element | Attributes | Mapping in the C simulation |
|--------------------------|------------|-----------------------------|
| **`FlightSegment`** (VO, Flight aggregate) | `windDirection`, `windSpeed`, `allowedAltitudeSlots`, `widthPerAltitudeSlot` | `segment_t.wind_direction` / `wind_speed` ← JSON `wind_dir_deg`/`wind_speed_mps`. **The concrete source of wind in the simulation.** |
| **`WeatherData`** (aggregate; `FlightPlan` *uses* 0..*) | `windSpeed`, `windDirection`, `temperature`, `pressure`, `visibility` | Broader domain source (Java side, US082). The simulation consumes its per-segment projection via the flight plan. |
| **`Node`** (VO) | `latitude`, `longitude`, `altitude` | the segment endpoints (`segment.from`/`to`) defining the planned route the wind acts upon |
| **`SafetyViolation`** (VO, in `SimulationReport`) | `latitude`, `longitude`, `altitude`, `speed`, `heading`, `timestamp` | a wind-induced conflict carries the **drifted** position — where US110 becomes visible |
| **`Simulation`** | `timeStep`, `safetyThreshold` | `STEP_SECONDS` and the Safety Cylinder thresholds — the wind is applied every `timeStep` |

---

## 4. Design

### 4.1 Wind drift model (`environment.h` / `environment.c`)

The wind is a horizontal vector applied as a **lateral drift** added to the
planned-route displacement on every step. Convention (decided): `wind_dir_deg` is
**meteorological** — the direction the wind blows *from* — so the aircraft drifts
towards `wind_dir + 180°`.

```c
/* environment.c — US110 (meteorological convention: wind_dir = where it comes FROM) */
void apply_wind_drift(double *lat, double *lon, const segment_t *seg, double dt) {
    if (seg->wind_speed <= 0.0) return;                          /* no wind → no drift */
    double drift_m = seg->wind_speed * dt;                       /* metres this step   */
    double bearing = (seg->wind_direction + 180.0) * DEG_TO_RAD; /* FROM → TO          */
    double north_m = drift_m * cos(bearing);
    double east_m  = drift_m * sin(bearing);
    *lat += north_m / 110574.0;                                  /* same constants as  */
    *lon += east_m  / (111320.0 * cos(*lat * DEG_TO_RAD));       /* horiz_distance_m   */
}
```

The metre→degree constants (110574 m per degree of latitude, 111320 m per degree
of longitude at the equator, scaled by `cos(lat)`) are exactly those already used
by `horiz_distance_m` in `flight_process.c`, so the drift is dimensionally
consistent with the existing physics.

### 4.2 Integration point (`flight_process.c`)

The drift is applied inside the per-step loop, **after** the planned-route update
and **before** the position is written to shared memory. The wind is read from the
shared-memory environment block (written by the environment thread) under the
`/aisafe_env` mutex semaphore; if the weather service reports calm, the child falls
back to the per-segment wind:

```c
lat += (dlat / dist_total_m) * horiz_step_m;   /* planned route (existing) */
lon += (dlon / dist_total_m) * horiz_step_m;
dist_covered_m += horiz_step_m;
alt += vz * STEP_SECONDS;

environment_t env;                              /* << US110: wind from env thread */
sem_wait(env_sem);                              /* mutex (ex2-6.c) */
env = shm->environment;
sem_post(env_sem);
if (env.wind_speed > 0.0)
    apply_wind_drift_values(&lat, &lon, env.wind_speed, env.wind_direction, STEP_SECONDS);
else
    apply_wind_drift(&lat, &lon, segment, STEP_SECONDS);  /* fallback: plan wind */
/* ... build aircraft_position_t and write to shm + sem_post(pos_sem) ... */
```

### 4.3 Termination safety (the critical correctness rule)

The wind affects **only `lat`/`lon`**. It must **not** touch `dist_covered_m`
(cruise progress) nor `alt` (climb/descend progress). The segment-end conditions
(`is_cruise && dist_covered_m >= dist_total_m`, `is_climb && alt >= alt_target`,
`is_descend && alt <= alt_target`) therefore remain reachable, so every segment
still terminates — no infinite loop. A flight with `wind_speed = 0` (or an absent
key) behaves exactly as before (backward compatible).

### 4.4 Heading vs ground track

The reported `heading` continues to be the **planned-route** bearing. Under wind
the aircraft's *ground track* differs from its heading (the drift is lateral); this
is left as the realistic interpretation and is worth noting in the oral defence.

### 4.5 Alignment with the professor's examples / domain

US110 adds one parent thread and reuses the existing IPC pipeline:

| Mechanism | Origin | Use in US110 |
|-----------|--------|--------------|
| `pthread_create` / `pthread_join` | US105/US106 (`ex1-9.c`) | the **environment thread** is the 4th parent thread |
| Mutex + condition variable (while-pred) | US106/US107 (T7/T8) | coordinator ticks the environment thread once per step |
| Named semaphore as mutex (value 1) | `ex2-6.c` | `/aisafe_env` guards the env block between the parent thread and the forked children |
| Shared memory + `sem_post(pos_sem)` | US105 (`ex1-7.c`, `ex2-5.c`) | the **drifted** position is published to the parent |
| Per-step lock-step | US103/US108 | the drift is integrated once per `STEP_SECONDS` |
| `FlightSegment.windDirection`/`windSpeed` | Domain Model V10 | the per-segment **fallback** wind when the service is calm |

---

## 5. Implementation

### 5.1 Files to Create/Modify

| File | Change summary |
|------|---------------|
| `simulation/weather_service.{c,h}` | **New** — the weather-service source (AC110.2); `weather_service_fetch()` reads wind from `AISAFE_WIND="speed,dir"`, calm if unset. |
| `simulation/environment.h` | `apply_wind_drift` + new `apply_wind_drift_values` prototypes. |
| `simulation/environment.c` | Drift maths refactored into `apply_wind_drift_values`; `apply_wind_drift(seg,…)` is a thin wrapper. Affects lat/lon only. |
| `simulation/types.h` | **New** `environment_t { wind_speed; wind_direction; }`. |
| `simulation/shared_memory.h/.c` | `sim_shm_t` gains `environment` + `env_step`; new `open_env_sem()` opens the `/aisafe_env` mutex semaphore (value 1, `ex2-6.c`); `cleanup_sems` unlinks it. `open_named_sem` gained an init-value parameter. |
| `simulation/main.c` | **New** `environment_thread` (AC110.1) + `environment_ctx_t`; spawned as 4th parent thread and joined; coordinator ticks it once per step (AC110.3); `env_mutex`/`env_cond` (intra-process tick) and `/aisafe_env` semaphore (cross-process block guard) lifecycle. |
| `simulation/flight_process.c` | Opens `/aisafe_env` (`open_env_sem`), reads `shm->environment` under it each step and applies `apply_wind_drift_values`, falling back to the per-segment wind when calm; closes it in `flight_done`. |
| `simulation/flight_parser.c` | Parses `wind_dir_deg`/`wind_speed_mps` into `segment_t` (default `0`) — feeds the fallback path. |
| `simulation/Makefile` | `SRCS += weather_service.c` (plus the pre-existing `environment.c`). |
| `simulation/flight_plans.json` | (demonstration) optional segment wind for the fallback path. |

> The Safety pipeline (`safety_monitor.c`, `safety_thread.c`), the ACA filter
> (`aca_filter.c`) and the report (`report.c`) are **unchanged**: the drifted position
> flows through them as any other position. `main.c` gains only the environment thread
> and its per-step tick.

### 5.2 Key Implementation Details (planned)

**Backward compatibility** — `apply_wind_drift` returns immediately when
`wind_speed <= 0`, so flights without wind data (or with calm conditions) keep
their exact previous trajectory; existing tests stay green.

**Parser default** — when `wind_dir_deg`/`wind_speed_mps` are absent the parser
sets both fields to `0`, matching the no-wind behaviour. This mirrors the existing
defaulting already done for the segment `mode` field.

**Magnitude** — wind of 15–20 m/s against a cruise speed of ~128 m/s (≈250 kt)
produces a drift that accumulates over the steps; the per-segment wind values in
`flight_plans.json` are tuned so the deviation is visible without being absurd.

---

## 6. Integration / Demonstration

### Build

```bash
cd aisafe.base/simulation
make clean && make
```

Expected to compile with zero warnings under `-Wall -Wextra -std=c99`.

### Run — normal mode (mild wind)

```bash
./flight_simulator
```

The flights complete; the live feed shows positions that are slightly displaced
from the straight planned route. `simulation_report.txt` reports a normal,
violation-free run.

### Run — wind forces a violation (the US110 ⇄ US107 demonstration)

Tune the segment wind in `flight_plans.json` so two flights are pushed towards each
other, then run:

```bash
./flight_simulator
```

Expected, **interleaved in real time** (this is the evidence US110 is evaluated on,
via US107):

```
[CYLINDER ALERT] Intersection risk detected at ...
[REPORT US107] Logged violation between UX1144 and FR9441 at ...
```

The conflict — caused purely by the wind drift — is detected by the
`safety_thread` (US102), logged in real time by the `report_thread` (US107) in
`simulation_violation_log.txt`, and counted in the final `simulation_report.txt`
(US109).

### Integration with the other user stories

| US | How US110 integrates |
|----|----------------------|
| US100 | drift computed inside each flight child process |
| US101 | drifted positions pass through the ACA filter / history unchanged |
| US102 | drift can create Safety Cylinder intersections → detection |
| US103/US108 | drift integrated per timestep within the lock-step |
| US105 | the child writes the drifted position to shared memory |
| US106 | the `safety_thread` evaluates drifted positions — no change |
| US107 | wind-induced violations are signalled/logged in real time — **evaluation point** |
| US109 | the final report includes the wind-induced violations |

---

## 7. Observations

- **Dedicated environment thread (AC110.1–3)** — US110 adds a 4th parent thread that
  loads wind from the weather service and publishes it to shared memory each step. Its
  worth is also shown by the existing real-time detection chain (US102/US107) reacting
  to weather-induced deviations.
- **Weather-service source** — `weather_service_fetch()` is the (simulated) weather
  service: wind is read from `AISAFE_WIND="speed,dir"`, calm if unset. When calm, the
  child falls back to the per-segment wind from Domain Model V10
  (`FlightSegment.windDirection`/`windSpeed`); the broader `WeatherData` aggregate
  (US082) is the eventual real source.
- **Cross-process synchronisation** — the env block in shm is guarded by the named
  semaphore `/aisafe_env` (value 1) used as a mutex — the professor's `ex2-6.c` pattern
  and the same primitive the project already uses for `/aisafe_pos_*` / `/aisafe_ctrl_*`
  (a named semaphore works across the parent thread and the forked children, unlike a
  plain pthread mutex). Coordinator↔env per-step signalling uses a parent-local
  mutex+cond (intra-process).
- **Modularity** — drift maths in `environment`, weather source in `weather_service`;
  `flight_process.c` gains only a small read-and-drift block.
- **Termination guaranteed** — applying the drift only to `lat`/`lon` (never to the
  progress variables) is what keeps every segment finite; this is the single most
  important correctness invariant of the feature.
- **Runtime requires a native POSIX environment (Linux)** — like the rest of the
  simulation, the named-semaphore IPC needs a real Linux (lab) to run; Cygwin
  without `cygserver` cannot share the semaphores across the forked flights.
