package aisafe.simulation.domain;

/**
 * Lifecycle / terminal status of a {@link Simulation} (Domain Model V10).
 */
public enum SimulationStatus {
    /** Created but not yet started. */
    PENDING,
    /** Currently executing. */
    RUNNING,
    /** Finished normally (all flights completed). */
    COMPLETED,
    /** Aborted (e.g. the safety-violation threshold was reached). */
    FAILED
}
