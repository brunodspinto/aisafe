package aisafe.airport.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Embeddable;

@Embeddable
public class AirportICAOCode implements ValueObject {

    private static final long serialVersionUID = 1L;
    private String icaoCode;

    public AirportICAOCode(final String code) {
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("Airport ICAO code cannot be empty.");
        if (!code.matches("[A-Z]{4}"))
            throw new IllegalArgumentException(
                    "Airport ICAO code must be exactly 4 uppercase letters: " + code);
        this.icaoCode = code;
    }

    protected AirportICAOCode() {}

    public static AirportICAOCode valueOf(final String code) {
        return new AirportICAOCode(code);
    }

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
}