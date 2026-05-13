package aisafe.airtransportcompany.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Embeddable;

/**
 * Value object representing a 2-letter IATA airline designator code (e.g. "TP").
 * Used as the primary identifier of an {@link AirTransportCompany}.
 */
@Embeddable
public class IATACode implements ValueObject, Comparable<IATACode> {

    private static final long serialVersionUID = 1L;

    private String code;

    /**
     * Creates a new IATA airline code.
     *
     * @param code exactly 2 uppercase letters (e.g. "TP")
     * @throws IllegalArgumentException if the format is not met
     */
    public IATACode(final String code) {
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("IATA code cannot be empty");
        if (!code.matches("[A-Z]{2}"))
            throw new IllegalArgumentException(
                    "Company IATA code must be exactly 2 uppercase letters: " + code);
        this.code = code;
    }

    protected IATACode() {
        // for ORM
    }

    /**
     * Factory method — equivalent to the constructor.
     *
     * @param code the raw 2-letter string
     * @return a new {@code IATACode} instance
     */
    public static IATACode valueOf(final String code) {
        return new IATACode(code);
    }

    /** @return the 2-letter IATA string */
    public String code() {
        return code;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof IATACode)) return false;
        return code.equals(((IATACode) o).code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }

    @Override
    public String toString() {
        return code;
    }

    @Override
    public int compareTo(final IATACode other) {
        return code.compareTo(other.code);
    }
}
