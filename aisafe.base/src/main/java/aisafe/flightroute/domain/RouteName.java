package aisafe.flightroute.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Embeddable;

/**
 * Value object and primary identifier for a {@link FlightRoute}.
 * A route name consists of exactly 2 uppercase letters followed by 1 to 4 digits (e.g. TP123).
 */
@Embeddable
public class RouteName implements ValueObject, Comparable<RouteName> {

    private String name;

    /**
     * Creates a new route name.
     *
     * @param name the route name string — must match [A-Z]{2}[0-9]{1,4}
     * @throws IllegalArgumentException if the format is not met
     */
    public RouteName(final String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Route name cannot be null or blank.");
        if (!name.matches("[A-Z]{2}[0-9]{1,4}"))
            throw new IllegalArgumentException(
                    "Route name must follow the format [A-Z]{2}[0-9]{1,4} (e.g. TP123): " + name);
        this.name = name;
    }

    /** For JPA. */
    protected RouteName() {
        // for ORM
    }

    /** @return the route name string */
    public String name() {
        return name;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof RouteName)) return false;
        return name.equals(((RouteName) o).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(final RouteName other) {
        return name.compareTo(other.name);
    }
}