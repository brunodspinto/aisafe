package aisafe.airport.domain;

import aisafe.aircontrolarea.domain.AirControlArea;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.util.Objects;

@Entity
public class Airport implements AggregateRoot<AirportIATACode> {

    @Id
    @Embedded
    private AirportIATACode iataCode;

    @Embedded
    private AirportICAOCode icaoCode;

    private String name;
    private String town;
    private String country;
    private double latitude;
    private double longitude;
    private double altitude;

    @ManyToOne
    private AirControlArea airControlArea;

    protected Airport() {}

    public Airport(final AirportIATACode iataCode,
                   final AirportICAOCode icaoCode,
                   final String name,
                   final String town,
                   final String country,
                   final double latitude,
                   final double longitude,
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
        if (latitude < -90 || latitude > 90)
            throw new IllegalArgumentException("Latitude must be between -90 and 90.");
        if (longitude < -180 || longitude > 180)
            throw new IllegalArgumentException("Longitude must be between -180 and 180.");
        if (airControlArea == null)
            throw new IllegalArgumentException("Air Control Area cannot be null.");

        this.iataCode = iataCode;
        this.icaoCode = icaoCode;
        this.name = name;
        this.town = town;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.airControlArea = airControlArea;
    }

    public AirportIATACode iataCode() { return iataCode; }
    public AirportICAOCode icaoCode() { return icaoCode; }
    public String name() { return name; }
    public String town() { return town; }
    public String country() { return country; }
    public double latitude() { return latitude; }
    public double longitude() { return longitude; }
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
        return String.format("Airport{iata='%s', icao='%s', name='%s', town='%s', country='%s'}",
                iataCode, icaoCode, name, town, country);
    }
}