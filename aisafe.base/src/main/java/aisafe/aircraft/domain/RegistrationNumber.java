package aisafe.aircraft.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Value object representing an aircraft registration number (e.g. "CS-TUA").
 * Used as the primary identifier of an {@link Aircraft}.
 */
@Embeddable
public class RegistrationNumber implements ValueObject, Comparable<RegistrationNumber> {

    @Column(name = "registration_number")
    private String value;

    protected RegistrationNumber() {
        // for ORM
    }

    public RegistrationNumber(final String value) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("Registration number cannot be blank.");
        this.value = value.trim().toUpperCase();
    }

    public static RegistrationNumber valueOf(final String value) {
        return new RegistrationNumber(value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof RegistrationNumber)) return false;
        return value.equals(((RegistrationNumber) o).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public int compareTo(final RegistrationNumber other) {
        return value.compareTo(other.value);
    }
}
