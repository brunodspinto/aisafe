# AISafe SCOMP C Defense - Complete Source Walkthrough

## Scope

This guide explains every project-owned C source/header line under `aisafe.base/simulation/`,
the native tests, and the `Makefile`. Line references match `main` on 2026-06-21.

`cJSON.c` and `cJSON.h` are vendored third-party library files. They are not team-authored
SCOMP logic, so they are not reproduced line by line. Their API, ownership rules, and every
place where AISafe calls them are explained in the `flight_parser.c` section.

Blank lines separate logical blocks. A closing brace is explained together with the block it
closes. Multi-line declarations/calls are explained as one range because splitting a single C
statement into disconnected explanations would make it less accurate, not more detailed.

## 1. Architecture You Should Explain First

The simulator is a hybrid process/thread system:

```text
Java application
    |
    | starts native executable / exchanges files
    v
Parent simulator process
    |-- coordinator_thread: receives positions, maintains ACA history, issues GO/STOP
    |-- safety_thread: predicts/intersects trajectories, sends signals, records violations
    |-- environment_thread: publishes wind into shared memory
    `-- report_thread: consumes violation events and writes final reports
    |
    | fork() once per flight
    v
Flight child processes
    |-- write one position into their shared-memory slot
    |-- sem_post(position semaphore)
    |-- sem_wait(control semaphore)
    `-- continue on GO, terminate on STOP/SIGUSR1
```

There are three synchronization domains:

1. **Inter-process position/control barrier:** named semaphores. Child posts `pos_sem`; parent
   waits. Parent writes `ctrl` and posts `ctrl_sem`; child waits.
2. **Intra-process thread handoffs:** `pthread_mutex_t` plus `pthread_cond_t`. These coordinate
   coordinator/safety, coordinator/environment, and safety/report.
3. **Cross-process environment mutual exclusion:** a named semaphore initialized to 1 protects
   the shared `environment` block like a mutex.

## 2. Core C/POSIX Concepts

### Translation units, headers, and linkage

- A `.c` file is a translation unit compiled independently into `.o`.
- A `.h` file publishes types, constants, and function prototypes needed by other units.
- `#include "x.h"` textually exposes that interface at preprocessing time.
- Include guards (`#ifndef`, `#define`, `#endif`) prevent duplicate declarations when headers
  are reached through several include paths.
- `static` on a file-level function/variable gives internal linkage: only that `.c` file can
  name it. This prevents symbol collisions and hides implementation details.
- A prototype in a header and definition in a `.c` file form the cross-file contract.

### Pointers and ownership

- `T *p` stores an address of `T`; `p->field` means `(*p).field`.
- `const T *p` means the pointed object is read-only through `p`.
- Output parameters such as `flight_plan_t **out` let a function return allocated memory and
  another scalar/result simultaneously.
- `malloc/calloc` allocate heap memory; every successful allocation must eventually be paired
  with `free`. Nested allocations are freed inside-out: segments, then legs, then plans.
- `calloc` also zero-initializes memory, important for C strings and pointer fields.

### Processes and threads

- `fork()` duplicates the calling process. Return `<0` means failure, `0` means child, `>0`
  is the child's PID in the parent.
- Processes have separate ordinary address spaces; `MAP_SHARED` memory and named POSIX objects
  are used when they must communicate.
- Threads share process memory. Shared mutable thread state needs a mutex or another explicit
  synchronization protocol.
- `pthread_create` starts a `void *(*)(void *)` routine. A context struct is passed through the
  single `void *` argument. `pthread_join` waits for termination and prevents thread-resource
  leakage.

### Shared memory

- `shm_open` creates/opens a kernel-named memory object and returns a file descriptor.
- `ftruncate` gives that object the exact byte size required for `sim_shm_t`.
- `mmap(..., MAP_SHARED, ...)` maps it into a process. Writes become visible through mappings
  in other processes.
- Closing the descriptor does not invalidate an established mapping.
- `munmap` removes a process mapping; `shm_unlink` removes the name so future opens fail. The
  object survives until all mappings/descriptors disappear.

### Semaphores

- A semaphore is an integer synchronization counter.
- `sem_wait` atomically decrements or blocks at zero.
- `sem_post` atomically increments and wakes a waiter.
- Initial value 0 models an event/barrier; initial value 1 models mutual exclusion.
- Named semaphores (`sem_open`) work across processes; unnamed pthread mutexes only coordinate
  threads in the parent here.
- `sem_close` releases one process handle; `sem_unlink` removes the global name.

### Mutexes and condition variables

- A mutex protects an invariant and the state used as a wait predicate.
- `pthread_cond_wait(cond, mutex)` atomically releases the mutex and sleeps; on wake it
  reacquires the mutex before returning.
- Every wait is inside `while (!predicate)` because wakeups can be spurious and another thread
  may consume/change state first.
- Signaling without changing the predicate under the same mutex is incorrect; this code sets
  flags/counters first, then signals.

### Signals

- Signals are asynchronous process notifications.
- `sigaction` installs a handler and controls masking/restart behavior.
- A handler may interrupt normal code at almost any point, so it can safely modify only
  `volatile sig_atomic_t` state and call async-signal-safe functions such as `write` and `_exit`.
- `SA_RESTART` asks the kernel to restart many interrupted blocking calls.
- `kill(pid, SIGUSR1)` sends a warning/abort signal; `SIGTERM` requests termination.

### File descriptors and stdio

- File descriptors are small integers used by POSIX `read/write/close`, pipes, and mappings.
- `FILE *` is buffered C stdio used by `fopen/fread/fprintf/fclose`.
- `write` may be partial or interrupted by `EINTR`; `safe_write`/`tprintf` loop until the buffer
  is consumed or a real error occurs.
- `errno` is thread-local diagnostic state set by failed system calls. Preserve it in logging
  helpers so logging does not hide the caller's error.

## 3. `types.h` - Shared Data Contract

| Lines | Detailed explanation |
|---|---|
| 1-3 | File comment: this header centralizes layouts shared by parser, parent threads, children, safety, reports, and tests. A layout mismatch would corrupt shared-memory interpretation. |
| 4-5 | Include guard begins. `FLIGHT_SIMULATION_TYPES_H` is a unique preprocessor symbol. |
| 6 | `MAX_FLIGHTS=10` bounds fixed arrays and prevents unbounded shared-memory/stack use. Config validation enforces it. |
| 7 | `MAX_POSITIONS=1000` bounds per-flight history. The coordinator checks the count before writing. |
| 8 | `MAX_PERF_POINTS=20` bounds climb/descent performance tables. |
| 9 | `MAX_VIOLATION_EVENTS=256` bounds the report queue in shared memory; overflow is counted instead of overrunning memory. |
| 11 | `<time.h>` supplies `time_t`, used for endpoint, position, and violation timestamps. |
| 13-16 | `coordinate_t`: two `double` degree values. `typedef` gives the anonymous struct a reusable type name. |
| 18-23 | One altitude-indexed performance row. `vertical_rate_mps` sign encodes climb/descent direction. |
| 25-32 | `flight_profile_t` owns fixed climb/descent tables plus counts saying how many slots are valid; unused array elements remain irrelevant. Cruise has one speed. |
| 34-43 | `segment_t`: mode string, start/end coordinates, start/target altitude, corridor width, and wind. Fixed `char[16]` avoids separate heap ownership. |
| 45-48 | `endpoint_t`: 4-character airport code plus null terminator (`[5]`) and epoch timestamp. |
| 50-56 | `environment_t`: wind published by the environment thread. Meteorological direction means where wind comes from, not where it moves toward. |
| 58-65 | `leg_t`: dynamic `segments` pointer and count, endpoint metadata, fuel, and embedded performance profile. Parser allocates `segments`; caller must free it. |
| 67-72 | `flight_plan_t`: fixed identifier/type strings and dynamic `legs`. It is the top-level heap object produced by `flight_parser.c`. |
| 74-83 | `aircraft_position_t`: complete telemetry snapshot. It is copied atomically at C-struct assignment level into a flight's shared slot, then published by semaphore. |
| 85-94 | `simulation_params_t`: ACA bounds, flight count, safety-cylinder dimensions, and abort threshold loaded from config. |
| 96-102 | `geo_boundary_t`: normalized rectangle consumed by `is_in_aca`; separate from config so boundary logic has a narrow type. |
| 104-109 | `aca_state_t` enum is a state machine: never entered, currently inside, exited. Explicit values make zero-initialization equal `ACA_BEFORE`. |
| 111-116 | `flight_history_t`: bounded snapshots, valid count, ID, and ACA state. `count` is the authoritative initialized prefix of `positions`. |
| 118-126 | `violation_event_t`: immutable report record with pair identities, both complete positions/velocity vectors, timestamp, and measured separations. |
| 128-129 | Close include guard and trailing separator. |

## 4. ACA Filtering

### `aca_filter.h`

| Lines | Explanation |
|---|---|
| 1-3 | Module purpose and US101 ownership. |
| 4-5, 12 | Include guard lifecycle. |
| 7 | Imports `aircraft_position_t` and `geo_boundary_t` from `types.h`. |
| 9-10 | Public pure-function prototype. Both pointers are `const` because the test must not mutate telemetry or boundary. Return uses C convention 1=true, 0=false. |

