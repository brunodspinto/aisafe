# US100 - Simulate Flights in a Given Area

## Context

- weather conditions
- safety thresholds

All required parameters should be validated.

## Acceptance Criteria

- The component must be implemented in C and must utilize processes, pipes, and signals.
- The system should fork a new process for each flight.
- Each flight process should execute its designated flight plan.
- Pipes should facilitate communication between the main process and each flight process.
- All required parameters should be validated.

## Implementation

**Location:** `aisafe.base/simulation/`

**Files:**

- `main.c` - starts and coordinates the simulation
- `flight_process.c` - runs the flight in the child process
- `ipc.c/h` - pipe communication
- `types.h` - simulation data structures
- `flight_data.h` - sample flight data
- `validation.c/h` - parameter validation
- `Makefile` - build, test, run, and clean targets

## How It Works

1. The parent process creates a pipe.
2. The parent forks a child process for the flight.
3. The child executes the flight plan step by step.
4. The child sends each position through the pipe.
5. The simulation ends after receiving the position updates.

The current implementation uses a sample flight from Porto to Lisbon.

## Build and Run

```bash
cd aisafe.base/simulation
make
make test
make run
```

## Current Status

| Feature | Status | Notes |
|---------|--------|-------|
| Single flight process | Done | Runs the sample flight |
| Pipe communication | Done | Parent and child exchange positions |
| Flight plan validation | Done | Validation tests are available |
| Multiple flights | Not implemented | Current version simulates one flight |
| Signal handling | Not implemented | Listed in the requirements, not yet implemented |
| Weather effects | Not implemented | Structures exist for future use |

## Notes

`make test` runs the validation tests, and `make clean` removes generated objects and binaries.


