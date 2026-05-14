package aisafe.maker.domain;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Version;

import java.util.Objects;

/**
 * Entity and Aggregate Root representing an aircraft or engine manufacturer.
 */
@Entity
public class Maker implements AggregateRoot<MakerName> {

    @EmbeddedId
    private MakerName name;

    @Version
    private Long version;

    private String country;

    protected Maker() {}

    /**
     * Creates a new maker.
     *
     * @param name    manufacturer name (non-null); used as the unique identifier
     * @param country country of the manufacturer (non-blank)
     * @throws IllegalArgumentException if either value is null or blank
     */
    public Maker(final MakerName name, final String country) {
        if (name == null)
            throw new IllegalArgumentException("Maker name cannot be null or empty.");
        if (country == null || country.isBlank())
            throw new IllegalArgumentException("Country cannot be null or empty.");
        this.name = name;
        this.country = country.trim();
    }

    /** @return unique manufacturer name string (primary key) */
    public String name() { return name.toString(); }

    /** @return country of the manufacturer */
    public String country() { return country; }

    @Override
    public MakerName identity() { return name; }

    @Override
    public boolean sameAs(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(name, ((Maker) o).name);
    }

    @Override
    public int hashCode() { return Objects.hash(name); }

    @Override
    public String toString() {
        return String.format("Maker{name='%s', country='%s'}", name, country);
    }
}
