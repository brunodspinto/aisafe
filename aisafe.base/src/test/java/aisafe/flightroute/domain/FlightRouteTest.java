package aisafe.flightroute.domain;

import aisafe.airtransportcompany.domain.IATACode;
import aisafe.airport.domain.AirportIATACode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link FlightRoute} aggregate root.
 * Verifies construction validation, identity, status and equality.
 */
class FlightRouteTest {

    private static RouteName validRouteName() {
        return new RouteName("TP123");
    }

    private static AirportIATACode origin() {
        return AirportIATACode.valueOf("OPO");
    }

    private static AirportIATACode destination() {
        return AirportIATACode.valueOf("LIS");
    }

    private static IATACode company() {
        return IATACode.valueOf("TP");
    }

    private static FlightRoute validFlightRoute() {
        return new FlightRoute(validRouteName(), origin(), destination(), company());
    }

    // -----------------------------------------------------------------------
    // Creation and validation
    // -----------------------------------------------------------------------

    @Test
    void ensureValidFlightRouteCanBeCreated() {
        final FlightRoute route = validFlightRoute();
        assertEquals("TP123", route.identity().toString());
        assertEquals(FlightRouteStatus.ACTIVE, route.status());
        assertEquals("OPO", route.originAirport().code());
        assertEquals("LIS", route.destinationAirport().code());
        assertEquals("TP", route.companyIataCode().code());
    }

    @Test
    void ensureRouteNameCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () ->
                new FlightRoute(null, origin(), destination(), company()));
    }

    @Test
    void ensureOriginAirportCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () ->
                new FlightRoute(validRouteName(), null, destination(), company()));
    }

    @Test
    void ensureDestinationAirportCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () ->
                new FlightRoute(validRouteName(), origin(), null, company()));
    }

    @Test
    void ensureCompanyCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () ->
                new FlightRoute(validRouteName(), origin(), destination(), null));
    }

    @Test
    void ensureOriginAndDestinationCannotBeTheSame() {
        assertThrows(IllegalArgumentException.class, () ->
                new FlightRoute(validRouteName(),
                        AirportIATACode.valueOf("OPO"),
                        AirportIATACode.valueOf("OPO"),
                        company()));
    }

    // -----------------------------------------------------------------------
    // Status
    // -----------------------------------------------------------------------

    @Test
    void ensureStatusStartsAsActive() {
        final FlightRoute route = validFlightRoute();
        assertEquals(FlightRouteStatus.ACTIVE, route.status());
        assertTrue(route.isActive());
    }

    @Test
    void ensureIsActiveReturnsTrueForNewRoute() {
        assertTrue(validFlightRoute().isActive());
    }

    @Test
    void ensureActiveUntilIsNullForNewRoute() {
        assertNull(validFlightRoute().activeUntil());
    }

    // -----------------------------------------------------------------------
    // Deactivation (US074)
    // -----------------------------------------------------------------------

    @Test
    void ensureDeactivateSetsStatusToInactive() {
        final FlightRoute route = validFlightRoute();
        route.deactivate(LocalDate.of(2025, 8, 1));
        assertEquals(FlightRouteStatus.INACTIVE, route.status());
    }

    @Test
    void ensureDeactivateSetsActiveUntilDate() {
        final LocalDate date = LocalDate.of(2025, 8, 1);
        final FlightRoute route = validFlightRoute();
        route.deactivate(date);
        assertEquals(date, route.activeUntil());
    }

    @Test
    void ensureIsActiveReturnsFalseAfterDeactivation() {
        final FlightRoute route = validFlightRoute();
        route.deactivate(LocalDate.of(2025, 8, 1));
        assertFalse(route.isActive());
    }

    @Test
    void ensureDeactivateWithNullDateThrows() {
        final FlightRoute route = validFlightRoute();
        assertThrows(IllegalArgumentException.class, () -> route.deactivate(null));
    }

    @Test
    void ensureCannotDeactivateAlreadyInactiveRoute() {
        final FlightRoute route = validFlightRoute();
        route.deactivate(LocalDate.of(2025, 8, 1));
        assertThrows(IllegalStateException.class,
                () -> route.deactivate(LocalDate.of(2025, 9, 1)));
    }

    // -----------------------------------------------------------------------
    // Identity and equality
    // -----------------------------------------------------------------------

    @Test
    void ensureIdentityReturnsRouteName() {
        final FlightRoute route = validFlightRoute();
        assertEquals(new RouteName("TP123"), route.identity());
    }

    @Test
    void ensureTwoRoutesWithSameNameAreEqual() {
        final FlightRoute r1 = new FlightRoute(new RouteName("TP123"),
                AirportIATACode.valueOf("OPO"), AirportIATACode.valueOf("LIS"), company());
        final FlightRoute r2 = new FlightRoute(new RouteName("TP123"),
                AirportIATACode.valueOf("LIS"), AirportIATACode.valueOf("FAO"), company());
        assertEquals(r1, r2);
    }

    @Test
    void ensureTwoRoutesWithDifferentNamesAreNotEqual() {
        final FlightRoute r1 = new FlightRoute(new RouteName("TP123"),
                origin(), destination(), company());
        final FlightRoute r2 = new FlightRoute(new RouteName("TP456"),
                origin(), destination(), company());
        assertNotEquals(r1, r2);
    }

    @Test
    void ensureEqualsReturnsTrueForSameInstance() {
        final FlightRoute route = validFlightRoute();
        assertEquals(route, route);
    }

    @Test
    void ensureEqualsReturnsFalseForNull() {
        assertNotEquals(null, validFlightRoute());
    }

    @Test
    void ensureHashCodeIsConsistentWithEquals() {
        final FlightRoute r1 = validFlightRoute();
        final FlightRoute r2 = validFlightRoute();
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void ensureSameAsReturnsTrueForEqualRoutes() {
        final FlightRoute r1 = validFlightRoute();
        final FlightRoute r2 = validFlightRoute();
        assertTrue(r1.sameAs(r2));
    }

    @Test
    void ensureToStringContainsRouteName() {
        assertTrue(validFlightRoute().toString().contains("TP123"));
    }
}