### `aca_filter.c`

| Lines | Explanation |
|---|---|
| 1-4 | Module comment and own-header include. Including the own header checks definition/prototype compatibility at compile time. |
| 6 | Function definition; `->` dereferences struct pointers. |
| 7-10 | Four inclusive comparisons implement a closed rectangle. `&&` short-circuits and yields 0/1. North/south constrain latitude; west/east constrain longitude. Boundary points count as inside. |
| 11 | Ends the function. |

## 5. Configuration

### `config.h`

| Lines | Explanation |
|---|---|
| 1-7, 28 | Purpose, guard, and import of `simulation_params_t`. |
| 9-16 | Contract for `load_config`: output pointer mutation and three return categories. Positive line numbers make malformed input diagnosable. |
| 18-23 | Contract for independent semantic validation; it reports all invalid fields rather than stopping at the first. |
| 25-26 | Startup diagnostic function prototype. |

### `config.c`

| Lines | Explanation |
|---|---|
| 1-8 | Imports stdio (`FILE`, parsing output), stdlib (`atoi/atof`), string operations, character classification, and the public interface. |
| 10 | Preprocessor constant converts nautical miles to metres, keeping internal physics in SI. |
| 12 | `static` hides `trim` from other translation units; it returns a pointer into the same mutable buffer. |
| 13 | Advances over leading whitespace. Cast to `unsigned char` is required because passing a negative signed `char` to `isspace` is undefined behavior. |
| 14 | Empty-after-trim fast path. `\0` is the C-string terminator. |
| 15 | Points at final character using `strlen`; safe because line 14 excluded the empty string. |
| 16 | Replaces trailing whitespace with terminators while moving backward. Post-decrement means write, then move. |
| 17-18 | Returns first nonspace character and closes helper. No allocation occurs. |
| 20-28 | `load_config` starts by assigning defaults. Missing keys intentionally retain these values. Horizontal NM is converted immediately to metres. |
| 30-31 | Opens text file read-only. Null `FILE *` returns the documented `-1`. |
| 33-34 | Fixed line buffer prevents unbounded reads; line counter supports diagnostics. |
| 36-39 | `fgets` reads at most 255 chars plus terminator. Each line is trimmed; comments and blank lines skip to next iteration. |
| 41-45 | `strchr` finds first `=`. Absence is syntax failure: close the resource and return exact line. |
| 46-48 | Replacing `=` with `\0` splits one buffer into two C strings; key/value are trimmed views into it. |
| 50-57 | Exact key dispatch with `strcmp`. `atoi/atof` convert values; horizontal threshold is converted from NM. Unknown keys are ignored by design. |
| 58-62 | End loop, close file on success, return 0. Every successful `fopen` has `fclose`. |
| 64-65 | Validator starts optimistic (`ok=1`) to accumulate all errors. Pointer is assumed valid because main owns it. |
| 67-81 | Latitude range and north/south ordering checks. Each failure writes to `stderr` and clears `ok`, but validation continues. |
| 82-96 | Longitude range and west/east ordering checks using the same pattern. |
| 97-101 | Flight count must fit both business minimum and compile-time array maximum. |
| 102-111 | Safety cylinder dimensions must be positive; zero would make detection meaningless. |
| 112-116 | At least one violation is required as an abort threshold. |
| 118-119 | Ternary operator converts accumulated boolean to API contract `0` success, `-1` failure. |
| 121-132 | Prints normalized configuration. Multi-line `printf` arguments mirror format specifiers; horizontal metres are converted back to NM only for human display. |

## 6. Environment and Weather

### `environment.h`

| Lines | Explanation |
|---|---|
| 1-4, 17 | Guard and shared type import. |
| 6-10 | Contract for segment-based drift. Only `lat/lon` pointers are writable, preserving progress/termination variables. |
| 12-15 | Lower-level overload accepts explicit weather values from shared memory. C has no true overloads; these are differently named functions. |

### `environment.c`

| Lines | Explanation |
|---|---|
| 1 | Requests POSIX.1-2008 declarations before headers. Feature-test macros must precede includes. |
| 2-3 | Public interface and trigonometric functions. |
| 5-7 | Portable fallback because strict C modes do not guarantee `M_PI`. |
| 9 | Parenthesized macro converts degrees to radians safely in expressions. |
| 11-12 | Pointer outputs let the function update caller coordinates in place. |
| 13 | Nonpositive speed is treated as calm and returns without mutation. |
| 14 | Distance equals speed times elapsed seconds. |
| 15 | Adds 180 degrees because meteorological direction is FROM; trig bearing is movement TO. |
| 16-17 | Resolve drift vector into north/east components. |
| 18 | Approximate metres per latitude degree. |
| 19 | Longitude metres-per-degree shrinks by `cos(latitude)`; updated latitude is close enough for one-step approximation. |
| 20 | End explicit-values function. |
| 22-24 | Wrapper extracts wind from `segment_t` and delegates, keeping one implementation of the mathematics. |

### `weather_service.h/.c`

| Lines | Explanation |
|---|---|
| h1-9 | Documents this as an adapter/stub: external weather is modeled through `AISAFE_WIND`, with calm fallback. |
| h10-18 | Guard, type import, and output-parameter API. `step` leaves room for time-varying weather. |
| c1-7 | Module comment, POSIX feature selection, interface, `getenv`, and parsing support. |
| c9-13 | Explains future use; `(void)step` explicitly marks the currently unused parameter and suppresses warnings. |
| c14-15 | Initialize output to a valid calm state before optional parsing. |
| c17-18 | `getenv` returns pointer to process environment storage or `NULL`; caller must not free it. |
| c19-24 | Parse two doubles with `sscanf`; assignment occurs only when exactly two fields parse and speed is positive. Invalid input safely leaves calm defaults. |
| c25-26 | Close conditional and function. |

## 7. Thread-Safe Output

### `thread_io.h` and `thread_io.c`

| Lines | Explanation |
|---|---|
| h1-8 | Rationale: thread routines format locally and issue `write` rather than sharing stdio buffering state. |
| h9-15 | Guard and variadic public prototype. `...` means a variable argument list interpreted by the format string. |
| c1-10 | Feature macro and headers for variadics, formatting, string sizes, POSIX write, and `errno`. |
| c12-17 | Allocate thread-local stack buffer; initialize `va_list`, format with bounded `vsnprintf`, then always call `va_end`. |
| c18 | Negative formatting result means failure, so emit nothing. |
| c20-21 | `vsnprintf` returns desired length, which may exceed buffer; ternary clamps to bytes actually stored, excluding terminator. |
| c23-25 | Save caller's `errno`; initialize byte offset. |
| c26-33 | Loop over partial writes. `buf+off` is pointer arithmetic to unsent bytes. Retry interruption (`EINTR`), stop on real failure, cast positive `ssize_t` to `size_t`. |
| c34-35 | Restore diagnostic state and end function. |

## 8. History Utilities (`ipc.h/.c`)

| Lines | Explanation |
|---|---|
| h1-7 | Guard and type contract. Despite the filename, these helpers operate on data produced by IPC; they do not create IPC resources. |
| h9-12 | Find-or-create API mutates history and returns index/-1. |
| h14-16 | Report history API and guard end. |
| c1-7 | Imports string comparison/copy and `tprintf`, because this output runs in coordinator thread. |
| c9-14 | Linear search for an exact existing ID; `strcmp==0` means equality. |
| c15-20 | Empty slot is identified by first null byte. `strncpy` is bounded and line 18 forces termination; state explicitly starts `ACA_BEFORE`. |
| c21-24 | Close loop and return `-1` if all bounded slots are occupied. |
| c26-30 | History printer locals; position pointer avoids repeatedly copying large structs; title uses thread-safe output. |
| c31-37 | Skip unused slots. Zero-count entries are reported then `continue` avoids nested `else`. |
| c39-45 | Print count and state-dependent suffix; separate calls are logically related but each uses safe writer. |
| c47-52 | Walk only initialized prefix `[0,count)`, take address of each snapshot, print telemetry fields. |
| c53-54 | Close loops/function. |

## 9. Shared Memory and Named Semaphores

### `shared_memory.h`

| Lines | Explanation |
|---|---|
| 1-7 | Module purpose: one shared object replaces position/control pipes, while named semaphores publish changes and impose ordering. |
| 8-13 | Include guard, POSIX feature level, semaphore API, and every shared struct type. |
| 15 | POSIX shared-memory names begin with `/`; all processes must use exactly this name. |
| 16 | `printf`-style position semaphore template. Comment documents direction: child posts, coordinator waits. |
| 17 | Control semaphore template and reverse direction. |
| 18 | Single environment semaphore name; initial count 1 makes it a cross-process mutex. |
| 19 | Maximum formatted semaphore-name buffer, including terminator. |
| 21-31 | `sim_shm_t` core: one position/control/active slot per flight; final counters; bounded event queue and overflow count. Arrays avoid pointers because raw pointer values are not meaningful across independently mapped process address spaces. |
| 32-38 | Environment block and version/tick. The comment states the invariant: named semaphore protects reads/writes that must be observed consistently. |
| 40-47 | Lifecycle prototypes: parent creates, child attaches, parent unmaps/unlinks. Returning mapped pointer makes the caller own `munmap`. |
| 49-55 | Per-flight semaphore factories and environment semaphore factory. `create` selects parent versus child behavior. |
| 57-60 | Global unlink cleanup and guard end. |

