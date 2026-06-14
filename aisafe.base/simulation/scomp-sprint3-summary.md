# SCOMP — Sprint 3 Summary (US105–US110)

A consolidated, technical overview of the SCOMP component of AISAFE: the C flight
simulator under `aisafe.base/simulation/`. The six user stories US105–US110 form a single
coherent arc, and this document presents the whole concurrency design at a glance. For the
full detail of any story, see its `user-stories/usNNN/readme.md`.

## Overview

The simulator is a **hybrid** architecture: **multiple child processes** (one per flight,
created with `fork`) coordinated by a **multi-threaded parent**. Processes communicate
through a single **POSIX shared-memory** segment (`sim_shm_t`) and synchronise with **named
POSIX semaphores** (inter-process) plus **pthread mutexes and condition variables**
(intra-parent). All output from threads uses `snprintf` + `write()` (printf is not
thread-safe). Signals (`SIGUSR1` + `SA_RESTART`) stop violating flights at a clean step
boundary. The build targets a **native POSIX/Linux** environment (named semaphores are not
shareable across forks under Cygwin without `cygserver`).

The term *hybrid* refers to **processes + threads**, not to "pipes + shared memory":
shared memory completely replaces the pipe-based IPC of US100–US103.

## Global architecture

```
                    PARENT PROCESS (multi-threaded)
   ┌──────────────────────────────────────────────────────────────┐
   │  coordinator_thread   safety_thread   report_thread   environment_thread
   │        │                   │               │                 │
   │        │  safety_channel_t │   g_notif_mutex+g_report_cond    │
   │        │  (mutex+cond,     │   (safety → report, US107)       │
   │        │   ping-pong,      │               ▲                  │
   │        │   US106) ◄────────┘               │                  │
   │        │                          g_done_cond (coord→report,  │
   │        │                          end-of-sim handoff, US105)  │
   │        │                                                      │
   │        │            env tick mutex+cond (coord→env, US110) ───┘
   └────────┼──────────────────────────────────────────────────────┘
            │  shm_open/ftruncate/mmap
            ▼
   ┌──────────────────────────  sim_shm_t  ──────────────────────────┐
   │ positions[]  ctrl[]  active[]  total_violations  sim_aborted     │
   │ violation_events[]  violation_event_count  dropped_*   (US107)   │
   │ environment  env_step                                  (US110)   │
   └──────────────────────────────────────────────────────────────────┘
            ▲ named semaphores (inter-process)
   ┌────────┴───────────────────────────────────────────────────────┐
   │  flight child 0 ... flight child N-1   (one fork() per flight)   │
   │  per flight: /aisafe_pos_N (0) , /aisafe_ctrl_N (0)              │
   │  shared:     /aisafe_env (1, cross-process mutex, US110)         │
   └──────────────────────────────────────────────────────────────────┘
```

**Two synchronisation planes:**

| Plane | Primitives | Purpose |
|-------|-----------|---------|
| Inter-process | Named semaphores `/aisafe_pos_N` (0), `/aisafe_ctrl_N` (0), `/aisafe_env` (1) | Step lockstep between children and parent; cross-process mutex for the wind block |
| Intra-parent | `safety_channel_t` (mutex+cond); `g_notification_mutex`+`g_report_cond`; `g_done_cond`; env tick mutex+cond | Coordinator↔safety ping-pong; safety→report event stream; end-of-sim handoff; coordinator→env tick |

**Thread lifecycle:** created in the order `environment`, `safety`, `coordinator`,
`report`; **joined** in the order `coordinator` (first — it signals the others to finish),
then `environment`, `safety`, `report`; finally the children are reaped with `waitpid`.

---

## US105 — Initialize Hybrid Simulation Environment with Shared Memory

**Goal:** stand up the hybrid environment — multi-threaded parent + per-flight child
processes communicating through shared memory.

> *As a Flight Control Operator, I want to start the simulation with a multi-threaded
> parent process and multiple child flight processes communicating through a shared memory
> area, so that the system efficiently coordinates simulation data across processes.*

