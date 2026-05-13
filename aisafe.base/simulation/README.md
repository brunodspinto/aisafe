# Simulation

This folder contains the C simulation module for the project.

## What it does

The simulator runs a sample flight, sends position updates through a pipe, and prints the positions in the parent process.

## Main files

- `main.c` - starts the simulation
- `flight_process.c` - runs the flight in the child process
- `ipc.c` / `ipc.h` - pipe helpers
- `types.h` - simulation data structures
- `flight_data.h` - sample flight data
- `Makefile` - build, test, run, and clean targets

## Build and run

```bash
cd aisafe.base/simulation
make
make test
make run
```

## Notes

- `make test` runs the validation tests.
- `make clean` removes generated objects and binaries.
