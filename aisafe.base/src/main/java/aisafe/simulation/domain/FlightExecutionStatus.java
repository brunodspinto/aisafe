package aisafe.simulation.domain;

import eapli.framework.domain.model.ValueObject;

import java.util.Objects;

/**
 * Value object describing the execution status of one flight in a simulation
 * (e.g. {@code FLIGHT_01} → {@code COMPLETED}). Supports AC111.2.
 */
public final class FlightExecutionStatus implements ValueObject {

    private final String flightDesignator;
    private final String status;

    /**
     * @param flightDesignator the flight identifier (non-blank)
     * @param status           the execution status (non-blank, e.g. {@code COMPLETED}/{@code STOPPED})
     * @throws IllegalArgumentException if any argument is null or blank
     */
    public FlightExecutionStatus(final String flightDesignator, final String status) {
        if (flightDesignator == null || flightDesignator.isBlank()) {
            throw new IllegalArgumentException("Flight designator cannot be null or blank.");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Execution status cannot be null or blank.");
        }
        this.flightDesignator = flightDesignator.trim();
        this.status = status.trim();
    }

    /** @return the flight identifier */
    public String flightDesignator() {
        return flightDesignator;
    }

    /** @return the execution status */
    public String status() {
        return status;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof FlightExecutionStatus other)) return false;
        return flightDesignator.equals(other.flightDesignator) && status.equals(other.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flightDesignator, status);
    }

    @Override
    public String toString() {
        return flightDesignator + " : " + status;
    }
}
