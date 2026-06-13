# US108 — Enforce Step-by-Step Simulation Synchronization

## 1. Context

US108 formalizes the **lockstep synchronization** between flight child processes and the
parent coordinator thread that was first introduced in US103 and preserved through US105.

In US103, each simulation step was gated by a blocking `read`/`write` pair on unnamed
pipes — only one side could proceed at a time. US105 replaced all pipe IPC with POSIX
shared memory, and **named semaphores** became the sole mechanism controlling step
progression. US108 makes this design choice explicit: semaphores are the synchronization
primitive that enforces the step-by-step contract.

The implementation lives entirely in `aisafe.base/simulation/` and requires no new
files — the semaphore infrastructure was delivered as part of US105 (`shared_memory.c/h`,
`flight_process.c`, `main.c`).

---

## 2. Requirements

**US108:** As a PO, I want the simulation engine to synchronize the simulation's
step-by-step progression using semaphores, so that all flight processes and parent threads
advance in lockstep through each simulation time step.

### Acceptance Criteria

| ID | Criterion | Status |
|----|-----------|--------|
| AC1 | Semaphores are used to control the progression of each simulation time step | Done |

---

## 3. Analysis

### 3.1 What "step-by-step" means

At each simulation step, two things must happen in strict order:

1. **Flight child writes** its new position to shared memory
2. **Coordinator reads** the position, checks safety, and decides GO or STOP

Without enforced ordering, the coordinator could read a partially-written position struct,
or a flight could advance to step T+1 before the coordinator has finished processing step T.
Both are race conditions that would corrupt the simulation.

### 3.2 Semaphore design

Two **named POSIX semaphores** per flight enforce the ordering, both initialized to **0**
(the event-signalling pattern from `ex2-5.c` and T6 slides — distinct from the
mutual-exclusion pattern that uses initial value 1):

| Semaphore | Name format | Direction | Initial value | Semantic |
|-----------|------------|-----------|---------------|----------|
| `pos_sem` | `/aisafe_pos_N` | child → coordinator | 0 | "I have written my position; you may read" |
| `ctrl_sem` | `/aisafe_ctrl_N` | coordinator → child | 0 | "I have checked safety; you may advance" |

Named semaphores are required (not anonymous `sem_init`) because they are shared between
**independent processes** — parent and child have separate address spaces and cannot share
a pointer. Named semaphores are identified by a string in the OS namespace and can be
opened independently by any process that knows the name.

---

## 4. Design

### 4.1 Step-by-step protocol

```
Flight child i                         coordinator_thread (parent)
───────────────────────────────────────────────────────────────────
shm->positions[i] = pos                 (waiting)
sem_post(pos_sems[i])      ──────────►
                                        sem_wait(pos_sems[i])
                                        pos = shm->positions[i]
                                        ACA filter + history (US101)
                                        hand off snapshot to safety_thread (US106)
                                        shm->ctrl[i] = 1 (GO) or 0 (STOP)
                                        sem_post(ctrl_sems[i])
sem_wait(ctrl_sems[i])     ◄──────────
if ctrl[i] == 0 → flight_done(); exit(1)
/* ctrl[i] == 1 → advance to next step */
```

The `sem_post → sem_wait` pair establishes a POSIX **happens-before** relationship: the
coordinator's read of `shm->positions[i]` is guaranteed to observe the value written by
the child's `shm->positions[i] = pos`. No additional mutex is needed to protect the
position slot because the semaphore already enforces exclusive temporal access — only one
side is active on that slot at any given time.

### 4.2 Flight termination

When a flight completes all its segments it calls `flight_done()`, which signals the
coordinator via `pos_sem` one last time with `active[i] = 0`:

```c
static void flight_done(int idx, sim_shm_t *shm, sem_t *pos_sem, sem_t *ctrl_sem,
                        sem_t *env_sem) {
    shm->active[idx] = 0;
    sem_post(pos_sem);          /* wake coordinator to see active[idx]=0 */
    sem_close(pos_sem);
    sem_close(ctrl_sem);
    sem_close(env_sem);         /* US110 — close the environment-block mutex */
    munmap(shm, sizeof(sim_shm_t));   /* pattern ex1-7.c */
}
```

The coordinator checks `shm->active[i] == 0` after each `sem_wait(pos_sems[i])` and
removes the flight from the active set — no extra "done" semaphore is needed.

### 4.3 Abort flow

When the safety monitor triggers an abort (`total_violations >= max_violations`), the
coordinator sends STOP to all active flights and breaks out of the step loop:

```c
shm->ctrl[i] = 0;           /* STOP */
sem_post(ctrl_sems[i]);     /* unblock each child */
```

