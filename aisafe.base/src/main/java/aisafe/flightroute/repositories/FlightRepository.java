package aisafe.flightroute.repositories;

import aisafe.flightroute.domain.FlightRoute;

import java.time.LocalDate;

/**
 * Repository interface for querying planned flights against a {@link FlightRoute}.
 * Used by the Deactivate Flight Route use case (US074) to enforce AC074.3.
 */
public interface FlightRepository {

    /**
     * Returns {@code true} if there are any planned flights on the given route
     * with a departure date on or after the specified deactivation date.
     *
     * @param route            the flight route to check
     * @param deactivationDate the boundary date (inclusive)
     * @return {@code true} if conflicting planned flights exist
     */
    boolean hasFlightsAfter(FlightRoute route, LocalDate deactivationDate);
}
