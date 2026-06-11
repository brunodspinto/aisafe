package aisafe.simulation.domain;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;

import java.time.LocalDateTime;

/**
 * Aggregate root representing a flight simulation run (Domain Model V10), identified by a
 * {@link SimulationId}. For US111 the aggregate's responsibility is to <b>build its own
 * {@link SimulationReport}</b> from the simulation results (Information Expert): it owns the
 * report and its terminal {@link SimulationStatus}.
 *
 * <p>Note: the full V10 simulation parameters (time range, time step, safety threshold) belong to
 * the simulation setup (US100) and are not required to generate the report, so they are omitted
 * from this minimal aggregate.
 */
public class Simulation implements AggregateRoot<SimulationId> {

    private final SimulationId id;
    private SimulationStatus status;
    private SimulationReport report;

    /**
     * Creates a simulation in {@link SimulationStatus#PENDING} status.
     *
     * @param id the simulation identity (non-null)
     * @throws IllegalArgumentException if {@code id} is null
     */
    public Simulation(final SimulationId id) {
        if (id == null) {
            throw new IllegalArgumentException("Simulation id cannot be null.");
        }
        this.id = id;
        this.status = SimulationStatus.PENDING;
    }

    /**
     * Builds the {@link SimulationReport} for this simulation from the run's results, sets the
     * simulation's terminal status, and stores the report.
     *
     * <p>The validation result is derived exactly as the simulation does: the run passes iff it
     * {@link SimulationStatus#COMPLETED completed} normally and had zero safety violations.
     *
     * @param results the parsed simulation results (non-null)
     * @return the produced {@link SimulationReport}
     * @throws IllegalArgumentException if {@code results} is null
     */
    public SimulationReport buildReport(final SimulationResults results) {
        if (results == null) {
            throw new IllegalArgumentException("Simulation results cannot be null.");
        }
        this.status = results.status();
        final boolean passed = results.status() == SimulationStatus.COMPLETED
                && results.safetyViolations().isEmpty();
        this.report = new SimulationReport(
                results.totalFlights(), passed, LocalDateTime.now(),
                results.executionStatuses(), results.safetyViolations());
        return this.report;
    }

    /** @return the current/terminal status */
    public SimulationStatus status() {
        return status;
    }

    /** @return the produced report, or {@code null} if {@link #buildReport} has not been called */
    public SimulationReport report() {
        return report;
    }

    @Override
    public SimulationId identity() {
        return id;
    }

    @Override
    public boolean sameAs(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public boolean equals(final Object o) {
        return DomainEntities.areEqual(this, o);
    }

    @Override
    public int hashCode() {
        return DomainEntities.hashCode(this);
    }

    @Override
    public String toString() {
        return String.format("Simulation{id='%s', status=%s}", id, status);
    }
}
