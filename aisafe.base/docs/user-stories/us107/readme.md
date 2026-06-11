# US107 — Notify Report Thread via Condition Variables upon Safety Violation Detection

## 1. Context

US107 builds directly on US106, which split the parent process into three
function-specific threads — `coordinator_thread`, `safety_thread` and
`report_thread`. US106 deliberately **deferred** one piece of behaviour: although
the `report_thread` already existed, it only produced the *final* report at the
end of the simulation; it did **not** react to individual safety violations as
they happened.

US107 closes that gap. It introduces a **real-time notification channel** between
the producer of safety violations (the `safety_thread`, via the US102 detection
logic) and the consumer that logs them (the `report_thread`). The moment a Safety
Cylinder violation is detected, the safety thread records the event in shared
memory and **signals the report thread through a condition variable**; the report
thread, blocked on that condition variable, wakes up and logs the event
immediately, so the violation log is kept up to date in real time instead of only
at the end of the run.

The implementation is written in C and lives under `aisafe.base/simulation/`. It
follows the professor's canonical producer/consumer-with-a-condition-variable
discipline (Luís Nogueira, ISEP SCOMP 2526) from the T7/T8 slides: shared state
protected by a mutex, the consumer waiting on a `while`-predicate, and the
producer signalling under the lock.

---

## 2. Requirements

**US107 — Notify report thread via condition variables upon safety violation detection**

> As a PO, I want the simulation system safety violation detection thread to
> notify the report generation thread through condition variables when a safety
> violation occurs, so that the report is updated in real time with accurate
> information.

### Acceptance Criteria

The criteria below are quoted from the assignment; the *Where it is met* column
maps each one to the implementation.

| ID | Criterion (as stated in the assignment) | Where it is met | Status |
|----|------------------------------------------|-----------------|--------|
| AC107.1 | The **safety violation detection thread** monitors the shared memory for flight conflicts. | `safety_thread` (`safety_thread.c`) runs `monitor_safety_violations` (`safety_monitor.c`), which checks the Safety Cylinder (US102) for each pair of active flights | Done |
| AC107.2 | Upon detecting a safety violation, the thread **signals the report generation thread using condition variables**. | `safety_monitor.c` records the event in `shm->violation_events` and calls `pthread_cond_signal(g_report_cond)` while holding `g_notification_mutex` | Done |
| AC107.3 | The report generation thread, **waiting on the condition variable, immediately processes the safety violation event and logs it**. | `report_thread` (`main.c`) blocks in `pthread_cond_wait(g_report_cond, ...)`, then drains and logs each new event via `append_violation_event_to_log` and a `[REPORT US107]` line | Done |
| AC107.4 | **Proper mutex locking** is used to ensure thread-safe notification. | Every write to the event buffer and the signal happen under `g_notification_mutex`; the consumer waits with the same mutex held (released/reacquired by `cond_wait`) | Done |

---

## 3. Analysis

### 3.1 Starting point (US106) and the gap

| Concern | State after US106 | Gap addressed by US107 |
|---------|-------------------|------------------------|
| Safety detection (`safety_thread`) | Detects violations, counts them, decides abort | Did not *notify* anyone in real time |
| Report (`report_thread`) | Produced the final report at end of simulation | Did not *react* to violations as they occurred |

The two threads existed but were not connected for per-violation events: a
detected violation was counted, but its detailed record (timestamp, the two
flights, positions, distances) was not logged until the end. US107 wires them
together so each violation is logged the instant it is detected.

### 3.2 Producer / consumer with a condition variable

The natural model is **producer/consumer**:

* **Producer** — the safety thread, which detects a violation and appends a
  `violation_event_t` to a bounded buffer in shared memory.
* **Consumer** — the report thread, which logs each event.

A condition variable (not a busy-wait, not polling) is the correct primitive: the
consumer must **block while there is nothing to do** and be woken **exactly when**
a new event is available. This is the T7/T8 discipline and is the same pattern
already used inside US105/US106 for the end-of-simulation handoff — here applied
to a stream of per-violation events.

### 3.3 Why a *separate* channel from the US106 ping-pong

US106 introduced one condition-variable channel (`safety_channel_t`) for the
**coordinator ↔ safety** per-step handoff. US107 needs a **different**
channel — **safety → report** — with different semantics (a one-way stream of
events, not a lock-step ping-pong). Mixing the two on a single mutex/cond would
couple unrelated synchronisation and risk spurious wake-ups across concerns, so
US107 uses its own pair: `g_notification_mutex` + `g_report_cond`.

