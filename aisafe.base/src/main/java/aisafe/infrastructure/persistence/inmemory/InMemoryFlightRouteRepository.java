package aisafe.infrastructure.persistence.inmemory;

import aisafe.airtransportcompany.domain.IATACode;
import aisafe.flightroute.domain.FlightRoute;
import aisafe.flightroute.domain.RouteName;
import aisafe.flightroute.repositories.FlightRouteRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

/**
 * In-memory implementation of {@link FlightRouteRepository} for development and testing.
 */
public class InMemoryFlightRouteRepository
        extends InMemoryDomainRepository<FlightRoute, RouteName>
        implements FlightRouteRepository {

    @Override
    public boolean existsByName(final RouteName routeName) {
        return matchOne(r -> r.routeName().equals(routeName)).isPresent();
    }

    @Override
    public Iterable<FlightRoute> findByCompany(final IATACode companyIataCode) {
        return match(r -> r.companyIataCode().equals(companyIataCode));
    }

    @Override
    public Iterable<FlightRoute> findActiveByCompany(final IATACode companyIataCode) {
        return match(r -> r.companyIataCode().equals(companyIataCode) && r.isActive());
    }
}