### `shared_memory.c`

| Lines | Explanation |
|---|---|
| 1-7 | Documents the exact professor patterns being implemented. |
| 8-17 | POSIX feature macro and headers: memory mapping, permission bits, open flags, errors/output, zeroing, semaphore operations, descriptor close, own contract. |
| 19-21 | Parent creation starts by unlinking a stale crashed-run name. Unlink failure is intentionally ignored because nonexistence is normal. |
| 22 | `shm_open`: create exclusively, read/write, owner read/write permissions. `O_EXCL` prevents silently attaching to another live simulator. |
| 23 | System-call failure uses `perror`, which combines label and current `errno`, then terminates because IPC is essential. |
| 24 | `ftruncate` sizes the initially zero-length object to exactly one `sim_shm_t`. |
| 25-27 | Map any virtual address (`NULL` hint), full struct length, read/write protection, shared visibility, offset 0; compare against special `MAP_FAILED`, not `NULL`. |
| 28 | Close descriptor after mapping; mapping remains valid and avoids descriptor leak. |
| 30-33 | Zero every byte so counts/flags/strings/padding start deterministic; set flight count and mark only configured slots active. |
| 35-36 | Return mapping and end creator. |
| 38-45 | Child attach: open existing name without create, map same size/shared mode, close descriptor, return mapping. Parent must already have created it. |
| 48-51 | Teardown separates mapping lifetime (`munmap`) from name lifetime (`shm_unlink`). Parent performs it after joins/waits. |
| 53-55 | Comment distinguishes event count 0 from mutex count 1; helper is `static` implementation detail. |
| 56-63 | Parent path unlinks stale name then creates exclusive semaphore with permissions/initial value; child path opens existing semaphore. |
| 64-66 | `SEM_FAILED` is the semaphore API's failure sentinel. Return opaque `sem_t *`. |
| 68-72 | Build `/aisafe_pos_i` safely with `snprintf`; delegate with initial value 0. |
| 74-78 | Same for `/aisafe_ctrl_i`, also event-style zero. |
| 80-83 | Environment semaphore has one available token, hence lock/unlock behavior via wait/post. |
| 85-92 | Loop through configured indices and unlink both per-flight names. Reusing one stack buffer is safe because `sem_unlink` consumes each string during the call. |
| 93-94 | Unlink environment semaphore and end cleanup. |

## 10. Safety Geometry and Signals

### `safety_monitor.h`

| Lines | Explanation |
|---|---|
| 1-7 | Guard plus pthread/PID/shared types needed by the API. Header exposes synchronization because monitor records events and wakes report thread. |
| 9-26 | `monitor_safety_violations` contract. Inputs include movement windows, active flags (legacy name `pipe_open`), child PIDs, shared report queue, report mutex/cond, thresholds and abort limit. Return 1 means caller must abort. |
| 28-36 | Future prediction API consumes plans and segment starting indices. `flight_plan_t *const *` means pointer entries are const through this view, while plan objects are treated read-only by implementation. |
| 38 | Guard end. |

### `safety_monitor.c`

| Lines | Explanation |
|---|---|
| 1-10 | Libraries provide diagnostics, signals/PIDs, trig, bounded copying, timestamps, own API, and thread-safe output. |
| 13 | Explicit `kill` prototype is redundant with `<signal.h>`/`<unistd.h>` on POSIX, but declares the intended signature. |
| 15 | Ten interpolation substeps trade precision for fixed predictable cost; includes both endpoints (`0..10`). |
| 17-19 | Portable pi fallback. |
| 21-22 | Private distance helper takes two positions and two output pointers, returning both horizontal and vertical components. |
| 23 | Mid-latitude improves longitude scale approximation. |
| 24 | Longitude degree delta to metres, corrected by cosine latitude. |
| 25 | Latitude degree delta to metres. |
| 27 | Euclidean horizontal norm via Pythagoras. |
| 28-29 | Absolute altitude separation and helper end. |
| 31-33 | Private trajectory intersection receives previous/current points for two aircraft and cylinder thresholds. |
| 34-35 | Sample normalized time `t` from 0 to 1; cast prevents integer division. |
| 37-44 | Linear interpolation of lat/lon/alt for both aircraft. Only fields used by distance are initialized; other struct fields are intentionally irrelevant here. |
| 46-47 | Compute cylinder components at this synchronized substep. |
| 49-50 | A safety-cylinder violation requires both horizontal and vertical separation strictly below thresholds. Immediate return short-circuits remaining samples. |
| 51-53 | No sample intersects, return false and close function. |
| 55-61 | Prediction selects the two plans and rejects null/empty plans. Only first leg is considered by this advisory implementation. |
| 63-64 | Addresses of first legs; no copy. |
| 66-73 | Iterate future segments for A and construct endpoint-only positions from route geometry/altitudes. |
| 75-82 | Nested loop does the same for every future segment of B, producing Cartesian pair comparison. |
| 84-89 | Reuse intersection algorithm; on first risk, log pair/segment indices and return true. It is advisory and does not signal/abort. |
| 91-94 | Close loops and return no prediction. |
| 96-101 | Runtime monitor definition matches header; long signature explicitly carries all state rather than hidden globals. |
| 102-105 | Fix current flight `i`; compare only `j>i` so each unordered pair is checked once. Skip B without two samples or inactive status. |
| 107-109 | Measure current separation for report details. Intersection decision itself uses movement windows. |
| 111-113 | Detect whether trajectories crossed the cylinder between observations, preventing tunneling from coarse positions. |
| 114-118 | Capture current epoch time and emit alert/threshold context through `tprintf`. `ctime` includes a newline. |
| 120-121 | Send `SIGUSR1` to both child processes. Their handlers set collision flags and use async-safe output. |
| 123 | Increment shared logical total through pointer owned by safety thread. Parenthesized dereference is required before postfix `++`. |
| 125 | Lock report-notification mutex before modifying queue/counters that report thread reads. |
| 126 | Publish current total. Mutex establishes memory ordering between producer and consumer threads. |
| 127-129 | Capacity check prevents out-of-bounds write. Post-increment reserves current queue slot and advances count. |
| 130-138 | Fill the reserved event completely while mutex is held: timestamp, bounded IDs, full position structs, measured separations. Struct assignment copies all fields by value. |
| 139-141 | Full queue increments dropped counter instead of corrupting adjacent shared memory. |
| 142 | Signal one report waiter after predicate state changed. Signal need not wake safety itself. |
| 143 | Unlock publishes the complete event and allows report thread to consume it. |
| 145-147 | Compare accumulated violations with configured threshold and log critical abort. |
| 148-150 | Send `SIGTERM` to every active child. `pipe_open` is semantically active flags despite legacy naming. |
| 151 | Return 1 to safety thread, which forwards abort verdict to coordinator. |
| 153-156 | Close violation/pair loops and return 0 when threshold not reached. |

## 11. Coordinator-Safety Ping-Pong

### `safety_thread.h`

| Lines | Explanation |
|---|---|
| 1-9 | Documents responsibility and two-way handoff. This is more than comments: it states the synchronization protocol maintainers must preserve. |
| 10-16 | Guard and dependencies for PID, pthread primitives, telemetry/plans, and shared report block. |
| 18-25 | Protocol invariant: coordinator produces snapshot; safety produces verdict; finish flag releases shutdown wait. |
| 26-31 | Snapshot arrays are copied under mutex. Fixed size makes channel self-contained and eliminates pointer-lifetime races. |
| 32-34 | Verdict fields owned by safety during production and coordinator after wake. |
| 35-40 | Predicate/state flags plus mutex/condition variable. Flags, not condition variable alone, carry truth. |
| 41 | Completes channel type. |
| 43-53 | Thread context bundles every dependency: channel, plans, config copy, child PIDs, count, shared queue, and report synchronization. Config is copied so its lifetime is independent. |
| 55-57 | POSIX thread routine prototype and guard end. |

### `safety_thread.c`

| Lines | Explanation |
|---|---|
| 1-11 | Module contract and crucial lock-duration rule: copy under lock, compute outside lock. |
| 12-16 | POSIX feature level, `memcpy/memset`, pthread API, own context, and safety algorithms. |
| 18-21 | Cast generic thread argument back to context; cache frequently used channel/count pointers/values. Caller guarantees context lifetime through join. |
| 23-25 | Stack-local pair matrix suppresses repeated future-prediction warnings. `memset` zero means not yet predicted. Only safety thread accesses it, so no lock. |
| 27 | Thread-local accumulated violation count. |
| 29-34 | Infinite worker loop; lock channel and wait while neither work nor shutdown is available. `pthread_cond_wait` releases and reacquires same mutex. |
| 35-38 | If shutdown with no pending work, unlock before break to avoid leaving mutex permanently owned. |
| 39 | Consume `step_ready` under lock. |
| 41-47 | Allocate local snapshots and copy every channel array while protected. Whole-array `sizeof` avoids element-count mistakes. |
| 48 | Unlock before O(flights^2 * segments^2) safety computation, allowing coordinator to sleep/wake correctly without unnecessarily holding lock. |
| 50-52 | Build array of plan pointers because predictor expects pointer-to-pointer API. |
| 53-65 | Pair loops skip inactive/no-position flights; only `i<j`; mark pair before prediction so warning runs once even if no collision is found. |
| 67-70 | Initialize this step's abort verdict; runtime monitoring requires two observations per active flight. |
| 71-77 | Cross-file call into `safety_monitor.c`, passing local snapshots, child PIDs, shared event queue and report synchronization. |
| 78-80 | Convert monitor return into abort verdict and stop checking more flights after critical threshold. |
| 83-89 | Re-lock channel, publish abort/count, set predicate, broadcast coordinator, unlock. Broadcast is safe even with one current waiter and supports protocol evolution. |
| 90-93 | Repeat until shutdown; `pthread_exit(NULL)` terminates cleanly with no result. |

