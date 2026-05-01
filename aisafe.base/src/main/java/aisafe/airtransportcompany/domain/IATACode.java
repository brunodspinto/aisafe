package aisafe.airtransportcompany.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Embeddable;

@Embeddable
public class IATACode implements ValueObject, Comparable<IATACode> {

    private static final long serialVersionUID = 1L;

    private String code;

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

    public static IATACode valueOf(final String code) {
        return new IATACode(code);
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