**SCOMP mechanisms:** `shm_open(O_CREAT|O_EXCL)` + `ftruncate` + `mmap` for `sim_shm_t`
(children attach with `shm_open(O_RDWR)` + `mmap`); named semaphores initialised to **0**
(event-signalling pattern, not mutual exclusion); `pthread_create`/`pthread_join`;
`pthread_mutex_init`/`pthread_cond_init` (stack-allocated, so **not**
`PTHREAD_MUTEX_INITIALIZER`); `SIGUSR1` + `SA_RESTART`. `shm_unlink`/`sem_unlink` are
called unconditionally before creation for crash recovery (mandatory on macOS where
`ftruncate` fails on a pre-existing object).

**Key protocol — per-step lockstep + end-of-sim handoff:**
```
child: shm->positions[i]=pos; sem_post(pos_sem) ──► coordinator: sem_wait(pos_sem);
       read pos; US101 ACA; US102 safety; ctrl[i]=GO/STOP; sem_post(ctrl_sem)
child: sem_wait(ctrl_sem); if ctrl==0 → flight_done(), exit(1)
end:   coordinator sets g_sim_done=1, pthread_cond_signal(g_done_cond)
       report_thread: while(!g_sim_done) pthread_cond_wait(g_done_cond) → final report
```
The `while`-predicate guard is mandatory (POSIX permits spurious wakeups).

| AC | Mechanism |
|----|-----------|
| AC1 parent spawns dedicated threads | `coordinator_thread` + `report_thread` (later +safety +environment) |
| AC2 each flight is an independent process | `fork()` per flight |
| AC3 shared memory allocated & initialized | `shm_create()` (`ex1-7.c` pattern) |
| AC4 flights synchronised with semaphores | `/aisafe_pos_N`, `/aisafe_ctrl_N` |
| AC5 threads, mutexes, cond vars, signals in C | `pthread_*`, `g_done_cond`, `SIGUSR1` |

---

## US106 — Implement Function-Specific Threads in the Parent Process

**Goal:** separate concerns inside the parent — each functionality in its own thread.

> *As a PO, I want the simulation controller parent process to have at least two dedicated
> threads (one for safety violation detection and one for report generation), so that each
> functionality operates concurrently and independently.*

**SCOMP mechanisms:** extracts the US102 safety logic out of the coordinator into a new
`safety_thread`, giving **three** function-specific threads (coordinator = telemetry +
ACA/history + GO/STOP; safety = collision prediction + Safety Cylinder; report = final
report). A **mutex + condition variable** channel (`safety_channel_t`) carries the per-step
**ping-pong** handoff; the safety thread copies the snapshot under the lock then **releases
the lock during the heavy computation**.

**Key protocol — coordinator↔safety ping-pong:**
```
coordinator: lock; copy snapshot; verdict_ready=0; step_ready=1; broadcast;
             while(!verdict_ready) cond_wait        ──► safety: while(!step_ready &&
read verdict (abort/violations); unlock                       !sim_finished) cond_wait
                                                           step_ready=0; copy; unlock
                                              ◄──        run prediction + cylinder check
                                                           lock; verdict_ready=1; broadcast
```
On termination the coordinator sets `sim_finished` and broadcasts so the join does not hang.

| AC | Mechanism |
|----|-----------|
| AC106.1 safety detection thread | `safety_thread` runs `predict_future_collisions` + `monitor_safety_violations` |
| AC106.2 report generation thread | `report_thread` (real-time response delivered in US107) |
| AC106.3 additional appropriate thread | `coordinator_thread` (positions, ACA/history, GO/STOP) |
| AC106.4 mutexes + cond vars | `safety_channel_t`; every wait uses a `while`-predicate |

---

## US107 — Notify Report Thread via Condition Variables upon Violation

**Goal:** log each safety violation in real time, not just at the end.

> *As a PO, I want the safety violation detection thread to notify the report generation
> thread through condition variables when a safety violation occurs, so that the report is
> updated in real time with accurate information.*

