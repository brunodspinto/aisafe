package aisafe.aircraft.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Embeddable;

@Embeddable
public class CabinConfiguration implements ValueObject {

    private int firstClassSeats;
    private int businessClassSeats;
    private int economyClassSeats;

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

    public int firstClassSeats() { return firstClassSeats; }
    public int businessClassSeats() { return businessClassSeats; }
    public int economyClassSeats() { return economyClassSeats; }

    public int totalSeats() {
        return firstClassSeats + businessClassSeats + economyClassSeats;
    }

    @Override
    public String toString() {
        return String.format("CabinConfiguration{first=%d, business=%d, economy=%d, total=%d}",
                firstClassSeats, businessClassSeats, economyClassSeats, totalSeats());
    }
}