## 12. Reporting

### `report.h`

| Lines | Explanation |
|---|---|
| 1-4, 16 | Guard and shared report/history types. |
| 6 | Reset/truncate live log at simulation start. |
| 7 | Append one immutable event consumed from shared queue. |
| 8 | Append queue-overflow warning. |
| 10-14 | Final-report API receives snapshots plus counts/status. `const` prevents report writer from mutating simulation results. |

### `report.c`

| Lines | Explanation |
|---|---|
| 1-6 | Stdio/file errors/time/shared types/API/thread-safe console output. |
| 8 | File-local constant pointer; string literal has static lifetime, pointer is not exported. |
| 10-18 | Open live log with `"w"` to truncate old run, fail fast on inability to persist, write heading, close to flush/release descriptor. |
| 21-24 | Prepare timestamp buffer, convert epoch to broken-down local time, open log with append mode. |
| 25-28 | Fatal file-open handling. |
| 30-33 | Format timestamp if conversion succeeds; otherwise print raw epoch. `snprintf` is bounded. |
| 35-43 | Write event summary and both positions. Format precision controls presentation, not stored precision. |
| 44-45 | Close/flush append and end function. |
| 47-57 | Overflow-notice function repeats append/error/close lifecycle and records number omitted from bounded queue. |
| 60-71 | Private formatting helper writes one position plus velocity vector. It centralizes identical A/B output. |
| 74-78 | Final report signature accepts histories, event array/count, total/dropped counts and abort state. |
| 79-81 | Comment and `tprintf` announce work without unsafe shared stdout buffering. |
| 83-87 | Open final report with truncation; persistence failure aborts process because requirement cannot be met. |
| 89 | Capture generation time once. |
| 90 | Validation passes only if not aborted and zero violations; logical `&&` and `!` produce 0/1. |
| 92-105 | Write report header, human timestamp, ternary-selected status/result, totals, live-log reference, separators. |
| 107-110 | Violation section and explicit empty case. |
| 111-119 | For each stored event, select event by address and format timestamp with fallback. |
| 121-128 | Write indexed pair, timestamp and separations. Adjacent string literals concatenate at compile time. |
| 129-131 | Delegate A/B vectors and add spacing. |
| 132-133 | Close event loop/branch. |
| 135-137 | Begin flight execution section and iterate configured flight count. |
| 138-141 | Print ID and execution state derived from global abort. |
| 143-148 | `switch` maps enum to human label; each `break` prevents fall-through. `default` covers `ACA_BEFORE` and defensive unknowns. |
| 149-151 | Print ACA label and bounded snapshot count. |
| 153-155 | Explicit message for no snapshots. |
| 156-163 | Iterate only initialized snapshots and print position/velocity fields. |
| 164-166 | Close branch, spacing, flight loop. |
| 168-171 | Footer, `fclose` flush/persist, thread-safe success message, function end. |

## 13. JSON Flight Parser

### `flight_parser.h`

| Lines | Explanation |
|---|---|
| 1-4, 19 | Guard and `types.h`, needed because API exposes `flight_plan_t`. |
| 6-16 | Doxygen contract documents file input, allocated output array, count output, and success/failure code. Caller becomes owner of returned nested heap allocations. |
| 17 | `flight_plan_t **` is required because function assigns a newly allocated pointer into caller variable; `int *` similarly returns count. |

### `flight_parser.c`

| Lines | Explanation |
|---|---|
| 1-5 | Own API, third-party cJSON API, stdio, heap allocation, strings. This is the only project module directly coupled to cJSON. |
| 7-8 | Private whole-file reader. Returning `char *` transfers heap ownership to caller. |
| 9-13 | Binary read mode avoids platform text transformations; null file reports OS error and returns sentinel. |
| 15-17 | Seek end, query byte length with `ftell`, rewind. This requires a seekable regular file. |
| 19 | Allocate content bytes plus one C-string terminator. Cast is unnecessary in C but harmless. |
| 20-24 | Allocation failure closes already-open file and returns null. |
| 26-31 | Read exact byte count. `fread` returns items read; mismatch triggers close, free, failure. Cast aligns signed `long` with unsigned `size_t` comparison. |
| 33-36 | Add terminator expected by cJSON, close file, transfer buffer. |
| 38-42 | Public parser reads file and propagates failure. Output pointers are assumed non-null by API contract. |
| 44 | `cJSON_Parse` builds a dynamically allocated DOM tree. |
| 45 | Input text is no longer needed after parse; DOM owns its own parsed nodes/strings. |
| 46-52 | Parse failure queries cJSON's error pointer, prints nearby input, and returns failure. No root exists to delete. |
| 54-58 | Schema root must be array. `cJSON_Delete` recursively frees DOM on wrong shape. |
| 60-62 | Get array count, return it through output pointer, allocate zeroed contiguous plan array. Zero defaults make missing optional values safe. |
| 63-67 | Allocation failure frees DOM before return. |
| 69-72 | cJSON traversal macro iterates root children; `i` tracks matching destination slot; address avoids copying plan. |
| 74-78 | Lookup case-sensitive identifier; validate JSON string/non-null; bounded copy and forced terminator prevent overflow/nonterminated strings. |
| 80-84 | Same pattern for flight type. |
| 86-89 | Legs must be array; count determines dynamic allocation size. |
| 90-101 | If leg allocation fails, unwind every previously completed plan's segment arrays, then legs, top array, DOM. This is manual exception safety in C. |
| 103-106 | Iterate leg DOM entries and select corresponding allocated `leg_t`. |
| 108-111 | Read segment array/count and allocate zeroed segments. |
| 112-127 | Segment-allocation rollback frees earlier segments in current plan, current legs, every previous plan's nested allocations, top array and DOM. Order is inner-to-outer. |
| 129-132 | Iterate segments and select destination `segment_t`. |
| 134-138 | Read `start_coord` only when array of exactly two; array items provide numeric `valuedouble`. |
| 140-144 | Same for end coordinate. |
| 146-150 | Read altitude number and use same value as start/target when JSON exposes one altitude. |
| 152-160 | Parse optional mode; bounded copy when present, otherwise default `cruise` for backward compatibility. Because destination was zeroed, bounded copy remains terminated. |
| 162-167 | Parse optional wind direction/speed with ternary fallback zero, preserving old JSON compatibility. |
| 169-176 | Advance segment, leg, and plan indices and close traversal blocks. |
| 178 | Transfer top-level heap pointer to caller. |
| 179 | Delete cJSON DOM now that all required values were copied into project structs. |
| 180-181 | Return success and close function. |

### cJSON reference and ownership

- `cJSON_Parse(text)` allocates a tree; ownership belongs to caller until `cJSON_Delete(root)`.
- `cJSON_GetObjectItemCaseSensitive` and `cJSON_GetArrayItem` return borrowed pointers into
  that tree. They must not be freed individually and become invalid after `cJSON_Delete`.
- AISafe copies strings/numbers into its own structs before deleting the tree.
- `cJSON_ArrayForEach` is a library macro that advances through sibling nodes.

## 14. Native Validation

### `validation.h`

| Lines | Explanation |
|---|---|
| 1-7, 13-14 | Purpose, guard, and type dependency. |
| 9-11 | Three boolean-style APIs return 1 valid, 0 invalid. Parameters are const because validation observes only. |

### `validation.c`

| Lines | Explanation |
|---|---|
| 1-7 | Purpose and dependencies for diagnostics, strings, domain types, and own API. |
| 9-13 | Reject null simulation pointer before dereference; report to `stderr`; return false. |
| 14-23 | Validate ordered ACA bounds. This tester validator stops at first error, unlike `config.c`'s accumulator. |
| 25-29 | Enforce fixed-array flight capacity. |
| 31-39 | Reject negative cylinder dimensions. Note this permits zero, whereas production config requires strictly positive; know this distinction in defense. |
| 41-44 | Abort threshold must be positive. |
| 46-47 | All checks passed. |
| 49-53 | Flight plan null guard. |
| 55-58 | Identifier must be nonempty C string. |
| 60-63 | Type must be exactly `REGULAR` or `CHARTER`; parentheses make intended `AND` grouping explicit. |
| 65-68 | Dynamic legs pointer must exist. |
| 70-73 | Logical leg count must be positive. Both pointer and count are checked because either can be inconsistent. |
| 75-76 | Valid plan return/end. |
| 78-82 | Coordinate null guard. |
| 84-87 | Latitude geographic range. |
| 89-92 | Longitude geographic range. |
| 94-96 | Valid coordinate and end. |

