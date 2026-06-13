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

The implementation is written in C and lives under `aisafe.base/simulation/`. The
environmental influence is computed **inside each flight child process**
(`flight_process.c`, the process model from US100/US105), one simulation step at a
time, consistently with the step-by-step synchronisation of US108. The wind logic
is isolated in a dedicated `environment` module so that US110 is self-contained
and the shared `flight_process.c` gains only a single call.

---

## 2. Requirements

**US110 — Integrate environmental influences into simulation.**

> As a Flight Control Operator, I want climatic conditions (wind) to influence the
> path taken by each aircraft during the simulation, so that the simulated flights
> reflect realistic environmental effects and the resulting safety risks are
> detected.

### Acceptance Criteria — Proposed (pending PO confirmation)

> ⚠️ **Status: PROPOSED.** The assignment defines no formal acceptance criteria for
> US110. The criteria below are **derived** from the Sprint 3 statement (*"Influence of
> climatic conditions on the path taken by the aircraft — evaluated as an integral part
> of US107"*) and from Domain Model V10. **They are not yet ratified and must be
> confirmed with the PO/assignment before being treated as binding.**

| ID | Criterion (proposed) | Where it will be met | Status |
|----|---------------------|----------------------|--------|
| AC110.1 | The simulation reads the **environmental data (wind direction and speed)** defined for each flight segment. | `flight_parser.c` parses `wind_dir_deg`→`wind_direction` and `wind_speed_mps`→`wind_speed` into `segment_t` (default 0 when absent) | Done |
| AC110.2 | The wind **alters the path actually taken** by each aircraft (its position deviates from the planned route). | `environment.c` (`apply_wind_drift`) applies a per-step lateral drift, called from `flight_process.c` | Done |
| AC110.3 | The environmental influence is applied **per simulation step**, inside each flight process, consistently with the step synchronisation (US108). | `apply_wind_drift(&lat,&lon,segment,STEP_SECONDS)` inside the per-step loop of `flight_process.c` | Done |
| AC110.4 | Wind-induced deviations are subject to the same **Safety Cylinder verification**, so resulting violations are detected, logged and signalled in real time (US102/US107). | no change required — the drifted position flows child → shared memory → `safety_thread` → US107 | Done |

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

| Approach | Description | Decision |
|----------|-------------|----------|
| Inline the wind maths in `flight_process.c` | Add the vector maths directly in the movement loop | Rejected — mixes US110 into a shared US100/US105 file; less modular |
| **Isolate in an `environment` module** | New `environment.{c,h}`; `flight_process.c` gains one `#include` + one call | **Chosen** — keeps US110 self-contained (Code Quality / modularity) and the shared file barely changes |

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
and **before** the position is written to shared memory:

```c
lat += (dlat / dist_total_m) * horiz_step_m;   /* planned route (existing) */
lon += (dlon / dist_total_m) * horiz_step_m;
dist_covered_m += horiz_step_m;
alt += vz * STEP_SECONDS;
apply_wind_drift(&lat, &lon, segment, STEP_SECONDS);   /* << US110 */
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

US110 is domain physics inside the existing process model rather than a new SCOMP
primitive; it leverages, without changing, the IPC pipeline from earlier US:

| Mechanism | Origin | Use in US110 |
|-----------|--------|--------------|
| One process per flight (`fork`) | US100/US105 | the drift is computed in each flight child |
| Shared memory + `sem_post(pos_sem)` | US105 (`ex1-7.c`, `ex2-5.c`) | the **drifted** position is published to the parent |
| Per-step lock-step | US103/US108 | the drift is integrated once per `STEP_SECONDS` |
| `FlightSegment.windDirection`/`windSpeed` | Domain Model V10 | the wind source consumed by `apply_wind_drift` |

---

## 5. Implementation

### 5.1 Files to Create/Modify

| File | Change summary |
|------|---------------|
| `simulation/environment.h` | **New** — `apply_wind_drift` prototype (the US110 module interface) |
| `simulation/environment.c` | **New** — the wind-drift maths (self-contained, with its own `M_PI`/`DEG_TO_RAD`); depends only on `types.h` (`segment_t`) and `<math.h>` |
| `simulation/flight_parser.c` | Parse `wind_dir_deg`→`wind_direction` and `wind_speed_mps`→`wind_speed` in the segment loop (default `0`) |
| `simulation/flight_process.c` | `#include "environment.h"` + one `apply_wind_drift(...)` call after the route update |
| `simulation/Makefile` | `SRCS += environment.c` |
| `simulation/flight_plans.json` | (demonstration) tune the segment wind to force a visible violation |

> The Safety pipeline (`safety_monitor.c`, `safety_thread.c`), the ACA filter
> (`aca_filter.c`), the parent threads (`main.c`) and the report (`report.c`) are
> **unchanged**: the drifted position flows through them as any other position.

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

- **Evaluated through US107** — US110 has little *new* SCOMP machinery; it is
  domain physics in the existing flight process. Its worth is shown by the existing
  real-time detection chain (US102/US107) reacting to weather-induced deviations.
- **No new domain concept** — the wind is already in Domain Model V10
  (`FlightSegment.windDirection`/`windSpeed`); US110 only *consumes* it. The broader
  `WeatherData` aggregate (US082) is the source the flight plan carries.
- **Modularity** — the wind maths lives in its own `environment` module; the shared
  `flight_process.c` changes by one include and one call, keeping US110's footprint
  on other stories minimal.
- **Termination guaranteed** — applying the drift only to `lat`/`lon` (never to the
  progress variables) is what keeps every segment finite; this is the single most
  important correctness invariant of the feature.
- **Runtime requires a native POSIX environment (Linux)** — like the rest of the
  simulation, the named-semaphore IPC needs a real Linux (lab) to run; Cygwin
  without `cygserver` cannot share the semaphores across the forked flights.
