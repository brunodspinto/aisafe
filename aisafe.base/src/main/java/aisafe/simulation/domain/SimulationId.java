package aisafe.simulation.domain;

import eapli.framework.domain.model.ValueObject;

/**
 * Value object identifying a {@link Simulation} (business identity of the aggregate).
 */
public final class SimulationId implements ValueObject, Comparable<SimulationId> {

    private final String value;

    /**
     * @param value a non-blank identifier (e.g. {@code "SIM-20260604-1"})
     * @throws IllegalArgumentException if {@code value} is null or blank
     */
    public SimulationId(final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Simulation id cannot be null or blank.");
        }
        this.value = value.trim();
    }

    /**
     * Factory method — equivalent to the constructor.
     *
     * @param value the raw identifier string
     * @return a new {@code SimulationId}
     */
    public static SimulationId valueOf(final String value) {
        return new SimulationId(value);
    }

    /** @return the raw identifier string */
    public String value() {
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof SimulationId)) return false;
        return value.equals(((SimulationId) o).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public int compareTo(final SimulationId other) {
        return value.compareTo(other.value);
    }
}