## 15. Flight Child Process

### `flight_process.h`

| Lines | Explanation |
|---|---|
| 1-9 | Guard and dependencies for named semaphores, plans/config, and shared-memory layout. |
| 11-14 | Documents the publish/barrier protocol and ownership directions. |
| 15-18 | Child entry point receives its array index, read-only plan/config, shared mapping, and its two semaphore handles. It returns `void` because it terminates via `exit`. |
| 20 | Guard end. |

### `flight_process.c`

| Lines | Explanation |
|---|---|
| 1-10 | File-level design: physics, shared-memory publication, semaphore barrier, signals, and professor examples. |
| 11-24 | Feature-test macro and APIs for output, exit, POSIX calls, strings/signals/time/trig/semaphores/unmap, plus project contracts. |
| 26-28 | Portable pi fallback. |
| 30-32 | `static volatile sig_atomic_t` is file-private signal-shared state. `volatile` forces actual reads/writes; `sig_atomic_t` is handler-safe atomic scalar. |
| 34-42 | SIGUSR1 handler ignores numeric parameter, defines stack/static-size message, writes asynchronously safe bytes excluding terminator, consumes result, then sets alert flag. No `printf`, allocation, locks, or non-safe cleanup. |
| 44-46 | One-second simulation tick, radian and knot conversion constants. Parentheses protect macro substitution precedence. |
| 48-53 | Approximate horizontal distance: midpoint latitude, degree-to-metre deltas, Euclidean norm. Private helper returns one `double`. |
| 55-57 | Performance lookup API uses output pointers for speed and vertical rate. Empty table gets deterministic defaults and immediate return. |
| 58-62 | Below first point or singleton table uses first point. |
| 63-67 | Above highest altitude clamps to last point. |
| 68-76 | Find surrounding altitude interval, compute normalized interpolation fraction, linearly interpolate speed and vertical rate, return. |
| 79-81 | Defensive fallback to last point if no interval matched due floating/data irregularity. |
| 83-86 | `flight_done` private cleanup contract and parameters. |
| 87 | Mark slot inactive before publishing completion; coordinator reads this after semaphore wake. |
| 88 | Post position event even though no new position: it wakes coordinator to observe `active=0`, preventing deadlock. |
| 89-91 | Close child handles for position/control/environment named semaphores. Closing does not unlink global names. |
| 92-93 | Unmap child's shared view and finish helper. Parent still owns global unlink. |
| 95-98 | Main child simulation signature. |
| 99 | Config currently unused inside child; cast suppresses warning while preserving API. |
| 101-104 | Null plan is fatal because every later dereference depends on it. |
| 106-113 | Initialize `sigaction` deterministically; set handler, `SA_RESTART`, full mask during handler, install for SIGUSR1. Full mask blocks other signals while handler executes. |
| 115-116 | Child-only startup stdio followed by explicit flush so output appears before blocking. |
| 118-119 | Open existing environment semaphore; parent created it before fork/child attachment. |
| 121 | Start logical simulation clock from real current epoch. It advances deterministically by one second per step. |
| 123-128 | Nested plan traversal. `const` pointers prevent accidental mutation of parsed route/profile data. |
| 130-133 | Initialize mutable position and target altitude from current segment. |
| 135-140 | Coordinate direction deltas and true horizontal segment length. |
| 141 | Clamp degenerate distance to 1 to avoid division by zero. Upstream validation should normally reject zero-length routes. |
| 143-144 | Approximate heading via `atan2(dlon,dlat)`, convert to degrees, normalize negative to `[0,360)`. |
| 146-148 | Decode mode once using string equality. Integers act as booleans. Unknown mode falls into descent branch later, relying on parser validity. |
| 150 | Cruise progress starts zero. |
| 152-156 | Step loop with mode-specific termination: altitude target for climb/descent, horizontal distance for cruise. |
| 158-168 | Select speed/vertical rate: cruise profile/default or interpolated climb/descent table. Output parameters fill local variables. |
| 170-171 | Convert knots to m/s; distance this one-second step. |
| 173-175 | Advance along coordinate direction proportionally to metre step and accumulate termination progress. This is an approximation because degree deltas are scaled by metre distance. |
| 177-179 | Advance altitude and clamp overshoot exactly to target. |
| 181-184 | Comment states wind locking and termination invariant. |
| 185-188 | Acquire cross-process environment semaphore, copy the small block by value, release immediately. Short critical section avoids holding lock during trig. |
| 189-193 | Prefer environment-service wind if positive; otherwise delegate segment wind. Both mutate only lat/lon. |
| 195-205 | Build a fully initialized telemetry struct. `memset` clears padding/unused bytes; assign physical values/time; bounded `snprintf` copies ID. |
| 207 | Copy complete position into this child's exclusive shared slot. No mutex is needed because one child writes one slot and semaphore publication orders parent read. |
| 208 | Post event after write: this is the release/publish point for coordinator. |
| 209 | Advance logical timestamp. |
| 211-213 | Wait for parent's GO/STOP response. `SA_RESTART` handles SIGUSR1 interruption behavior. |
| 214-217 | Stop if shared control says STOP or signal flag is set; publish completion, clean local resources, exit nonzero. |
| 218-220 | Close step, segment, and leg loops. |
| 222-225 | Normal exhaustion publishes completion and exits zero. `exit` does not return, matching header's `void`. |

## 16. `main.c` - Complete Simulator Orchestration

### File setup and output helper

| Lines | Explanation |
|---|---|
| 1-19 | Architectural comment attributes each responsibility to US105/106 and lists POSIX patterns. It is the high-level defense summary embedded in code. |
| 20 | Requests POSIX.1-2008 APIs before any system headers. |
| 21-30 | System APIs: descriptors/processes/wait/signals/stdio/exit/strings/pthreads/semaphores/`errno`. |
| 31-40 | Project interfaces. This list is the cross-file dependency map: types, config, history, ACA, parser, child entry, report, shared IPC, safety thread, weather source. |
| 42-44 | Runtime filenames and collision-mode flight limit. Macros centralize literals used in main. |
| 46-50 | Comment and private full-write helper signature. `fd` generalizes stdout or another descriptor. |
| 51-61 | Preserve `errno`, loop partial writes, retry `EINTR`, stop on real logging error, advance pointer offset, restore `errno`. Same principle as `tprintf`; local helper avoids variadic formatting. |
| 62 | End helper. |

### Thread context structures

| Lines | Explanation |
|---|---|
| 64-67 | Visual section separator only. |
| 68-81 | `coordinator_ctx_t` bundles shared mapping, per-flight semaphores, history, ACA/config copies, PIDs/plans, safety channel, and report synchronization. Passing one context satisfies pthread's single-argument API. |
| 82-87 | Environment handoff fields: parent-thread mutex/condition and shared tick/done predicates. Pointers ensure both threads observe the same variables. |
| 89-97 | `environment_ctx_t`: shared destination, cross-process environment semaphore, intra-parent thread synchronization, and predicates. The two lock types protect different domains. |
| 99-106 | `report_ctx_t`: final shared results/history and notification predicate state. |

### Coordinator thread: telemetry collection

| Lines | Explanation |
|---|---|
| 108-111 | Section comment and pthread-compatible routine signature. `static` keeps symbol local. |
| 112-114 | Recover context and cache shared pointer/count. Cast is valid because `pthread_create` passes `&coord_ctx`. |
| 116-119 | Previous/current movement windows plus per-flight sample count. Zeroing counts determines which arrays are logically initialized. |
| 121-123 | Parent-local active flags and active count. Initialize only configured flights. |
| 125-126 | Running safety result and abort state. |
| 128-130 | Stack formatting buffer and actual formatted length for safe writes. |
| 132 | Emit live-feed heading. Length excludes C terminator. |
| 134-139 | Outer simulation barrier loop; for each active flight wait on its position event. Sequential waits create one complete global step snapshot. |
| 141-148 | Completion event: child posted semaphore after setting shared `active=0`. Mirror locally, decrement active count, log plan ID, skip telemetry processing. |
| 151 | Copy shared slot by value after semaphore wake. Each child exclusively writes its own slot. |
| 153-156 | Cross-file calls to `is_in_aca` and `find_or_create_flight`; pass addresses, not copies. History capacity uses `MAX_FLIGHTS`. |
| 157-164 | If history slot exists and this is first inside sample, log ACA entry. Previous state is captured before mutation. |
| 165-167 | Set state inside; append snapshot only below fixed capacity. Post-increment writes at old count then advances initialized length. |
| 168-174 | Format and emit live telemetry with bounded `snprintf`; output length drives `safe_write`. |
| 175-181 | Outside ACA after previously inside means exit transition; log and set irreversible `ACA_AFTER`. Other outside states do nothing. |
| 182-183 | Close ACA/history conditions. |
| 185-191 | Maintain sliding movement window: first sample uses same position as previous, later samples shift current to previous; assign new current and increment sample count. |
| 192 | Close per-flight collection loop; global snapshot is now complete. |
| 194 | If all children completed during collection, leave outer loop before safety handoff. |

