package aisafe.aircontrolarea.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;



/**
 * Entity and Aggregate Root representing an Air Control Area.
 */
@Entity
@Table(name = "T_AIR_CONTROL_AREA")
public class AirControlArea implements AggregateRoot<AirControlAreaCode> {

    @EmbeddedId
    private AirControlAreaCode areaCode;

    @Version
    private Long version;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
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
     * Creates a valid air control area.
     *
     * @param areaCode             unique area code (non-null)
     * @param name                 descriptive name (non-blank)
     * @param minimumFuelRequired  minimum fuel required for operations in this area (non-negative)
     * @param boundaries           geographic bounding box (non-null)
     * @throws IllegalArgumentException if any constraint is violated
     */
    public AirControlArea(final AirControlAreaCode areaCode, final String name,
                          final double minimumFuelRequired, final GeoBoundary boundaries) {
        if (areaCode == null) {
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
        if (boundaries.northLatitude() <= boundaries.southLatitude()) {
            throw new IllegalArgumentException("North latitude must be greater than south latitude.");
        }

        this.areaCode = areaCode;
        this.name = name;
        this.minimumFuelRequired = minimumFuelRequired;
        this.boundaries = boundaries;
    }

    /** @return unique area code string (primary key) */
    public String areaCode() {
        return areaCode.toString();
    }

    /** @return descriptive name of this area */
    public String name() {
        return name;
    }

    /** @return minimum fuel required for operations in this area */
    public double minimumFuelRequired() {
        return minimumFuelRequired;
    }

    /** @return geographic bounding box of this area */
    public GeoBoundary boundaries() {
        return boundaries;
    }

    @Override
    public boolean equals(final Object o) { return DomainEntities.areEqual(this, o); }

    @Override
    public int hashCode() { return DomainEntities.hashCode(this); }

    @Override
    public boolean sameAs(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public AirControlAreaCode identity() {
        return this.areaCode;
    }

    @Override
    public String toString() {
        return String.format("AirControlArea{code='%s', name='%s'}", areaCode, name);
    }

}
