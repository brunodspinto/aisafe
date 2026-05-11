package aisafe.airport.domain;

import aisafe.aircontrolarea.domain.AirControlArea;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Objects;

@Entity
@Table(name = "T_AIRPORT",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"icaoCode"})
        })
public class Airport implements AggregateRoot<AirportIATACode> {

    @Id
    @Embedded
    private AirportIATACode iataCode;

    @Embedded
    private AirportICAOCode icaoCode;

    private String name;
    private String town;
    private String country;

    @Embedded
    private GeoCoordinate location;

    private double altitude;

    @ManyToOne
    private AirControlArea airControlArea;

    protected Airport() {}

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

    public AirportIATACode iataCode() { return iataCode; }
    public AirportICAOCode icaoCode() { return icaoCode; }
    public String name() { return name; }
    public String town() { return town; }
    public String country() { return country; }
    public GeoCoordinate location() { return location; }
    public double altitude() { return altitude; }
    public AirControlArea airControlArea() { return airControlArea; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(iataCode, ((Airport) o).iataCode);
    }

    @Override
    public int hashCode() { return Objects.hash(iataCode); }

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