### Coordinator thread: safety handoff and control barrier

| Lines | Explanation |
|---|---|
| 196-202 | Protocol comment: safety computation is function-specific and coordinator blocks for verdict. |
| 203-204 | Cache channel and lock its mutex before touching predicates/data. |
| 205-208 | Copy complete movement/active snapshot into channel. Producer owns these fields while lock held. |
| 209-210 | Clear old verdict and set new work predicate in that order. |
| 211 | Broadcast after predicate update wakes safety waiter. |
| 212-213 | Wait in predicate loop. `cond_wait` releases mutex so safety can acquire it, then reacquires before return. |
| 214-215 | Consume safety verdict/count while still protected. |
| 216 | Unlock channel; snapshot/verdict transaction is complete. |
| 217-218 | Residual explanatory comments; actual prediction/verification happened in `safety_thread.c`. |
| 220-230 | On abort, set final state, write STOP to every active shared slot, post each control semaphore so no child remains blocked, clear locals, force loop termination. |
| 233-238 | Normal step signals environment thread: lock predicate mutex, increment tick, signal, unlock. Tick is a generation counter, safer than a one-bit event because changes are comparable. |
| 240-246 | Write GO and post control semaphore for each active child, releasing them into next step. Data write precedes semaphore publication. |
| 247 | End outer simulation loop. |

### Coordinator shutdown

| Lines | Explanation |
|---|---|
| 249-256 | Wake safety shutdown: set `sim_finished` under channel mutex and broadcast so a thread waiting with no step exits. |
| 258 | Cross-file history print executes after simulation loop, before report signaling. |
| 260-270 | Under report mutex publish final total/abort, set `g_sim_done`, signal report condition, unlock. This ensures report sees a coherent final state. |
| 272-276 | Under environment mutex set done and broadcast so environment thread cannot remain blocked during `pthread_join`. |
| 278-279 | Terminate coordinator thread and close function. |

### Environment thread

| Lines | Explanation |
|---|---|
| 281-285 | Section comment and pthread routine declaration. |
| 286-288 | Recover context and allocate local output buffer/length. |
| 290-296 | Fetch step-zero weather before first tick; acquire named environment semaphore, copy environment/tick into shared memory, release. This supplies children from their first movement. |
| 297-300 | Log initial wind through bounded formatting and safe write. |
| 302 | `last_tick` records consumed generation. |
| 303-306 | Infinite worker loop; wait while no new generation and not done. Predicate loop handles spurious wakeups. |
| 307-310 | Done with no unconsumed tick: unlock and exit. If a tick remains, process it before exit. |
| 311-312 | Snapshot tick under mutex, then unlock before service call and cross-process lock. |
| 314-318 | Fetch weather and publish environment/tick under named semaphore, protecting child readers in other processes. |
| 319-320 | Mark generation consumed and repeat. |
| 322-323 | Exit thread cleanly. |

### Report thread

| Lines | Explanation |
|---|---|
| 325-328 | Section and routine declaration. |
| 329-331 | Recover context; counters track queue entries and overflow notices already persisted. |
| 333-337 | Consumer protocol comment and live-log reset before waiting. |
| 339-342 | Lock report mutex; outer condition means continue until simulation done and all queued events consumed. Inner wait sleeps only when queue empty and simulation not done. |
| 344-347 | Copy next event by value and advance consumer index while protected; prepare logging buffer. |
| 348 | Unlock before file I/O, avoiding long critical section and allowing safety producer to append. Local event copy remains valid. |
| 350-354 | Cross-file append to live log and thread-safe console notification. |
| 356-357 | Reacquire mutex before reading queue predicates/counters again. |
| 359-363 | Detect increased drop count, snapshot it, allocate warning buffer, unlock before file I/O. |
| 365-370 | Append only newly dropped count and print cumulative warning. |
| 372-374 | Re-lock and update acknowledged drop generation. |
| 376-378 | Once producer marked done and consumer caught up, break while still holding mutex. |
| 379-383 | Snapshot final counters, event count/array pointer and abort state while protected. No producer writes after done/caught-up invariant. |
| 384 | Unlock before final report file generation. Shared mapping remains alive until report join. |
| 386-389 | Emit final-report transition through safe descriptor output. `strlen` avoids stale hard-coded length. |
| 390-392 | Cross-file call into `report.c`, passing histories and immutable final shared results. |
| 394-395 | Exit report thread. |

### `main`: startup and input

| Lines | Explanation |
|---|---|
| 397-400 | Section and C entry point. `argc` counts arguments; `argv` is array of C-string pointers. |
| 401-405 | Stack config, load file, distinguish open failure, report to `stderr`, return nonzero process status. |
| 407-410 | Positive result identifies malformed config line. |
| 412-413 | Semantic config validation; nonzero means fatal. Single-line `if` controls following return only. |
| 415-422 | Initialize parser outputs, cross-file JSON parse, and reject failure. Parser allocates `plans`. |
| 424-431 | Default to all parsed plans; scan arguments from index 1 and enable collision mode by limiting to first two. |
| 432-437 | Defensive clamp if requested count exceeds available parsed plans. |
| 438-441 | Compile-time capacity clamp protects fixed arrays even if input contains more plans. |
| 443 | Ignore SIGPIPE. This prevents process termination if a legacy/broken pipe write occurs; current main communication uses semaphores/shared memory. |
| 445-452 | Startup diagnostics, ternary mode selection, cross-file config print, loaded IDs, and `fflush` before forking to avoid duplicated buffered output in children. |

### `main`: IPC creation and fork

| Lines | Explanation |
|---|---|
| 454-455 | Parent creates and maps shared `sim_shm_t` before fork so all later actors can attach/use it. |
| 457-462 | Allocate handle arrays and create two named zero-count semaphores per configured flight. |
| 463-464 | Create one named count-1 environment mutex. |
| 466-470 | PID/history fixed arrays; zero history then explicitly set enum initial state (zero already equals BEFORE, but explicit assignment documents intent). |
| 472-477 | Designated initializer constructs normalized ACA boundary from config, independent of field declaration order. |
| 479-482 | Fork one child per plan. Store PID in parent array; failure is fatal. |
| 483 | Child-only branch because fork returns zero there. |
| 484-489 | Explains descriptor/handle inheritance: each child initially inherits all handles opened so far. Unused references must be closed. |
| 490-494 | Child closes every inherited per-flight and environment semaphore handle. Names remain; close affects only this process reference. |
| 496-499 | Child makes its own shared mapping and opens only its own position/control semaphores. Environment is reopened inside child routine. |
| 500-502 | Enter child simulation with matching plan/index. Function exits process, so following comment says unreachable. |
| 503-504 | Parent skips child block and continues loop to fork remaining flights. |

### `main`: synchronization initialization and contexts

| Lines | Explanation |
|---|---|
| 506-511 | Create parent-thread report mutex/condition and done predicate on stack; runtime initialization is mandatory before use. |
| 513-520 | Separate environment thread mutex/condition and tick/done predicates, also runtime initialized. |
| 522-528 | Stack safety channel is zeroed so predicates/counters start false, then its embedded mutex/condition are initialized. Never `memset` an initialized pthread object; zeroing correctly occurs first. |
| 530-546 | Populate coordinator context field by field with addresses of long-lived main-stack objects. They remain alive until all joins. |
| 547-550 | Copy only configured semaphore handles into context arrays. |
| 552-559 | Populate environment context with shared mapping, named semaphore, and thread predicate addresses. |
| 561-570 | Populate safety context, linking it to channel, plans, PID array, final queue and report wakeup. |
| 572-579 | Populate report context with shared mapping, histories, count and done synchronization. |

### `main`: thread lifecycle, child reaping, cleanup

| Lines | Explanation |
|---|---|
| 581-586 | Thread launch comments and four opaque `pthread_t` IDs. Environment is created first so initial weather can be published. |
| 587-590 | Create environment thread; nonzero pthread error is treated fatal. Note pthread APIs return error numbers directly rather than necessarily setting `errno`; `perror` is conventional here but not perfectly diagnostic. |
| 591-594 | Create safety worker before coordinator so it can wait for first snapshot. |
| 595-598 | Create coordinator, which begins driving children. |
| 599-602 | Create report consumer. Shared predicates ensure it can still observe events/done even if producer runs quickly. |
| 604-609 | Join all four. Coordinator joins first because it initiates shutdown predicates; environment/safety/report then have guaranteed exit paths. Join is also a memory-synchronization point. |
| 611-617 | Reap every child with `waitpid`, preventing zombies. `WIFEXITED` validates normal termination before extracting `WEXITSTATUS`. |
| 619-625 | Destroy pthread mutexes/conditions only after no thread can use them. Destroying earlier would be undefined behavior. |
| 627-631 | Close parent semaphore handles. Child handles were closed in `flight_done`. |
| 632 | Unlink all semaphore names after processes/threads finish. |
| 633 | Unmap and unlink shared memory after report has consumed it and children are reaped. |
| 634 | Free parser's top plan array. Important caveat: nested legs/segments are not freed here, unlike tester cleanup; this is a memory leak at process shutdown, mostly reclaimed by OS but defensible as an improvement point. |
| 636-637 | Return success to shell and end program. |

