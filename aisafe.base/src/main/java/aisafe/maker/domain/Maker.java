package aisafe.maker.domain;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.Objects;

/**
 * Entity and Aggregate Root representing an aircraft or engine manufacturer.
 */
@Entity
public class Maker implements AggregateRoot<String> {

    @Id
    private String name;

    private String country;

    protected Maker() {}

    public Maker(final String name, final String country) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Maker name cannot be null or empty.");
        if (country == null || country.isBlank())
            throw new IllegalArgumentException("Country cannot be null or empty.");
        this.name = name.trim();
        this.country = country.trim();
    }

    public String name() { return name; }
    public String country() { return country; }

    @Override
    public String identity() { return name; }

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
