package aisafe.flightroute.repositories;

import aisafe.airtransportcompany.domain.IATACode;
import aisafe.flightroute.domain.FlightRoute;
import aisafe.flightroute.domain.RouteName;
import eapli.framework.domain.repositories.DomainRepository;

/**
 * Repository interface for {@link FlightRoute} aggregate roots, keyed by {@link RouteName}.
 */
public interface FlightRouteRepository extends DomainRepository<RouteName, FlightRoute> {

    /**
     * Checks whether a flight route with the given name already exists.
     *
     * @param routeName the route name to check
     * @return {@code true} if a route with that name exists
     */
    boolean existsByName(RouteName routeName);

    /**
     * Returns all flight routes operated by the given company.
     *
     * @param companyIataCode the IATA code of the company
     * @return all routes for that company
     */
    Iterable<FlightRoute> findByCompany(IATACode companyIataCode);
}