## 17. Standalone Flight Tester (`flight_tester_main.c`)

This executable is separate from the multi-flight simulator. It validates one JSON plan and
deliberately demonstrates many POSIX mechanisms in isolation for US085.

| Lines | Explanation |
|---|---|
| 1-19 | Contract, JSON output/exit codes, and explicit POSIX API checklist. These comments define what the Java caller can expect. |
| 21-37 | POSIX feature level and dependencies for processes, pipes, signals, shared memory, semaphores, pthreads, parser, validation and types. |
| 39-47 | Global handler-visible state. PID is volatile because asynchronously read/written; signal flag uses `sig_atomic_t`. IPC names are populated before handlers so abnormal cleanup can find objects. |
| 49-55 | SIGUSR1 handler marks failure and sends SIGTERM to active child. It ignores signal number. `kill` is async-safe. |
| 57-60 | Documents SIGTERM cleanup caveat: unlink calls are not formally async-signal-safe, so this is best-effort pragmatic cleanup. |
| 61-68 | SIGTERM handler kills child to avoid orphan, conditionally unlinks populated names, then `_exit` skips unsafe stdio/atexit processing. |
| 70-80 | Coordinator context: read end, bounded history and count pointer, GO semaphore, mutex/condition, and done predicate. |
| 82-87 | Thread casts context and repeatedly reads exactly one `aircraft_position_t` from pipe. Short reads terminate loop; production code assumes writes are struct-sized and within pipe atomicity limits. |
| 88-92 | Protect history count/array mutation; append only below capacity; unlock promptly. |
| 93-95 | Post GO after consuming position, establishing tester step barrier. |
| 97-101 | Pipe EOF means child closed writer; set done under mutex, signal main waiter, unlock. |
| 103-104 | Return null is equivalent to `pthread_exit(NULL)`. |
| 106-109 | Child simulation helper receives plan, pipe writer, GO semaphore, shared-memory mutex and mapped latest-position pointer. |
| 110-113 | Iterate every leg and segment by pointer. |
| 115-119 | Validate both segment endpoints; invalid coordinate closes pipe (creating EOF) and exits failure. |
| 121-126 | Zero and populate one endpoint position from segment destination and target altitude. |
| 128-132 | Write exact struct to pipe; partial/failure closes and exits. Pipe copies bytes between process kernel buffers. |
| 134-139 | If mapping exists, acquire named binary semaphore, copy latest position, release. This demonstrates shared memory independently of pipe history. |
| 141-143 | Child blocks on named GO semaphore before next segment. |
| 145-147 | Close write end to generate EOF and exit success. |
| 149-157 | `main` usage guard. It emits machine-readable FAIL JSON and nonzero status if path missing. Adjacent C literals concatenate. |
| 159-167 | Initialize parser outputs, parse JSON, reject failure/zero plans. |
| 169 | Tester intentionally uses first parsed plan only. |
| 171-180 | Native validation failure: preserve identifier safely, print FAIL, free top-level leg arrays and plans, return. Caveat: this early path does not free per-leg `segments`, an improvement point. |
| 182-184 | Copy identifier before later freeing plan memory. Forced terminator guarantees valid output string. |
| 186-192 | Build PID-suffixed names so concurrent tester processes do not collide in global POSIX namespace. |
| 194-203 | Create exclusive zero-count GO semaphore with owner permissions; on failure report JSON and free plans. |
| 205-215 | Create anonymous pipe. `pfd[0]` is read end, `[1]` write end. Failure closes/unlinks prior semaphore and frees memory. |
| 217-228 | Create shared-memory object for one latest position; failure unwinds pipe/semaphore. |
| 229-240 | Resize object to one position; on failure close/unlink every resource acquired so far. |
| 241-254 | Map shared position with read/write shared visibility, close descriptor after mapping, detect `MAP_FAILED`, unwind on error. |
| 255 | Zero mapped position so final read is deterministic even if child emits nothing. |
| 257-270 | Create second named semaphore initialized 1 as shared-memory mutex; unwind all earlier resources on failure. |
| 272-275 | Copy names into global handler buffers before handlers are installed. Static globals were zero-initialized, and source names fit buffers. |
| 277-282 | Install SIGUSR1 handler with empty additional mask. No `SA_RESTART`, so interrupted operations may report `EINTR`; main logic treats signal as abort. |
| 284-289 | Install SIGTERM handler used when Java destroys timed-out process. |
| 291-305 | Fork tester child; failure performs complete IPC/pthread-independent unwind and returns FAIL. |
| 307-314 | Child restores default SIGTERM, closes unused pipe read end, runs simulation with write end and inherited mappings/semaphores. |
| 316-317 | Parent closes unused write end. This is essential: otherwise reader would never see EOF after child closes its copy. |
| 319-321 | Static bounded history/count. Static storage gives zero initialization and avoids a very large stack allocation. |
| 323-329 | Explain and runtime-initialize stack mutex/condition. |
| 331-339 | Designated initializer wires coordinator context, including addresses of shared main-thread state. |
| 341-360 | Create coordinator thread. Failure kills/reaps child, releases mapping/descriptors/semaphores/names/sync objects/plans, prints FAIL. |
| 362-367 | Main waits on done predicate under mutex; condition wait loop handles spurious wakeups. |
| 369-374 | Reap child, join coordinator, destroy synchronization objects after use. |
| 376-379 | Extract normal child exit status with wait macros; external SIGUSR1 forces failure regardless. |
| 381-385 | After child exit there is no writer, so final shared position can be copied without semaphore. This is synchronization by process termination/wait. |
| 387-394 | Close pipe, unmap, close/unlink both semaphores, unlink shared memory. Order releases local handles then global names. |
| 396-405 | Correct deep cleanup for normal path: segments inside each leg, legs inside each plan, top array. |
| 407-413 | PASS JSON includes identifier, number of pipe-consumed steps, and final mapped lat/lon; return 0. |
| 414-420 | FAIL reason distinguishes signal abort from validation/simulation error; return 1. |
| 421-422 | Close branch/function. |

## 18. `Makefile`

| Lines | Explanation |
|---|---|
| 1 | Compiler variable allows command-line override (`make CC=clang`). |
| 2 | Warnings, C99 language, optimization, and current-directory header search. `-Wall/-Wextra` support defense-quality diagnostics. |
| 3 | Link math (`cos/sqrt`), pthread APIs. Libraries belong at link stage. |
| 5-9 | Detect OS with immediate `:=`; Linux adds realtime library for older systems where `shm_open` lives in `librt`. |
| 11 | Complete simulator source list. Includes vendored `cJSON.c` because parser calls its symbols. |
| 12 | Make substitution transforms every `.c` into matching `.o`. |
| 13 | Main binary name. |
| 15-18 | Standalone tester sources/objects/output. It does not link full simulator modules because it has its own coordinator/IPC demonstration. |
| 20-23 | Validation test binary composition. |
| 25-28 | Flight tester test binary includes parser/validator/cJSON. |
| 30-33 | Shared-memory unit test links implementation under test. |
| 35-38 | Environment math unit test links only environment implementation. |
| 40 | Default target builds simulator and tester. |
| 42-43 | Simulator link rule. `$@` target, `$^` all prerequisites. Recipe lines require a tab. |
| 45-46 | Tester link rule. |
| 48-49 | Pattern rule compiles any `.c` prerequisite into `.o`; `$<` is first prerequisite. |
| 51-52 | `run` depends on up-to-date simulator then executes it. |
| 54-57 | Test target dependency comment and continuation backslash. Tester binary is prerequisite because integration test calls it via `system`. |
| 58-61 | Execute four native test binaries sequentially; make stops on first nonzero status. |
| 63-73 | Individual link rules for each test executable. |
| 75-81 | `clean` removes every generated object/binary. Backslashes continue one shell recipe line. `rm -f` tolerates missing files. |
| 83-84 | `.PHONY` prevents files named `all/run/test/clean` from suppressing recipes; trailing blank line. |

## 19. Native Test Framework Pattern

All four test files use a tiny dependency-free framework:

- `tests_run/tests_failed` are file-private counters.
- `ASSERT_TRUE` is a multi-line macro wrapped in `do { ... } while (0)` so it behaves as one
  statement in any `if/else` context.
- `__LINE__` is a preprocessor builtin reporting assertion source line.
- `main` runs cases, prints counts, and returns `EXIT_SUCCESS` only when no failures occurred.

### `tests/test_validation.c`

