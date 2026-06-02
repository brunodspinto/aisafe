package aisafe.infrastructure.persistence.jpa;

import aisafe.airtransportcompany.domain.IATACode;
import aisafe.flightroute.domain.FlightRoute;
import aisafe.flightroute.domain.RouteName;
import aisafe.flightroute.repositories.FlightRouteRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

import java.util.HashMap;
import java.util.Map;

/**
 * JPA implementation of {@link FlightRouteRepository}.
 */
public class JpaFlightRouteRepository
        extends JpaAutoTxRepository<FlightRoute, RouteName, RouteName>
        implements FlightRouteRepository {

    public JpaFlightRouteRepository(final String puName) {
        super(puName, "routeName");
    }

    public JpaFlightRouteRepository(final TransactionalContext tx) {
        super(tx, "routeName");
    }

    @Override
    public boolean existsByName(final RouteName routeName) {
        final Map<String, Object> params = new HashMap<>();
        params.put("name", routeName.toString());
        return matchOne("e.routeName.name = :name", params).isPresent();
    }

    @Override
    public Iterable<FlightRoute> findByCompany(final IATACode companyIataCode) {
        final Map<String, Object> params = new HashMap<>();
        params.put("code", companyIataCode.toString());
        return match("e.companyIataCode.code = :code", params);
    }
}