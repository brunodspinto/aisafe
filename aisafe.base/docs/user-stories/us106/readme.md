# US106 — Implement Function-Specific Threads in the Parent Process

## 1. Context

US106 builds directly on the hybrid architecture delivered in US105 (POSIX shared
memory + named semaphores, multi-threaded parent). US105 already introduced two
parent threads — a `coordinator_thread` and a `report_thread` — but the
coordinator was **monolithic**: a single thread that collected aircraft
positions, applied the ACA filter and history (US101), ran the collision
prediction and the Safety Cylinder verification (US102), and sent the GO/STOP
control to each flight.

US106 addresses the *separation of concerns* inside the parent process:
each distinct functionality must run in its **own dedicated, function-specific
thread**. Concretely, the **safety-monitoring functionality (US102) is extracted
into a new `safety_thread`**, leaving the coordinator responsible only for
telemetry/coordination and the report thread responsible only for the final
report. The three threads cooperate, one simulation step at a time, through a
condition-variable "ping-pong" handoff.

The implementation is written in C and lives under `aisafe.base/simulation/`.
It is deliberately aligned with the professor's canonical examples
(Luís Nogueira, ISEP SCOMP 2526), specifically the POSIX threads examples
(`pthread_create`/`pthread_join`, the `find_local_max` worker-thread example),
the multiple distinct thread functions of `psum` (`sum_global`/`sum_local`/…),
the mutex usage of `increment_safe`, and the mutex + condition-variable
discipline from the T7/T8 slides.

---

## 2. Requirements

**US106 — Implement function-specific threads in the parent process**

> As a PO, I want the simulation controller parent process to have at least two
> dedicated threads (one for safety violation detection and one for report
> generation), so that each functionality operates concurrently and
> independently.

### Acceptance Criteria

The criteria below are quoted from the assignment; the *Where it is met* column
maps each one to the implementation.

| ID | Criterion (as stated in the assignment) | Where it is met | Status |
|----|------------------------------------------|-----------------|--------|
| AC106.1 | The parent process creates a **safety violation detection thread** responsible for scanning the shared memory for aircraft flight conflicts. | `safety_thread` (`safety_thread.c`) runs `predict_future_collisions` + `monitor_safety_violations` on each step's position snapshot and records conflicts | Done |
| AC106.2 | A **report generation thread** is created to compile simulation results and respond to safety violation events. | `report_thread` (`main.c`) calls `generate_final_report`; the real-time *response* to violation events is delivered through the condition-variable notification of **US107** | Done |
| AC106.3 | Any **additional thread** that you deem appropriate for any of the required functionalities. | `coordinator_thread` (`main.c`) — position collection, ACA filter/history (US101) and GO/STOP control | Done |
| AC106.4 | Threads are managed using **mutexes and condition variables** for internal synchronisation. | `safety_channel_t` (`mutex`+`cond`, coordinator↔safety handoff) and `g_notification_mutex`+`g_report_cond` (safety→report); every wait uses a `while`-predicate, so there is no busy-waiting | Done |

---

## 3. Analysis

### 3.1 Starting point (US105) and the problem

| Concern | Where it ran in US105 | Problem |
|---------|----------------------|---------|
| Telemetry collection + ACA/history (US101) | `coordinator_thread` | — |
| Collision prediction + Safety Cylinder (US102) | `coordinator_thread` | Coupled into the coordinator; a single thread does everything |
| GO/STOP control | `coordinator_thread` | — |
| Final report (US109) | `report_thread` | Already separated |

The coordinator mixing US101 and US102 in one thread violates the user story:
*"proper separation of functionalities by threads in the parent process."*

### 3.2 Decomposition decision

| Approach | Description | Decision |
|----------|-------------|----------|
| Keep everything in the coordinator | One thread does telemetry + safety + control | Rejected — does not satisfy AC106.1 (no dedicated safety-detection thread) |
| Extract **safety** into its own thread | `coordinator_thread` keeps telemetry + control; a new `safety_thread` owns US102; `report_thread` unchanged | **Chosen** — gives three function-specific threads with a clean responsibility per thread |

| Thread | Responsibility | US |
|--------|----------------|-----|
| `coordinator_thread` *(stays in `main.c`)* | Collect positions (`pos_sems`), ACA filter + history, live feed, GO/STOP (`ctrl_sems`) | US101 |
| `safety_thread` *(new module — this US106)* | Future-collision prediction + Safety Cylinder verification, violation count, abort decision | US102 |
| `report_thread` *(stays in `main.c`)* | Wait on condition variable, generate final report | US109 |

