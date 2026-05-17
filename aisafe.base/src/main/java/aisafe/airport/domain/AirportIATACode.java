package aisafe.airport.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Embeddable;

/**
 * Value object representing a 3-letter IATA airport code (e.g. "LIS").
 * Used as the primary identifier of an {@link Airport}.
 */
@Embeddable
public class AirportIATACode implements ValueObject, Comparable<AirportIATACode> {

    private String iataCode;

    /**
     * Creates a new IATA code.
     *
     * @param code exactly 3 uppercase letters (e.g. "LIS")
     * @throws IllegalArgumentException if the format is not met
     */
    public AirportIATACode(final String code) {
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("Airport IATA code cannot be empty.");
        if (!code.matches("[A-Z]{3}"))
            throw new IllegalArgumentException(
                    "Airport IATA code must be exactly 3 uppercase letters: " + code);
        this.iataCode = code;
    }

    protected AirportIATACode() {}

    /**
     * Factory method — equivalent to the constructor.
     *
     * @param code the raw IATA string
     * @return a new {@code AirportIATACode} instance
     */
    public static AirportIATACode valueOf(final String code) {
        return new AirportIATACode(code);
    }

    /** @return the 3-letter IATA string */
    public String code() { return iataCode; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof AirportIATACode)) return false;
        return iataCode.equals(((AirportIATACode) o).iataCode);
    }

    @Override
    public int hashCode() { return iataCode.hashCode(); }

    @Override
    public String toString() { return iataCode; }

    @Override
    public int compareTo(final AirportIATACode other) {
        return iataCode.compareTo(other.iataCode);
    }
}