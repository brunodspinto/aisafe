package aisafe.aircraft.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Embeddable;

/**
 * Value object representing the seat distribution across cabin classes of an aircraft.
 * At least one seat must exist across all classes combined.
 */
@Embeddable
public class CabinConfiguration implements ValueObject {

    private int firstClassSeats;
    private int businessClassSeats;
    private int economyClassSeats;

    /**
     * Creates a cabin configuration with the given seat counts per class.
     *
     * @param firstClassSeats    number of first-class seats (non-negative)
     * @param businessClassSeats number of business-class seats (non-negative)
     * @param economyClassSeats  number of economy-class seats (non-negative)
     * @throws IllegalArgumentException if any count is negative or all are zero
     */
    public CabinConfiguration(final int firstClassSeats, final int businessClassSeats, final int economyClassSeats) {
        if (firstClassSeats < 0)
            throw new IllegalArgumentException("First class seats cannot be negative.");
        if (businessClassSeats < 0)
            throw new IllegalArgumentException("Business class seats cannot be negative.");
        if (economyClassSeats < 0)
            throw new IllegalArgumentException("Economy class seats cannot be negative.");
        if (firstClassSeats + businessClassSeats + economyClassSeats == 0)
            throw new IllegalArgumentException("Cabin must have at least one seat.");
        this.firstClassSeats = firstClassSeats;
        this.businessClassSeats = businessClassSeats;
        this.economyClassSeats = economyClassSeats;
    }

    protected CabinConfiguration() {
        // for ORM
    }

    /** @return number of first-class seats */
    public int firstClassSeats() { return firstClassSeats; }

    /** @return number of business-class seats */
    public int businessClassSeats() { return businessClassSeats; }

    /** @return number of economy-class seats */
    public int economyClassSeats() { return economyClassSeats; }

    /**
     * @return sum of all seat classes
     */
    public int totalSeats() {
        return firstClassSeats + businessClassSeats + economyClassSeats;
    }

    @Override
    public String toString() {
        return String.format("CabinConfiguration{first=%d, business=%d, economy=%d, total=%d}",
                firstClassSeats, businessClassSeats, economyClassSeats, totalSeats());
    }
}