Each child's `sem_wait(ctrl_sems[i])` returns, sees `ctrl == 0`, and calls `flight_done()`
which posts `pos_sems[i]`. Since the coordinator has already exited its loop, no one waits
on those posts — the semaphores are left with value 1 and released by `cleanup_sems()`.
No deadlock occurs.

### 4.4 Alignment with professor's examples

| Pattern | Professor example | Use in US108 |
|---------|------------------|--------------|
| Named semaphore initial value 0 (event signalling) | `ex2-5.c` | `pos_sem` / `ctrl_sem` per flight |
| `sem_open(O_CREAT\|O_EXCL)` + `sem_unlink` before creation | `ex1-7.c`, `ex2-5.c` | Crash recovery for stale named semaphores |
| `sem_post` → `sem_wait` as happens-before | T6 slides (semaphore invariant) | Position write/read ordering |
| Child calls `sem_close` before `exit` | POSIX standard | `flight_done()` |

---

## 5. Implementation

### 5.1 Files

No new files were created for US108. The relevant code is part of US105:

| File | Relevant section |
|------|-----------------|
| `simulation/shared_memory.h` | `SEM_POS_FMT`, `SEM_CTRL_FMT` constants; `open_pos_sem`, `open_ctrl_sem`, `cleanup_sems` declarations |
| `simulation/shared_memory.c` | `open_named_sem()`, `open_pos_sem()`, `open_ctrl_sem()`, `cleanup_sems()` |
| `simulation/flight_process.c` | `sem_post(pos_sem)` + `sem_wait(ctrl_sem)` in the step loop; `flight_done()` |
| `simulation/main.c` | `open_pos_sem(i, 1)` + `open_ctrl_sem(i, 1)` per flight; `sem_wait(pos_sems[i])` + `sem_post(ctrl_sems[i])` in `coordinator_thread`; `sem_close` + `cleanup_sems` in cleanup |

### 5.2 Semaphore lifecycle

```
Parent (main):
  shm_create()                    ← shared memory
  open_pos_sem(i, 1)  ×N          ← create /aisafe_pos_i, value=0
  open_ctrl_sem(i, 1) ×N          ← create /aisafe_ctrl_i, value=0
  fork() per flight
    Child i:
      close all inherited sem handles
      open_pos_sem(i, 0)           ← attach to /aisafe_pos_i
      open_ctrl_sem(i, 0)          ← attach to /aisafe_ctrl_i
      open_env_sem(0)              ← attach to /aisafe_env (US110)
      flight_process_main(...)
      flight_done() → sem_close ×3 (pos, ctrl, env), munmap, exit
  pthread_create(coordinator_thread)
    coordinator_thread:
      sem_wait(pos_sems[i]) ×N per step
      sem_post(ctrl_sems[i]) ×N per step
  pthread_join(...)
  waitpid(...)
  sem_close ×2N
  cleanup_sems()                  ← sem_unlink /aisafe_pos_i, /aisafe_ctrl_i ×N
  shm_destroy()
```

---

## 6. Integration / Demonstration

### Build

```bash
cd aisafe.base/simulation
make clean && make
```

### Run

```bash
./flight_simulator           # normal mode
./flight_simulator --collision   # forces safety violation + abort
```

### Evidence of lockstep

The live feed output shows flights advancing strictly one step at a time, interleaved only
after each position has been safely processed:

```
[FLIGHT_01] lat=39.1904 lon=-7.7525 alt=11000m spd=250kt ...   ← step T
[FLIGHT_02] lat=38.9200 lon=-9.1354 alt=9200m  spd=240kt ...   ← step T
[FLIGHT_01] lat=39.1918 lon=-7.7499 alt=11000m spd=250kt ...   ← step T+1
[FLIGHT_02] lat=38.9214 lon=-9.1327 alt=9200m  spd=240kt ...   ← step T+1
```

No flight ever appears at step T+1 before all flights at step T have been processed.

---

## 7. Observations

**Lockstep preserved from US103** — The step ordering contract is identical to US103:
the coordinator collects all positions for step T before issuing any GO/STOP for step T.
The only change is the IPC mechanism — semaphores replace blocking `read`/`write` on pipes,
but the happens-before guarantee is the same.

**Semaphore vs. mutex for position slots** — A mutex would also enforce mutual exclusion,
but would require the child to lock before writing and the parent to lock before reading.
The semaphore pattern is strictly stronger: it not only prevents concurrent access but also
enforces the *ordering* (child writes first, parent reads second), which is exactly the
property needed for correct simulation step progression.

**`SA_RESTART` interaction** — Flight children install `SIGUSR1` with `SA_RESTART`
(US102/US105). If `SIGUSR1` arrives while the child is blocked in `sem_wait(ctrl_sem)`,
the kernel automatically restarts the `sem_wait` call rather than returning `EINTR`. This
keeps the semaphore protocol clean — the child always exits the wait at a defined step
boundary, never mid-step.
