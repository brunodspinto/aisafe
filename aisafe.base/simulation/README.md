# Simulation

C module that simulates flights in an Air Control Area and detects safety violations. It is written for POSIX systems (Linux, macOS or WSL).

## What it does

- Loads parameters from `simulation.conf` and flight plans from `flight_plans.json`.
- Forks one child process per flight. Each child advances its route step by step and writes its position to a POSIX shared-memory segment.
- The parent process coordinates the flights with named semaphores (GO/STOP barrier) and runs four threads:
  - **coordinator**: collects positions, keeps each flight's history and filters positions by Air Control Area
  - **safety**: checks the horizontal/vertical safety separation, signals violating flights (`SIGUSR1`) and aborts the run once `max_violations` is reached
  - **environment**: publishes wind data into shared memory
  - **report**: writes the live violation log and the final report
- `flight_tester` tests a single flight plan in isolation and prints a JSON `PASS`/`FAIL` result. The Java application uses it for the "Test Flight Plan" feature.

## Main files

| File | Purpose |
|---|---|
| `main.c` | Simulator entry point: loads data, creates IPC objects, forks flights, starts threads |
| `flight_process.c/.h` | Child process loop for a single flight |
| `shared_memory.c/.h` | Shared-memory segment (`sim_shm_t`) and named semaphore helpers |
| `safety_thread.c/.h` | Safety thread and its handoff channel with the coordinator |
| `safety_monitor.c/.h` | Separation checks, violation counting and signalling |
| `aca_filter.c/.h` | Air Control Area boundary checks |
| `environment.c/.h` | Wind drift applied to aircraft positions |
| `weather_service.c/.h` | Wind source: `AISAFE_WIND="speed,dir"` environment variable (calm if unset) |
| `report.c/.h` | Live violation log and final report |
| `config.c/.h` | `simulation.conf` parsing and validation |
| `flight_parser.c/.h` | JSON flight plan parsing (uses the vendored `cJSON.c/.h`) |
| `validation.c/.h` | Parameter, flight plan and coordinate validation |
| `ipc.c/.h` | Flight history helpers |
| `thread_io.c/.h` | Thread-safe output for the parent threads |
| `types.h` | Shared data structures |
| `flight_tester_main.c` | Entry point of `flight_tester` |
| `tests/` | Unit tests and JSON fixtures |

## Build and run

```bash
cd aisafe.base/simulation
make                             # builds flight_simulator and flight_tester
./flight_simulator               # runs every plan in flight_plans.json
./flight_simulator --collision   # collision-test mode
./flight_tester tests/fixtures/valid_single.json
```

Outputs (in the current directory):
- `simulation_report.txt`: final report
- `simulation_violation_log.txt`: violations logged during the run

## Tests

```bash
make test    # builds and runs test_validation, test_flight_tester, test_shared_memory, test_environment
make clean   # removes objects and binaries
```
