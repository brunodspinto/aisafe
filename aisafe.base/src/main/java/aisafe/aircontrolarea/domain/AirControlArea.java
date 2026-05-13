package aisafe.aircontrolarea.domain;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.Objects;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;



/**
 * Entity and Aggregate Root representing an Air Control Area.
 */
@Entity
public class AirControlArea implements AggregateRoot<String> {

    // The @Id annotation tells JPA that this is the unique identifier (Primary Key).
    @Id
    private String areaCode;

    private String name;

    private double minimumFuelRequired;

    // GeoBoundary is our embedded Value Object
    @Embedded
    private GeoBoundary boundaries;

    /**
     * Protected constructor required by JPA (ORM).
     */
    protected AirControlArea() {
        // for ORM only
    }

    /**
     * Full constructor to create a valid Air Control Area.
     */
    public AirControlArea(String areaCode, String name, double minimumFuelRequired, GeoBoundary boundaries) {
        if (areaCode == null || areaCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Air Control Area code cannot be null or empty.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Air Control Area name cannot be null or empty.");
        }
        if (minimumFuelRequired < 0) {
            throw new IllegalArgumentException("Minimum fuel required cannot be negative.");
        }
        if (boundaries == null) {
            throw new IllegalArgumentException("Geographical boundaries cannot be null.");
        }

        this.areaCode = areaCode;
        this.name = name;
        this.minimumFuelRequired = minimumFuelRequired;
        this.boundaries = boundaries;
    }

    // --- Getters ---

    public String areaCode() {
        return areaCode;
    }

    public String name() {
        return name;
    }

    public double minimumFuelRequired() {
        return minimumFuelRequired;
    }

    public GeoBoundary boundaries() {
        return boundaries;
    }

    // --- Identity Methods ---
    // The equality of an Entity is defined ONLY by its ID.

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AirControlArea that = (AirControlArea) o;
        return Objects.equals(areaCode, that.areaCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(areaCode);
    }

    @Override
    public boolean sameAs(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public String identity() {
        // Retorna o identificador único desta entidade
        return this.areaCode;
    }

}
