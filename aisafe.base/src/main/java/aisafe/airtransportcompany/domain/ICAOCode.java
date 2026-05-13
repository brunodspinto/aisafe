package aisafe.airtransportcompany.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Embeddable;

/**
 * Value object representing a 2–3 letter ICAO airline designator code (e.g. "TAP").
 */
@Embeddable
public class ICAOCode implements ValueObject {

    private static final long serialVersionUID = 1L;

    private String icaoCode;

    /**
     * Creates a new ICAO airline code.
     *
     * @param code 2 or 3 uppercase letters (e.g. "TAP")
     * @throws IllegalArgumentException if the format is not met
     */
    public ICAOCode(final String code) {
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("ICAO code cannot be empty");
        if (!code.matches("[A-Z]{2,3}"))
            throw new IllegalArgumentException(
                    "Company ICAO code must be 2 or 3 uppercase letters: " + code);
        this.icaoCode = code;
    }

    protected ICAOCode() {
        // for ORM
    }

    /**
     * Factory method — equivalent to the constructor.
     *
     * @param code the raw 2–3 letter string
     * @return a new {@code ICAOCode} instance
     */
    public static ICAOCode valueOf(final String code) {
        return new ICAOCode(code);
    }

    /** @return the 2–3-letter ICAO string */
    public String code() {
        return icaoCode;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof ICAOCode)) return false;
        return icaoCode.equals(((ICAOCode) o).icaoCode);
    }

    @Override
    public int hashCode() {
        return icaoCode.hashCode();
    }

    @Override
    public String toString() {
        return icaoCode;
    }
}
