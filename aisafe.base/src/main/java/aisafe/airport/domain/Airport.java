package aisafe.airport.domain;

import aisafe.aircontrolarea.domain.AirControlArea;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;


/**
 * Aggregate root representing an airport.
 * An airport is uniquely identified by its IATA code; the ICAO code must also be unique.
 * Every airport belongs to an {@link aisafe.aircontrolarea.domain.AirControlArea}.
 */
@Entity
@Table(name = "T_AIRPORT",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"icaoCode"})
        })
public class Airport implements AggregateRoot<AirportIATACode> {

    @EmbeddedId
    private AirportIATACode iataCode;

    @Version
    private Long version;

    @Embedded
    private AirportICAOCode icaoCode;

    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String town;
    @Column(nullable = false)
    private String country;

    @Embedded
    private GeoCoordinate location;

    private double altitude;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {})
    @JoinColumn(name = "air_control_area_code")
    private AirControlArea airControlArea;

    protected Airport() {}

    /**
     * Creates a new airport.
     *
     * @param iataCode       3-letter IATA identifier (primary key)
     * @param icaoCode       4-letter ICAO identifier (unique)
     * @param name           official airport name
     * @param town           city or town served by the airport
     * @param country        country of the airport
     * @param location       geographic position (latitude / longitude)
     * @param altitude       elevation above sea level in metres
     * @param airControlArea the air control area the airport belongs to
     * @throws IllegalArgumentException if any required field is null or blank
     */
    public Airport(final AirportIATACode iataCode,
                   final AirportICAOCode icaoCode,
                   final String name,
                   final String town,
                   final String country,
                   final GeoCoordinate location,
                   final double altitude,
                   final AirControlArea airControlArea) {

        if (iataCode == null)
            throw new IllegalArgumentException("IATA code cannot be null.");
        if (icaoCode == null)
            throw new IllegalArgumentException("ICAO code cannot be null.");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Airport name cannot be null or empty.");
        if (town == null || town.isBlank())
            throw new IllegalArgumentException("Town cannot be null or empty.");
        if (country == null || country.isBlank())
            throw new IllegalArgumentException("Country cannot be null or empty.");
        if (location == null)
            throw new IllegalArgumentException("Location cannot be null.");
        if (airControlArea == null)
            throw new IllegalArgumentException("Air Control Area cannot be null.");

        this.iataCode = iataCode;
        this.icaoCode = icaoCode;
        this.name = name;
        this.town = town;
        this.country = country;
        this.location = location;
        this.altitude = altitude;
        this.airControlArea = airControlArea;
    }

    /** @return 3-letter IATA code (primary key) */
    public AirportIATACode iataCode() { return iataCode; }

    /** @return 4-letter ICAO code */
    public AirportICAOCode icaoCode() { return icaoCode; }

    /** @return official airport name */
    public String name() { return name; }

    /** @return town or city served by this airport */
    public String town() { return town; }

    /** @return country where the airport is located */
    public String country() { return country; }

    /** @return geographic coordinates (latitude / longitude) */
    public GeoCoordinate location() { return location; }

    /** @return elevation above sea level in metres */
    public double altitude() { return altitude; }

    /** @return the air control area responsible for this airport */
    public AirControlArea airControlArea() { return airControlArea; }

    @Override
    public boolean equals(final Object o) { return DomainEntities.areEqual(this, o); }

    @Override
    public int hashCode() { return DomainEntities.hashCode(this); }

    @Override
    public boolean sameAs(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public AirportIATACode identity() { return this.iataCode; }

    @Override
    public String toString() {
        return String.format("Airport{iata='%s', icao='%s', name='%s', town='%s', country='%s', location=%s}",
                iataCode, icaoCode, name, town, country, location);
    }
}
