package aisafe.infrastructure.persistence.inmemory;

import aisafe.flightplan.domain.FlightPlan;
import aisafe.flightplan.repositories.FlightPlanRepository;
import aisafe.flightroute.domain.FlightRoute;
import aisafe.flightroute.domain.RouteName;
import aisafe.flightroute.repositories.FlightRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * In-memory implementation of {@link FlightRepository} for development and testing.
 * <p>
 * A "planned flight" is a {@link FlightPlan} that references a route
 * (via its {@code routeName}) and has a concrete {@code departureDateTime}.
 * The check is delegated to the {@link FlightPlanRepository} so both repositories
 * share the same underlying data.
 */
public class InMemoryFlightRepository implements FlightRepository {

    private final FlightPlanRepository flightPlans;

    public InMemoryFlightRepository(final FlightPlanRepository flightPlans) {
        this.flightPlans = flightPlans;
    }

    @Override
    public boolean hasFlightsAfter(final FlightRoute route, final LocalDate deactivationDate) {
        final RouteName target = route.identity();
        for (final FlightPlan plan : flightPlans.findAll()) {
            final RouteName planRoute = plan.routeName();
            final LocalDateTime departure = plan.departureDateTime();
            if (target.equals(planRoute)
                    && departure != null
                    && !departure.toLocalDate().isBefore(deactivationDate)) {
                return true;
            }
        }
        return false;
    }
}