> Scope note: only the **safety** functionality is newly extracted in US106.
> `coordinator_thread` and `report_thread` were delivered in US105 and remain in
> `main.c` unchanged (apart from the coordinator's handoff to the new thread).

### 3.3 Why a condition-variable handoff is required

Within one simulation step the work is **sequentially dependent**: positions must
be collected *before* safety can be checked, and the GO/STOP decision depends on
the safety verdict. Two cooperating threads therefore need an ordered handoff.
A mutex + condition variable provides this without busy-waiting:

* the coordinator is the *producer* of the per-step snapshot;
* the safety thread is the *producer* of the per-step verdict;
* each side blocks on a predicate until the other has produced its part.

This is the canonical producer/consumer-with-a-predicate pattern, the same
discipline already used in US105 for the end-of-simulation report handoff.

---

## 4. Design

### 4.1 Synchronisation channel (`safety_thread.h`)

```c
typedef struct {
    /* snapshot of the step: written by coordinator, read by safety */
    aircraft_position_t prev_positions[MAX_FLIGHTS];
    aircraft_position_t current_positions[MAX_FLIGHTS];
    int                 has_position[MAX_FLIGHTS];
    int                 local_active[MAX_FLIGHTS];
    /* verdict: written by safety, read by coordinator */
    int                 abort_sim;
    int                 total_violations;
    /* ping-pong state machine */
    int                 step_ready;     /* coordinator -> safety */
    int                 verdict_ready;  /* safety -> coordinator */
    int                 sim_finished;   /* coordinator -> safety: terminate */
    pthread_mutex_t     mutex;
    pthread_cond_t      cond;
} safety_channel_t;
```

The channel, the `safety_ctx_t` thread context and the `safety_thread`
prototype all live in `safety_thread.h` — the single US106 header — so no
shared type is duplicated across files.

### 4.2 Per-step ping-pong protocol

```
coordinator_thread (in main.c)              safety_thread (safety_thread.c)
──────────────────────────────────────────────────────────────────────────
collect all positions for step T
lock(mutex)
  copy snapshot into channel
  verdict_ready = 0; step_ready = 1
  cond_broadcast                ───────────►  lock(mutex)
  while(!verdict_ready)                         while(!step_ready && !sim_finished)
      cond_wait                                     cond_wait
                                                  step_ready = 0
                                                  copy snapshot to locals
                                                unlock(mutex)
                                                predict_future_collisions(...)
                                                monitor_safety_violations(...)
                                                lock(mutex)
                                                  abort_sim/total_violations = ...
                                                  verdict_ready = 1
  (wakes)                       ◄───────────      cond_broadcast
  read abort_sim/total_violations               unlock(mutex)
unlock(mutex)
send GO (or STOP on abort) via ctrl_sems
```

Both waits use a **`while`-loop predicate**, mandatory under POSIX because
`pthread_cond_wait` may return spuriously and to avoid lost wake-ups.

### 4.3 Working on a copy of the snapshot

The safety thread copies the snapshot under the mutex and then **releases the
lock during the (heavier) computation** — it does not hold the lock while running
prediction/verification (the lesson from `increment_safe`). This is safe because:

* `monitor_safety_violations()` only **reads** `local_active`/`pids` (it never
  modifies them), so no result needs to propagate back through the snapshot; and
* while the safety thread computes, the coordinator is blocked in
  `while(!verdict_ready) cond_wait`, so it cannot touch the snapshot.

### 4.4 Clean termination (no hung join)

When the simulation ends (all flights completed, or an abort), the coordinator
sets `sim_finished = 1` and broadcasts. The safety thread, blocked in
`while(!step_ready && !sim_finished)`, wakes, sees `sim_finished`, and breaks out
of its loop, so `pthread_join(safety_tid)` returns instead of hanging.

### 4.5 Alignment with the professor's examples

| Pattern | Professor example | Use in US106 |
|---------|-------------------|--------------|
| `pthread_create` / `pthread_join` | `ex1-9.c`, `find_local_max` | three threads created/joined in `main.c` |
| Distinct, function-specific thread routines | `psum` (`sum_global`/`sum_local`/…) | `coordinator_thread` vs `safety_thread` vs `report_thread` |
| `mutex` protecting shared state | `increment_safe` | `safety_channel_t.mutex` |
| `mutex` + `cond var` with `while`-predicate | T7/T8 slides | per-step handoff + report handoff |
| `snprintf` + `write()` in threads (*"printf is not thread-safe"*) | `find_local_max` | coordinator live feed; report header |

---

## 5. Implementation

### 5.1 Files Created/Modified

| File | Change summary |
|------|---------------|
| `simulation/safety_thread.h` | **New** — defines `safety_channel_t`, `safety_ctx_t` and the `safety_thread` prototype (the single US106 header, self-contained) |
| `simulation/safety_thread.c` | **New** — implements `safety_thread`: waits for each step's snapshot, runs `predict_future_collisions` + `monitor_safety_violations` (US102), returns the verdict via the channel |
| `simulation/main.c` | Coordinator's inline US102 block replaced by the ping-pong handoff to `safety_thread`; `safety_channel_t` initialised; third `pthread_create`/`pthread_join` added; `sim_finished` signalled at the end |
| `simulation/Makefile` | Added `safety_thread.c` to `SRCS` |

> The US102 logic itself (`safety_monitor.c`) and the US101/US109 code are
> **unchanged**: US106 only *relocates which thread* runs the safety functions.

### 5.2 Key Implementation Details

**`safety_thread` loop** — blocks on `step_ready || sim_finished`; on a step it
clears `step_ready`, copies the snapshot, releases the mutex, runs prediction +
Safety Cylinder verification, then publishes `abort_sim`/`total_violations` with
`verdict_ready = 1` and a broadcast.

**Running total of violations** — `monitor_safety_violations` increments a thread
-local accumulator (passed by pointer); the current total is copied into the
channel on every verdict so the coordinator can store it in shared memory for the
report at the end.

**`pthread_mutex_init` vs `PTHREAD_MUTEX_INITIALIZER`** — the channel's mutex and
condition variable are stack-allocated in `main()`, so they are initialised with
`pthread_mutex_init`/`pthread_cond_init` (the static `_INITIALIZER` forms are only
guaranteed for static storage) and destroyed at cleanup.

**No deadlock on abort** — on a critical violation the safety thread returns the
abort verdict; the coordinator reads it, sends STOP to all active flights and
breaks the loop; it then sets `sim_finished`, which releases the safety thread
from its wait. Each child's `sem_wait(ctrl_sem)` returns, sees `ctrl == 0`, and
exits.

**Deferred scope** — the real-time notification of the report thread upon a
violation is **US107**; the formal step barrier guarantee is **US108**. US106
delivers the thread separation and the per-step synchronisation foundation only;
the final report is still produced at the end of the simulation (as in US105).

---

## 6. Integration / Demonstration

### Build

```bash
cd aisafe.base/simulation
make clean && make
```

Compiles with zero warnings under `-Wall -Wextra -std=c99`.

### Run — normal mode

```bash
./flight_simulator
```

Expected: the live feed advances step by step; each flight reports
`flight completed.`; then `[SYSTEM] Simulation concluded...` and each flight
`ended with code 0`. The program returns to the prompt — evidence that the
coordinator↔safety ping-pong does not deadlock.

### Run — collision mode

```bash
./flight_simulator --collision
```

Expected: the **safety thread** prints `[CYLINDER ALERT] ...`, the involved
flights receive `SIGUSR1` (`[FLIGHT] SIGUSR1 received...`) and `end with code 1`.

### Demonstrating the three parent threads

While the simulator runs, in another terminal:

```bash
ps -T -C flight_simulator
```

The parent process shows **three threads** (LWPs): coordinator, safety and
report — direct evidence of the function-specific separation required by US106.

> Note: US110 later adds a 4th parent thread (`environment_thread`), so on the
> current binary `ps -T` shows **four** threads — coordinator, safety, report and
> environment. The three above are the ones US106 introduces/separates.

> Note: execution requires a native POSIX environment (Linux). On Cygwin without
> `cygserver`, named semaphores fail to be shared across the forked children
> (`sem_open` → "Connection timed out"); this is an environment limitation, not a
> defect of the thread design.

---

## 7. Observations

**Ping-pong serialises the two threads per step** — because the coordinator
blocks on `verdict_ready` while the safety thread computes, the two never write
to `stdout` at the same time. The existing US102 code (`safety_monitor.c`) uses
`printf` internally; under this handoff that output cannot interleave with the
coordinator's `write()` calls.

**Snapshot copy is the safety boundary** — confirming that
`monitor_safety_violations` only reads `local_active`/`pids` is what makes it
correct for the safety thread to operate on a copy; if it mutated them, those
changes would have to be returned through the channel.

**Separation without behavioural change** — US106 moves *where* code runs, not
*what* it computes. The step-by-step output is identical to US105; only the
internal thread structure of the parent changed, satisfying the
"proper separation of functionalities by threads" criterion.
