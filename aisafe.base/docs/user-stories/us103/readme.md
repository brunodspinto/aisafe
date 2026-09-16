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

# Run from the simulation directory (reads simulation.conf and flight_plans.json)
cd aisafe.base/simulation

# Normal simulation (3 flights, no guaranteed collision)
./flight_simulator

# Collision test (4 flights; FLIGHT_04 triggers cylinder alert)
./flight_simulator --collision
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
