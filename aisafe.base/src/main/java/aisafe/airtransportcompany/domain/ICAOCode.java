package aisafe.airtransportcompany.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Embeddable;

@Embeddable
public class ICAOCode implements ValueObject {

    private static final long serialVersionUID = 1L;

    private String icaoCode;

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

    public static ICAOCode valueOf(final String code) {
        return new ICAOCode(code);
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