**SCOMP mechanisms:** a **producer/consumer** channel over a **separate** mutex/cond pair
(`g_notification_mutex` + `g_report_cond`), distinct from the US106 ping-pong. The producer
(safety, in `safety_monitor.c`) appends a `violation_event_t` to a **bounded** buffer in
shared memory and `pthread_cond_signal`s **under the lock**; the consumer (`report_thread`)
waits on a `while`-predicate and **unlocks during the file/stdout I/O**. Buffer overflow is
counted (`dropped_violation_events`), never silently overwritten.

**Key protocol — violation event stream:**
```
safety (producer):  lock(g_notif_mutex); record event in shm->violation_events[] (or
                    dropped++); cond_signal(g_report_cond); unlock
report (consumer):  lock; while(no new events && !sim_done) cond_wait;
                    copy event; unlock; append_violation_event_to_log + [REPORT US107] line; lock
```

| AC | Mechanism |
|----|-----------|
| AC107.1 detection thread monitors shm | `safety_thread` → `monitor_safety_violations` |
| AC107.2 signal via condition variable | `pthread_cond_signal(g_report_cond)` under the mutex |
| AC107.3 consumer wakes & logs immediately | `report_thread` `cond_wait` then `append_violation_event_to_log` |
| AC107.4 proper mutex locking | buffer, counters and signal all under `g_notification_mutex` |

---

## US108 — Enforce Step-by-Step Simulation Synchronization

**Goal:** make the semaphore-based lockstep the explicit, formal step contract.

> *As a PO, I want the simulation engine to synchronize the step-by-step progression using
> semaphores, so that all flight processes and parent threads advance in lockstep.*

**SCOMP mechanisms:** **no new files** — formalises the US105 infrastructure. Two named
semaphores per flight, both init **0**: `/aisafe_pos_N` (child→coordinator) and
`/aisafe_ctrl_N` (coordinator→child). The `sem_post → sem_wait` pair establishes a POSIX
**happens-before** relationship, so the coordinator always reads the value the child wrote
— no extra mutex is needed for the position slot. Named (not anonymous `sem_init`)
semaphores are required because parent and children are **separate address spaces**.

**Why semaphore over mutex:** a semaphore enforces not just mutual exclusion but the
**ordering** (child writes first, parent reads second) — exactly the property needed for
correct step progression.

| AC | Mechanism |
|----|-----------|
| AC1 semaphores control each time step | `pos_sem`/`ctrl_sem` post/wait per flight per step |

`flight_done()` posts `pos_sem` one last time with `active[i]=0` so the coordinator detects
completion; on abort the coordinator sends STOP + `sem_post(ctrl_sem)` to all and breaks —
no deadlock, leftover posts are released by `cleanup_sems()`.

---

## US109 — Generate and Store Final Simulation Report

**Goal:** persist a complete report after the simulation concludes.

> *As a Flight Control Operator, I want a comprehensive report detailing simulation
> outcomes (flight statuses, violation events with timestamps/positions/velocity vectors,
> validation result), so that I can assess safety and performance post-simulation.*

**SCOMP mechanisms:** owned by the `report_thread`, which blocks on `g_report_cond`/`g_done_cond`
instead of polling. When the coordinator sets `g_sim_done`, the thread drains the live
violation queue, snapshots the final shm counters, and writes `simulation_report.txt` with
standard `fopen`/`fprintf`/`fclose` (safe because it runs in a normal thread, not a signal
handler). Consumes data from US101 (ACA history), US102 (violations), US105–US107.

**Validation rule:** `PASS` if completed with **no** violations; `FAIL` if aborted **or**
any violation detected. The file also keeps an operational `Simulation End Status` line.

| AC | Mechanism |
|----|-----------|
| AC1 aggregate once sim concludes | `report_thread` after `g_sim_done` |
| AC2 total flights + per-flight statuses | `FLIGHT EXECUTION STATUSES` section |
| AC3 detailed events (timestamp/position/velocity) | `SAFETY VIOLATION EVENTS` section |
| AC4 PASS/FAIL clearly indicated | explicit `Final Validation Result` line |
| AC5 report saved to file | `simulation_report.txt` (overwritten each run) |

---

## US110 — Integrate Environmental Influences into Simulation *(5+ student groups)*

