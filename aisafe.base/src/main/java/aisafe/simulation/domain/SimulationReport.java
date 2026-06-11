package aisafe.simulation.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The report produced by a {@link Simulation} (Domain Model V10). It summarises a simulation run:
 * the total number of flights and their execution statuses (AC111.2), the safety violations with
 * timestamps and positions (AC111.3), and the overall pass/fail result (AC111.4).
 */
public final class SimulationReport {

    private final int totalFlights;
    private final boolean passed;
    private final LocalDateTime generatedAt;
    private final List<FlightExecutionStatus> executionStatuses;
    private final List<SafetyViolation> safetyViolations;

    /**
     * @param totalFlights      total number of flights in the run (≥ 0)
     * @param passed            whether the simulated flight plan passed validation
     * @param generatedAt       when the report was generated (non-null)
     * @param executionStatuses one entry per flight (non-null)
     * @param safetyViolations  zero or more violations (non-null)
     * @throws IllegalArgumentException if any constraint is violated
     */
    public SimulationReport(final int totalFlights, final boolean passed,
                            final LocalDateTime generatedAt,
                            final List<FlightExecutionStatus> executionStatuses,
                            final List<SafetyViolation> safetyViolations) {
        if (totalFlights < 0) {
            throw new IllegalArgumentException("Total flights cannot be negative.");
        }
        if (generatedAt == null) {
            throw new IllegalArgumentException("Generated-at timestamp cannot be null.");
        }
        if (executionStatuses == null) {
            throw new IllegalArgumentException("Execution statuses cannot be null.");
        }
        if (safetyViolations == null) {
            throw new IllegalArgumentException("Safety violations cannot be null.");
        }
        this.totalFlights = totalFlights;
        this.passed = passed;
        this.generatedAt = generatedAt;
        this.executionStatuses = Collections.unmodifiableList(new ArrayList<>(executionStatuses));
        this.safetyViolations = Collections.unmodifiableList(new ArrayList<>(safetyViolations));
    }

    /** @return total number of flights (AC111.2) */
    public int totalFlights() { return totalFlights; }

    /** @return {@code true} if the simulated flight plan passed validation (AC111.4) */
    public boolean passed() { return passed; }

    /** @return when the report was generated */
    public LocalDateTime generatedAt() { return generatedAt; }

    /** @return one execution status per flight (AC111.2) */
    public List<FlightExecutionStatus> executionStatuses() { return executionStatuses; }

    /** @return the safety violations with timestamps and positions (AC111.3) */
    public List<SafetyViolation> safetyViolations() { return safetyViolations; }
}
