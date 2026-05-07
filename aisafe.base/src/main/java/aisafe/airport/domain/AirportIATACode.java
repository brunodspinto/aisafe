package aisafe.airport.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Embeddable;

@Embeddable
public class AirportIATACode implements ValueObject, Comparable<AirportIATACode> {

    private static final long serialVersionUID = 1L;
    private String iataCode;

    public AirportIATACode(final String code) {
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("Airport IATA code cannot be empty.");
        if (!code.matches("[A-Z]{3}"))
            throw new IllegalArgumentException(
                    "Airport IATA code must be exactly 3 uppercase letters: " + code);
        this.iataCode = code;
    }

    protected AirportIATACode() {}

    public static AirportIATACode valueOf(final String code) {
        return new AirportIATACode(code);
    }

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