---

## 4. Design

### 4.1 The notification channel

Three pieces of shared state live in shared memory (`sim_shm_t`,
`shared_memory.h`) and are protected by a dedicated mutex/condition pair declared
in `main()`:

```c
/* shared-memory event buffer (sim_shm_t) */
violation_event_t violation_events[MAX_VIOLATION_EVENTS];
int               violation_event_count;     /* events produced so far */
int               dropped_violation_events;   /* events lost on overflow */
int               total_violations;

/* notification primitives (main.c), passed to both threads by pointer */
pthread_mutex_t g_notification_mutex;   /* protects the buffer + counters */
pthread_cond_t  g_report_cond;          /* safety -> report wake-up       */
int             g_sim_done;             /* set when the simulation ends   */
```

`g_notification_mutex` and `g_report_cond` are stack-allocated in `main()`, so
they are initialised with `pthread_mutex_init`/`pthread_cond_init` (the static
`PTHREAD_*_INITIALIZER` forms are only guaranteed for static storage) and
destroyed at cleanup. Pointers to them are placed in both the safety thread
context and the report thread context.

### 4.2 Producer side — `safety_monitor.c`

When a Safety Cylinder intersection is detected:

```c
(*total_violations)++;

pthread_mutex_lock(notification_mutex);          /* AC107.4 */
shm->total_violations = *total_violations;
if (shm->violation_event_count < MAX_VIOLATION_EVENTS) {
    violation_event_t *event =
        &shm->violation_events[shm->violation_event_count++];
    event->timestamp = now;
    snprintf(event->flight_a, sizeof(event->flight_a), "%s", current_positions[i].flight_id);
    snprintf(event->flight_b, sizeof(event->flight_b), "%s", current_positions[j].flight_id);
    event->position_a = current_positions[i];
    event->position_b = current_positions[j];
    event->horizontal_distance_m = d_horiz;
    event->vertical_distance_m   = d_vert;
} else {
    shm->dropped_violation_events++;             /* bounded buffer overflow */
}
pthread_cond_signal(notification_cond);          /* AC107.2: wake the report thread */
pthread_mutex_unlock(notification_mutex);
```

The full event record (timestamp, both flight ids, both positions, horizontal and
vertical distances) is what lets the report be *"updated with accurate
information"*. The signal is emitted **under the mutex**, so the wake-up cannot be
lost between the producer updating the buffer and the consumer re-checking its
predicate.

### 4.3 Consumer side — `report_thread` (`main.c`)

```
report_thread
─────────────────────────────────────────────────────────────
reset_live_violation_log()
lock(g_notification_mutex)
while (events_left  ||  !sim_done):
    while (no new events  &&  !sim_done):     <-- while-predicate (AC107.3)
        cond_wait(g_report_cond, g_notification_mutex)
    while (processed < violation_event_count):
        copy event = violation_events[processed++]
        unlock(g_notification_mutex)          <-- do I/O without holding the lock
            append_violation_event_to_log(&event)
            write("[REPORT US107] Logged violation between A and B at T")
        lock(g_notification_mutex)
    if (dropped_violation_events grew): log an overflow notice
    if (sim_done && all processed): break
unlock(g_notification_mutex)
generate_final_report(...)                    <-- end-of-run aggregate (US109)
```

Two design points:

* **`while`-predicate wait** — mandatory under POSIX: `pthread_cond_wait` may
  return spuriously, and the predicate (`processed >= violation_event_count`)
  also closes the lost-wake-up window when a violation is signalled before the
  consumer reaches the wait.
