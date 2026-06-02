package aisafe.infrastructure.persistence.jpa;

import aisafe.flightplan.domain.FlightPlan;
import aisafe.flightplan.domain.FlightPlanDesignator;
import aisafe.flightroute.domain.FlightRoute;
import aisafe.flightroute.repositories.FlightRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * JPA implementation of {@link FlightRepository}.
 * <p>
 * A "planned flight" is a {@link FlightPlan} that references a route
 * (via its {@code routeName}) and has a concrete {@code departureDateTime}.
 * This is used by the Deactivate Flight Route use case (US074) to enforce AC074.3.
 * A single existence (count) query is used to avoid loading flight plans into memory.
 */
public class JpaFlightRepository
        extends JpaAutoTxRepository<FlightPlan, FlightPlanDesignator, FlightPlanDesignator>
        implements FlightRepository {

    public JpaFlightRepository(final String puName) {
        super(puName, "designator");
    }

    public JpaFlightRepository(final TransactionalContext tx) {
        super(tx, "designator");
    }

    @Override
    public boolean hasFlightsAfter(final FlightRoute route, final LocalDate deactivationDate) {
        final Map<String, Object> params = new HashMap<>();
        params.put("route", route.identity().name());
        params.put("date", deactivationDate.atStartOfDay());
        return matchOne(
                "e.routeName.name = :route AND e.departureDateTime >= :date", params).isPresent();
    }
}
