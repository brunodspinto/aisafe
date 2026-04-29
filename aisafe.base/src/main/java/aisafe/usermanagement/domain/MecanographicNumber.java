package aisafe.usermanagement.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Embeddable;

@Embeddable
public class MecanographicNumber implements ValueObject, Comparable<MecanographicNumber> {

    private static final long serialVersionUID = 1L;

    private String number;

    public MecanographicNumber(final String mecanographicNumber) {
        if (mecanographicNumber == null || mecanographicNumber.isBlank())
            throw new IllegalArgumentException("Mecanographic number cannot be null or empty");
        this.number = mecanographicNumber;
    }

    protected MecanographicNumber() {
        // for ORM
    }

    public static MecanographicNumber valueOf(final String mecanographicNumber) {
        return new MecanographicNumber(mecanographicNumber);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof MecanographicNumber)) return false;
        final MecanographicNumber that = (MecanographicNumber) o;
        return this.number.equals(that.number);
    }

    @Override
    public int hashCode() {
        return this.number.hashCode();
    }

    @Override
    public String toString() {
        return this.number;
    }

    @Override
    public int compareTo(final MecanographicNumber other) {
        return number.compareTo(other.number);
    }
}