* **Lock released during logging** — the report thread copies the event, then
  **unlocks before doing the (slower) file/stdout I/O**, re-locking afterwards.
  This keeps the critical section short and lets the safety thread keep producing
  while the report thread writes (the same "don't hold the lock during the heavy
  work" lesson used in US106).

### 4.4 Real-time vs end-of-run

US107 is responsible for the **real-time** per-violation logging
(`append_violation_event_to_log`, which appends to the live violation log, plus
the `[REPORT US107]` console line). The **aggregate** report assembled once the
simulation concludes (`generate_final_report`) is **US109** — the report thread
calls it after the loop ends. US107 and US109 therefore share the same thread but
cover different moments.

### 4.5 Alignment with the professor's examples

| Pattern | Professor example | Use in US107 |
|---------|-------------------|--------------|
| `mutex` + `cond var`, consumer waits on a `while`-predicate | T7/T8 slides | `g_notification_mutex` + `g_report_cond` |
| Producer signals under the lock | T7/T8 slides | `pthread_cond_signal` inside the locked section in `safety_monitor.c` |
| Bounded shared buffer between producer/consumer | `producer_consumer.c` | `shm->violation_events[MAX_VIOLATION_EVENTS]` with an overflow counter |
| `snprintf` + `write()` in threads (*"printf is not thread-safe"*) | `find_local_max` | `[REPORT US107]` log lines |

---

## 5. Implementation

### 5.1 Files Created/Modified

| File | Change summary |
|------|---------------|
| `simulation/main.c` | Declares `g_notification_mutex`/`g_report_cond`/`g_sim_done`; passes them to the safety and report thread contexts; the `report_thread` now drains and logs violation events in real time, waking on `g_report_cond` |
| `simulation/safety_monitor.c` | On each detected violation, records the `violation_event_t` in shared memory and signals `g_report_cond` under `g_notification_mutex` |
| `simulation/safety_thread.h` | The safety thread context (`safety_ctx_t`) carries the `g_notification_mutex`/`g_report_cond` pointers through to `monitor_safety_violations` |
| `simulation/report.c` / `report.h` | Live-log helpers used by the consumer: `reset_live_violation_log`, `append_violation_event_to_log`, `append_violation_drop_notice` |
| `simulation/shared_memory.h` | `sim_shm_t` holds the bounded event buffer (`violation_events`, `violation_event_count`, `dropped_violation_events`, `total_violations`) |

### 5.2 Key Implementation Details

**Thread-safe notification (AC107.4)** — the event buffer, the counters and the
signal are all touched only while `g_notification_mutex` is held, so producer and
consumer never race on the buffer and no wake-up is lost.

**Bounded buffer with overflow accounting** — the event buffer is fixed
(`MAX_VIOLATION_EVENTS`). If violations arrive faster than they fit, the producer
increments `dropped_violation_events` instead of overwriting; the report thread
logs a one-line overflow notice (`append_violation_drop_notice`) so the dropped
count is auditable rather than silently lost.

**Clean termination** — when the coordinator finishes it sets `g_sim_done = 1`
and signals `g_report_cond`. The report thread drains any remaining events, sees
`sim_done`, leaves the loop, and proceeds to the final report — so
`pthread_join(report_tid)` returns instead of hanging.

---

## 6. Integration / Demonstration

### Build

```bash
cd aisafe.base/simulation
make clean && make
```

Compiles with zero warnings under `-Wall -Wextra -std=c99`.

### Run — collision mode (triggers the notification)

```bash
./flight_simulator --collision
```

Expected, **interleaved with the live feed as each violation happens**:

```
[CYLINDER ALERT] Intersection risk detected at ...
[REPORT US107] Logged violation between UX1144 and FR9441 at 1750000000
```

The `[CYLINDER ALERT]` line is printed by the **safety thread** (producer); the
`[REPORT US107] Logged violation ...` line is printed by the **report thread**
(consumer) immediately after being woken on the condition variable — direct
evidence that the notification works in real time. The detailed event is also
appended to the live violation log (`simulation_violation_log.txt`).

### Run — normal mode

```bash
./flight_simulator
```

With no violations, the report thread simply blocks on `g_report_cond` until the
simulation ends (no busy-waiting), then generates the final report (US109).

---

## 7. Observations

**Real-time logging, not end-of-run** — US107's contribution is that a violation
appears in the log the moment it is detected, not when the simulation finishes.
The end-of-run aggregate report (`generate_final_report`) belongs to US109; both
run in the same `report_thread`.

**Separate channel by design** — US107 uses its own `g_notification_mutex` /
`g_report_cond`, distinct from the US106 coordinator↔safety `safety_channel_t`.
This keeps the two synchronisation concerns (per-step lock-step vs per-violation
event stream) independent.

**Overflow is observable, not silent** — under a flood of violations the bounded
buffer drops events but counts them, and the report thread surfaces that count.
Correctness of the notification mechanism does not depend on the buffer never
filling.