**Goal:** make wind deviate each aircraft's actual path; evaluated **as part of US107**
(wind can push two flights into the Safety Cylinder, detected/logged in real time).

> *As a PO, I want the simulation to incorporate environmental factors such as wind, so
> that the flight paths become more realistic and adapt to dynamic conditions.*

**SCOMP mechanisms:** a **4th parent thread** `environment_thread` loads wind from a
weather service (`weather_service_fetch()`, reads `AISAFE_WIND="speed,dir"`, calm if unset)
and writes `shm->environment` each step. The env block is guarded by the named semaphore
`/aisafe_env` (value **1**) used as a **cross-process mutex** (works across the parent
thread and the forked children, unlike a plain pthread mutex). The coordinator ticks the
env thread once per step via a parent-local mutex+cond. Each child reads the wind under
`/aisafe_env` and applies a lateral drift to **lat/lon only**.

**Termination invariant (critical):** the drift touches `lat`/`lon` only — never
`dist_covered_m` or `alt` — so every segment-end condition stays reachable and the
simulation always terminates. `wind_speed = 0` ⇒ no drift (backward compatible); calm
service ⇒ fallback to per-segment plan wind.

| AC | Mechanism |
|----|-----------|
| AC110.1 spawn additional "environment" thread | `environment_thread` (4th parent thread) |
| AC110.2 load wind from a weather service | `weather_service_fetch()` |
| AC110.3 write env data to shm each step | `shm->environment` under `/aisafe_env`, ticked per step |

---

## SCOMP concepts cross-reference

| Concept | Primitive(s) | Where used |
|---------|--------------|------------|
| Shared memory | `shm_open`/`ftruncate`/`mmap`, `sim_shm_t` | US105, US108, US109, US110 |
| Named semaphores | `/aisafe_pos_N`, `/aisafe_ctrl_N` (0); `/aisafe_env` (1) | US105, US108, US110 |
| Threads | `pthread_create`/`pthread_join` (4 parent threads) | US105, US106, US109, US110 |
| Mutex + condition variable | `safety_channel_t`; `g_notification_mutex`+`g_report_cond`; `g_done_cond`; env tick | US105, US106, US107, US109, US110 |
| Producer / consumer | bounded `violation_events[]` buffer + overflow accounting | US107 |
| Signals | `SIGUSR1` + `SA_RESTART` (clean step-boundary stop) | US105 (US102) |
| Thread-safe output | `snprintf` + `write()` | US105, US106, US107 |

---

## Build & run

```bash
cd aisafe.base/simulation
make clean && make            # zero warnings under -Wall -Wextra -std=c99

./flight_simulator            # normal mode
./flight_simulator --collision   # forces a safety violation / abort
```

Evidence of the function-specific threads, while the simulator runs:

```bash
ps -T -C flight_simulator     # shows 4 LWPs: coordinator, safety, report, environment
```

Outputs: `simulation_report.txt` (final report, US109) and
`simulation_violation_log.txt` (live violation log, US107). The simulator requires a
**native POSIX/Linux** toolchain (`gcc`, `make`, POSIX shm, named semaphores, pthreads).

## Alignment with the professor's examples

| Pattern | Example | Used in |
|---------|---------|---------|
| `shm_open`+`ftruncate`+`mmap`; child `munmap` before exit | `ex1-7.c` | US105, US108, US110 |
| Named semaphore init 0 (event signalling) | `ex2-5.c` | US105, US108 |
| Named semaphore init 1 (mutual exclusion) | `ex2-6.c` | US110 (`/aisafe_env`) |
| `pthread_create`/`pthread_join`; `write()` not `printf` in threads | `ex1-9.c` | US105, US106, US107, US110 |
| Distinct, function-specific thread routines | `psum` | US106 |
| Mutex protecting shared state | `increment_safe` | US106, US107 |
| Mutex + cond var with `while`-predicate; producer signals under lock | T6/T7/T8 slides | US105, US106, US107, US109 |
| Bounded producer/consumer buffer | `producer_consumer.c` | US107 |
| `SIGUSR1` + `SA_RESTART` (async-signal-safe handler) | `ex1-3.c`, `ex1-4.c` | US105 (US102) |
