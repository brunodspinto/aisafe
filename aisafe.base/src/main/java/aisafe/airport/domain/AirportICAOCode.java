package aisafe.airport.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Embeddable;

/**
 * Value object representing a 4-letter ICAO airport code (e.g. "LPPT").
 */
@Embeddable
public class AirportICAOCode implements ValueObject, Comparable<AirportICAOCode> {

    private String icaoCode;

    /**
     * Creates a new ICAO code.
     *
     * @param code exactly 4 uppercase letters (e.g. "LPPT")
     * @throws IllegalArgumentException if the format is not met
     */
    public AirportICAOCode(final String code) {
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("Airport ICAO code cannot be empty.");
        if (!code.matches("[A-Z]{4}"))
            throw new IllegalArgumentException(
                    "Airport ICAO code must be exactly 4 uppercase letters: " + code);
        this.icaoCode = code;
    }

    protected AirportICAOCode() {}

    /**
     * Factory method — equivalent to the constructor.
     *
     * @param code the raw ICAO string
     * @return a new {@code AirportICAOCode} instance
     */
    public static AirportICAOCode valueOf(final String code) {
        return new AirportICAOCode(code);
    }

    /** @return the 4-letter ICAO string */
    public String code() { return icaoCode; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof AirportICAOCode)) return false;
        return icaoCode.equals(((AirportICAOCode) o).icaoCode);
    }

    @Override
    public int hashCode() { return icaoCode.hashCode(); }

    @Override
    public String toString() { return icaoCode; }

    @Override
    public int compareTo(final AirportICAOCode other) {
        return icaoCode.compareTo(other.icaoCode);
    }
}