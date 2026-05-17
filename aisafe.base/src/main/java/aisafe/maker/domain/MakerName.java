package aisafe.maker.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Value object representing a manufacturer name (e.g. "Boeing").
 * Used as the primary identifier of a {@link Maker}.
 */
@Embeddable
public class MakerName implements ValueObject, Comparable<MakerName> {

    @Column(name = "name")
    private String value;

    protected MakerName() {
        // for ORM
    }

    public MakerName(final String value) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("Maker name cannot be null or empty.");
        this.value = value.trim();
    }

    public static MakerName valueOf(final String value) {
        return new MakerName(value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof MakerName)) return false;
        return value.equals(((MakerName) o).value);
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
    public int compareTo(final MakerName other) {
        return value.compareTo(other.value);
    }
}
