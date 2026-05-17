package aisafe.usermanagement.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Embeddable;

/**
 * Value object and primary identifier for a {@link User}.
 * Represents a unique mecanographic (employee) number.
 */
@Embeddable
public class MecanographicNumber implements ValueObject, Comparable<MecanographicNumber> {

    private String number;

    /**
     * Creates a new mecanographic number.
     *
     * @param mecanographicNumber the raw number string (must not be blank)
     * @throws IllegalArgumentException if the value is null or blank
     */
    public MecanographicNumber(final String mecanographicNumber) {
        if (mecanographicNumber == null || mecanographicNumber.isBlank())
            throw new IllegalArgumentException("Mecanographic number cannot be null or empty");
        this.number = mecanographicNumber;
    }

    /** For JPA. */
    protected MecanographicNumber() {
        // for ORM
    }

    /**
     * Factory method equivalent to the constructor.
     *
     * @param mecanographicNumber the raw number string
     * @return a new {@code MecanographicNumber} instance
     */
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
