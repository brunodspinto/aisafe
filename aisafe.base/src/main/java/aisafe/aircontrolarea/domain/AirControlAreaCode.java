package aisafe.aircontrolarea.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Value object representing an air control area code (e.g. "PT-N").
 * Used as the primary identifier of an {@link AirControlArea}.
 */
@Embeddable
public class AirControlAreaCode implements ValueObject, Comparable<AirControlAreaCode> {

    @Column(name = "area_code")
    private String value;

    protected AirControlAreaCode() {
        // for ORM
    }

    public AirControlAreaCode(final String value) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("Air Control Area code cannot be null or empty.");
        this.value = value.trim().toUpperCase();
    }

    public static AirControlAreaCode valueOf(final String value) {
        return new AirControlAreaCode(value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof AirControlAreaCode)) return false;
        return value.equals(((AirControlAreaCode) o).value);
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
    public int compareTo(final AirControlAreaCode other) {
        return value.compareTo(other.value);
    }
}
