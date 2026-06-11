package aisafe.simulation.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable carrier of the raw outcome of a simulation run, as read from the simulation output
 * (e.g. parsed from {@code simulation_report.txt}). It is the input the {@link Simulation}
 * aggregate uses to build a {@link SimulationReport}.
 */
public final class SimulationResults {

    private final SimulationStatus status;
    private final int totalFlights;
    private final List<FlightExecutionStatus> executionStatuses;
    private final List<SafetyViolation> safetyViolations;

    /**
     * @param status            the terminal status of the run ({@code COMPLETED} or {@code FAILED})
     * @param totalFlights      number of flights in the run (≥ 0)
     * @param executionStatuses one entry per flight (non-null)
     * @param safetyViolations  zero or more violations (non-null)
     * @throws IllegalArgumentException if any constraint is violated
     */
    public SimulationResults(final SimulationStatus status, final int totalFlights,
                             final List<FlightExecutionStatus> executionStatuses,
                             final List<SafetyViolation> safetyViolations) {
        if (status == null) {
            throw new IllegalArgumentException("Simulation status cannot be null.");
        }
        if (totalFlights < 0) {
            throw new IllegalArgumentException("Total flights cannot be negative.");
        }
        if (executionStatuses == null) {
            throw new IllegalArgumentException("Execution statuses cannot be null.");
        }
        if (safetyViolations == null) {
            throw new IllegalArgumentException("Safety violations cannot be null.");
        }
        this.status = status;
        this.totalFlights = totalFlights;
        this.executionStatuses = Collections.unmodifiableList(new ArrayList<>(executionStatuses));
        this.safetyViolations = Collections.unmodifiableList(new ArrayList<>(safetyViolations));
    }

    public SimulationStatus status() { return status; }

    public int totalFlights() { return totalFlights; }

    public List<FlightExecutionStatus> executionStatuses() { return executionStatuses; }

    public List<SafetyViolation> safetyViolations() { return safetyViolations; }
}
