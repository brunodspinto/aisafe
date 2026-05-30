package aisafe.flightroute.domain;

import aisafe.airtransportcompany.domain.IATACode;
import aisafe.airport.domain.AirportIATACode;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.LocalDate;

/**
 * Aggregate root representing a named flight route between two airports.
 * A route is operated by an air transport company and identified by its unique {@link RouteName}.
 * A route is always created in {@link FlightRouteStatus#ACTIVE} status.
 */
@Entity
@Table(name = "T_FLIGHT_ROUTE",
        uniqueConstraints = @UniqueConstraint(columnNames = {"route_name"}))
public class FlightRoute implements AggregateRoot<RouteName> {

    @EmbeddedId
    @AttributeOverride(name = "name", column = @Column(name = "route_name", nullable = false))
    private RouteName routeName;

    @Version
    private Long version;

    @Embedded
    @AttributeOverride(name = "code", column = @Column(name = "company_iata_code", nullable = false))
    private IATACode companyIataCode;

    @Embedded
    @AttributeOverride(name = "iataCode", column = @Column(name = "origin_airport_code", nullable = false))
    private AirportIATACode originAirport;

    @Embedded
    @AttributeOverride(name = "iataCode", column = @Column(name = "destination_airport_code", nullable = false))
    private AirportIATACode destinationAirport;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlightRouteStatus status;

    private LocalDate activeUntil;

    /**
     * Creates a new active flight route.
     *
     * @param routeName          unique route name (non-null, format [A-Z]{2}[0-9]{1,4})
     * @param originAirport      IATA code of the origin airport (non-null)
     * @param destinationAirport IATA code of the destination airport (non-null, different from origin)
     * @param companyIataCode    IATA code of the operating company (non-null)
     * @throws IllegalArgumentException if any constraint is violated
     */
    public FlightRoute(final RouteName routeName,
                       final AirportIATACode originAirport,
                       final AirportIATACode destinationAirport,
                       final IATACode companyIataCode) {
        if (routeName == null)
            throw new IllegalArgumentException("Route name cannot be null.");
        if (originAirport == null)
            throw new IllegalArgumentException("Origin airport cannot be null.");
        if (destinationAirport == null)
            throw new IllegalArgumentException("Destination airport cannot be null.");
        if (companyIataCode == null)
            throw new IllegalArgumentException("Company IATA code cannot be null.");
        if (originAirport.equals(destinationAirport))
            throw new IllegalArgumentException("Origin and destination airports must be different.");

        this.routeName = routeName;
        this.originAirport = originAirport;
        this.destinationAirport = destinationAirport;
        this.companyIataCode = companyIataCode;
        this.status = FlightRouteStatus.ACTIVE;
        this.activeUntil = null;
    }

    /** For JPA. */
    protected FlightRoute() {
        // for ORM
    }

    /** @return the route name (primary identifier) */
    public RouteName routeName() { return routeName; }

    /** @return the IATA code of the origin airport */
    public AirportIATACode originAirport() { return originAirport; }

    /** @return the IATA code of the destination airport */
    public AirportIATACode destinationAirport() { return destinationAirport; }

    /** @return the IATA code of the operating company */
    public IATACode companyIataCode() { return companyIataCode; }

    /** @return the current operational status */
    public FlightRouteStatus status() { return status; }

    /** @return the date until which the route is active, or null if no end date is set */
    public LocalDate activeUntil() { return activeUntil; }

    /** @return {@code true} if the route is currently active */
    public boolean isActive() { return status == FlightRouteStatus.ACTIVE; }

    @Override
    public RouteName identity() { return routeName; }

    @Override
    public boolean sameAs(final Object other) { return DomainEntities.areEqual(this, other); }

    @Override
    public boolean equals(final Object o) { return DomainEntities.areEqual(this, o); }

    @Override
    public int hashCode() { return DomainEntities.hashCode(this); }

    @Override
    public String toString() {
        return String.format("FlightRoute{name='%s', origin='%s', destination='%s', company='%s', status=%s}",
                routeName, originAirport, destinationAirport, companyIataCode, status);
    }
}