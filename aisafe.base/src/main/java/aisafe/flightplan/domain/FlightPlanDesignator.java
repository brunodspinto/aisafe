package aisafe.flightplan.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Value object representing a flight plan designator (e.g. "TP1234").
 * Used as the primary identifier of a {@link FlightPlan}.
 */
@Embeddable
public class FlightPlanDesignator implements ValueObject, Comparable<FlightPlanDesignator> {

    @Column(name = "designator")
    private String value;

    protected FlightPlanDesignator() {
        // for ORM
    }

    public FlightPlanDesignator(final String value) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("Flight plan designator cannot be null or empty.");
        this.value = value.trim().toUpperCase();
    }

    public static FlightPlanDesignator valueOf(final String value) {
        return new FlightPlanDesignator(value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof FlightPlanDesignator)) return false;
        return value.equals(((FlightPlanDesignator) o).value);
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
    public int compareTo(final FlightPlanDesignator other) {
        return value.compareTo(other.value);
    }
}