| Lines | Explanation |
|---|---|
| 1-8 | Test dependencies and zero-initialized counters. |
| 10-17 | Assertion macro increments run count, negates condition, prints diagnostic to stderr and increments failures. Backslashes continue macro definition. |
| 19-23 | Heap-allocate zeroed plan fixture; return null on allocation failure. |
| 25-27 | Populate bounded strings and logical leg count. Zeroed allocation supplies terminators. |
| 29-33 | Allocate leg array; on failure free already-owned plan. |
| 35-41 | Allocate segment array; failure frees legs then plan. |
| 43-44 | Transfer valid fixture ownership to test. |
| 46-60 | Deep destroy helper tolerates null, frees each segment array, legs, then plan. |
| 62-72 | Designated initializer builds valid config fixture. |
| 74 | Assert baseline valid. |
| 76-85 | Mutate one field at a time to invalid latitude order, restore, invalid longitude order, restore, and zero flight count. Isolation makes failures attributable. |
| 86 | End config test. |
| 88-92 | Valid coordinate fixture. |
| 94-101 | Assert valid, then below-range latitude, restore latitude, above-range longitude. |
| 102 | End coordinate test. |
| 104-109 | Create plan, assert fixture, guard against dereferencing allocation failure. |
| 111 | Baseline valid plan. |
| 113-115 | Make identifier empty, assert failure, restore. |
| 117-121 | Set unsupported type, force terminator, assert, restore valid type/terminator. |
| 123-125 | Set zero leg count, assert, restore. |
| 127-131 | Free legs and set pointer null, assert failure, then destroy remaining top plan safely. |
| 134-143 | Run three test groups, print counters, ternary return success/failure. |

### `tests/test_flight_tester.c`

| Lines | Explanation |
|---|---|
| 1-15 | Scope/build requirements and parser/validation dependencies. |
| 17-29 | Same minimal framework; macro arguments are parenthesized to avoid precedence surprises. |
| 31-56 | Parse valid fixture, assert return/count/identifier/type/legs/segments, deep-free nested allocations and top array. Conditional guards prevent invalid dereference. |
| 58-66 | Aggregate initialization creates valid/bad coordinates; assert geographic boundaries. |
| 68-76 | Zero a stack plan, set ID/type, deliberately leave no legs, assert validator rejection. |
| 78-93 | Integration PASS uses `system` with shell redirection to `/tmp`; checks zero shell status, reads output file boundedly, terminates buffer, searches for `"PASS"`. |
| 95-108 | Integration FAIL runs invalid fixture, expects nonzero and `"FAIL"` in captured output. |
| 110-125 | Execute unit and integration cases, print totals, return status. |

### `tests/test_shared_memory.c`

| Lines | Explanation |
|---|---|
| 1-19 | Test intentions, POSIX feature selection, semaphore/errno/memory APIs, own module. `sem_trywait` gives nonblocking behavioral assertions. |
| 21-31 | Counters and assertion macro. |
| 35-49 | Create 3-flight segment; assert mapping, configured active prefix, inactive next slot, zero counters/events/env; destroy mapping/name. |
| 52-70 | Create and attach second mapping, write through creator, assert visibility through viewer, then unmap/unlink. Calling `shm_destroy` twice causes second `shm_unlink` to fail harmlessly because implementation ignores unlink result. |
| 72-90 | Create pos/control events at zero. `sem_trywait` must fail `EAGAIN`; post makes one token consumable exactly once; close and unlink. |
| 93-106 | Environment semaphore starts one: first acquire succeeds, second nonblocking acquire fails, post releases, acquire succeeds again; close/unlink. |
| 108-120 | Verify cleanup by attempting second unlink and expecting `ENOENT` for semaphore and shared-memory names. |
| 122-133 | Run five cases, print totals, return status. |

### `tests/test_environment.c`

| Lines | Explanation |
|---|---|
| 1-14 | Test contract, math/stdio/stdlib and environment API. |
| 16-26 | Counters and boolean assertion macro. |
| 28-29 | `ASSERT_NEAR` checks floating values within epsilon using `fabs`; exact equality is unsuitable for trig/floating arithmetic. |
| 33-43 | Calm and negative speed calls must leave lat/lon unchanged to very small tolerance. |
| 45-56 | Wind FROM north converts to southward drift: latitude decreases by metres/degree, longitude unchanged. |
| 58-70 | Wind FROM west converts to eastward drift: longitude increases with latitude cosine correction, latitude unchanged. |
| 72-89 | Keep separate progress variables, call drift with only coordinate addresses, assert progress/altitude unchanged and coordinates moved. This verifies API-level termination invariant. |
| 91-106 | Populate segment wind, run wrapper and explicit-values API from equal starts, assert identical results. |
| 108-119 | Run five cases, print totals, return status. |

## 20. Cross-File Call Graph

```text
main.c
  -> config.c: load_config, validate_config, print_config
  -> flight_parser.c -> cJSON: parse flight_plans.json into flight_plan_t
  -> shared_memory.c: create mapping and named semaphores
  -> fork -> flight_process.c
       -> environment.c: apply wind drift
       -> shared_memory.c: open environment semaphore
  -> coordinator_thread
       -> aca_filter.c: is_in_aca
       -> ipc.c: find_or_create_flight, print_history
       -> safety_thread.c through safety_channel_t
  -> safety_thread.c
       -> safety_monitor.c: prediction and runtime cylinder detection
       -> child processes through kill(SIGUSR1/SIGTERM)
       -> report_thread through shared queue + condition signal
  -> environment_thread
       -> weather_service.c -> getenv/sscanf
       -> child processes through shared environment + named semaphore
  -> report_thread
       -> report.c: live log and final report
```

The standalone `flight_tester_main.c` is a second executable. It reuses
`flight_parser.c`, `validation.c`, `types.h`, and cJSON but implements its own pipe/shared-
memory/semaphore/thread demonstration.

## 21. Defense Questions and Precise Answers

### Why shared memory plus semaphores, not shared memory alone?

Shared memory provides visibility but not timing. Without semaphores the parent could read
while a child is halfway through a struct write, read the same step twice, or spin wasting CPU.
The semaphore establishes event count, blocking, and happens-before ordering.

### Why both named semaphores and pthread mutexes?

Named semaphores synchronize separate processes. The pthread mutexes/conditions synchronize
threads inside the parent and carry richer predicate-based handoffs. The environment named
semaphore is required because flight readers are child processes.

### Why are condition waits inside `while`, not `if`?

POSIX permits spurious wakeups, and a signal only says state may have changed. The predicate is
the truth. Rechecking under the mutex prevents lost/incorrect consumption.

### Why copy snapshots before safety computation?

It shortens channel lock duration and gives safety an immutable step view. Coordinator is
blocked waiting for verdict, so it cannot overwrite channel state; safety can perform expensive
pair/segment loops without monopolizing the mutex.

### Why does a finishing child post `pos_sem`?

The coordinator is blocked waiting for that flight's next event. Setting `active=0` without a
post would never wake it, causing deadlock. The post publishes completion rather than position.

### Why close inherited semaphore handles after fork?

Each process receives copies of open handles. Keeping unrelated handles obscures ownership and
can delay resource release. Children close all inherited handles and reopen only the names they
actually use.

### Why does the report thread unlock around file I/O?

File I/O can block. Holding the notification mutex would prevent the safety thread from appending
new events. The report thread copies one event under lock, unlocks, writes it, then relocks.

### What are the principal improvement points?

1. Deep-free `plans[i].legs[j].segments` and `plans[i].legs` in simulator `main` before
   `free(plans)`.
2. Check return codes from `sem_wait`, `sem_post`, pthread initialization/join, `munmap`, and
   unlink operations where recovery/diagnostics matter.
3. Replace `perror` for pthread return codes with `strerror(return_code)`.
4. Normalize comments/legacy `pipe_open` naming now that pipes were replaced.
5. Validate all required JSON fields and numeric node types before dereferencing array items.
6. Consider `localtime_r` in threaded reporting instead of `localtime`; current use is isolated
   enough in practice but the reentrant variant states intent better.

These do not invalidate the architecture, but mentioning them honestly in defense demonstrates
that you understand both the implementation and its engineering limits.

## 22. Coverage Matrix

The walkthrough above covers every line from 1 through EOF for each project-owned file. Blank
lines and closing braces are included with the logical block immediately before/after them.

| File | Lines covered |
|---|---:|
| `types.h` | 1-129 |
| `aca_filter.h` | 1-12 |
| `aca_filter.c` | 1-11 |
| `config.h` | 1-28 |
| `config.c` | 1-132 |
| `environment.h` | 1-17 |
| `environment.c` | 1-24 |
| `weather_service.h` | 1-18 |
| `weather_service.c` | 1-26 |
| `thread_io.h` | 1-15 |
| `thread_io.c` | 1-35 |
| `ipc.h` | 1-16 |
| `ipc.c` | 1-54 |
| `shared_memory.h` | 1-60 |
| `shared_memory.c` | 1-94 |
| `safety_monitor.h` | 1-38 |
| `safety_monitor.c` | 1-156 |
| `safety_thread.h` | 1-57 |
| `safety_thread.c` | 1-93 |
| `report.h` | 1-16 |
| `report.c` | 1-171 |
| `flight_parser.h` | 1-19 |
| `flight_parser.c` | 1-181 |
| `validation.h` | 1-14 |
| `validation.c` | 1-96 |
| `flight_process.h` | 1-20 |
| `flight_process.c` | 1-225 |
| `main.c` | 1-637 |
| `flight_tester_main.c` | 1-422 |
| `Makefile` | 1-84 |
| `tests/test_validation.c` | 1-143 |
| `tests/test_flight_tester.c` | 1-125 |
| `tests/test_shared_memory.c` | 1-133 |
| `tests/test_environment.c` | 1-119 |

Third-party integration coverage: `cJSON.h/.c` is explained at its AISafe call boundary in
section 13. Its internal implementation belongs to the upstream cJSON library, not the team's
SCOMP defense code.
