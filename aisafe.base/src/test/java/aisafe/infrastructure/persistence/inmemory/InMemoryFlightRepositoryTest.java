package aisafe.infrastructure.persistence.inmemory;

import aisafe.aircraft.domain.RegistrationNumber;
import aisafe.airport.domain.AirportIATACode;
import aisafe.airtransportcompany.domain.IATACode;
import aisafe.dsl.ast.FlightType;
import aisafe.flightplan.domain.FlightPlan;
import aisafe.flightplan.domain.FlightPlanDesignator;
import aisafe.flightplan.domain.FuelQuantity;
import aisafe.flightroute.domain.FlightRoute;
import aisafe.flightroute.domain.RouteName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link InMemoryFlightRepository#hasFlightsAfter} — the cross-aggregate
 * planned-flight check that enforces AC074.3 (Deactivate a Flight Route).
 * <p>
 * Note: the framework's in-memory store is shared statically across instances, so each
 * test uses a distinct route name to remain isolated from other tests in the JVM run.
 */
class InMemoryFlightRepositoryTest {

    private InMemoryFlightPlanRepository flightPlans;
    private InMemoryFlightRepository flights;

    @BeforeEach
    void setUp() {
        flightPlans = new InMemoryFlightPlanRepository();
        flights = new InMemoryFlightRepository(flightPlans);
    }

    private static FlightRoute routeNamed(final String name) {
        return new FlightRoute(new RouteName(name),
                AirportIATACode.valueOf("OPO"),
                AirportIATACode.valueOf("LIS"),
                IATACode.valueOf("TP"));
    }

    private FlightPlan plan(final String designator,
                            final String routeName,
                            final LocalDateTime departure) {
        return new FlightPlan(
                FlightPlanDesignator.valueOf(designator),
                FlightType.REGULAR,
                new RouteName(routeName),
                RegistrationNumber.valueOf("CS-TUA"),
                1L,
                departure,
                FuelQuantity.valueOf(5000));
    }

    @Test
    void ensureReturnsFalseWhenNoPlansExistForRoute() {
        assertFalse(flights.hasFlightsAfter(routeNamed("TP010"), LocalDate.now().plusDays(10)));
    }

    @Test
    void ensureDetectsFlightDepartingOnDeactivationDate() {
        final LocalDateTime departure = LocalDateTime.now().plusDays(30);
        flightPlans.save(plan("TP0201", "TP020", departure));
        assertTrue(flights.hasFlightsAfter(routeNamed("TP020"), departure.toLocalDate()));
    }

    @Test
    void ensureDetectsFlightDepartingAfterDeactivationDate() {
        final LocalDateTime departure = LocalDateTime.now().plusDays(30);
        flightPlans.save(plan("TP0301", "TP030", departure));
        assertTrue(flights.hasFlightsAfter(routeNamed("TP030"),
                departure.toLocalDate().minusDays(5)));
    }

    @Test
    void ensureIgnoresFlightDepartingBeforeDeactivationDate() {
        final LocalDateTime departure = LocalDateTime.now().plusDays(10);
        flightPlans.save(plan("TP0401", "TP040", departure));
        assertFalse(flights.hasFlightsAfter(routeNamed("TP040"),
                departure.toLocalDate().plusDays(5)));
    }

    @Test
    void ensureIgnoresFlightsOfOtherRoutes() {
        final LocalDateTime departure = LocalDateTime.now().plusDays(30);
        flightPlans.save(plan("TP0591", "TP059", departure));
        assertFalse(flights.hasFlightsAfter(routeNamed("TP050"), departure.toLocalDate()));
    }

    @Test
    void ensureIgnoresDslImportedPlansWithoutRouteOrDeparture() {
        // DSL-imported plan: routeName and departureDateTime are null
        flightPlans.save(new FlightPlan(
                FlightPlanDesignator.valueOf("TP0601"), FlightType.REGULAR, "flight TP0601 { }"));
        assertFalse(flights.hasFlightsAfter(routeNamed("TP060"), LocalDate.now().plusDays(10)));
    }
}
