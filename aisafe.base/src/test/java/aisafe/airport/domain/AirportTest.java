package aisafe.airport.domain;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.aircontrolarea.domain.GeoBoundary;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AirportTest {

    private static AirControlArea validArea() {
        return new AirControlArea("PT-N", "Northern Portugal", 1200.0,
                new GeoBoundary(42.15, 36.95, -6.18, -9.50));
    }

    private static GeoCoordinate validLocation() {
        return new GeoCoordinate(38.7756, -9.1354);
    }

    private static Airport validAirport() {
        return new Airport(
                new AirportIATACode("LIS"),
                new AirportICAOCode("LPPT"),
                "Humberto Delgado Airport",
                "Lisbon",
                "Portugal",
                validLocation(),
                113.0,
                validArea()
        );
    }

    @Test
    void ensureValidAirportCanBeCreated() {
        final Airport airport = validAirport();
        assertEquals("LIS", airport.iataCode().code());
        assertEquals("LPPT", airport.icaoCode().code());
        assertEquals("Humberto Delgado Airport", airport.name());
        assertEquals("Lisbon", airport.town());
        assertEquals("Portugal", airport.country());
        assertEquals(38.7756, airport.location().latitude());
        assertEquals(-9.1354, airport.location().longitude());
        assertEquals(113.0, airport.altitude());
        assertEquals("PT-N", airport.airControlArea().areaCode());
    }

    @Test
    void ensureIATACodeCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport(null, new AirportICAOCode("LPPT"),
                        "Name", "Town", "Country",
                        validLocation(), 100.0, validArea()));
    }

    @Test
    void ensureICAOCodeCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport(new AirportIATACode("LIS"), null,
                        "Name", "Town", "Country",
                        validLocation(), 100.0, validArea()));
    }

    @Test
    void ensureNameCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport(new AirportIATACode("LIS"), new AirportICAOCode("LPPT"),
                        null, "Town", "Country",
                        validLocation(), 100.0, validArea()));
    }

    @Test
    void ensureNameCannotBeBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport(new AirportIATACode("LIS"), new AirportICAOCode("LPPT"),
                        "   ", "Town", "Country",
                        validLocation(), 100.0, validArea()));
    }

    @Test
    void ensureTownCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport(new AirportIATACode("LIS"), new AirportICAOCode("LPPT"),
                        "Name", null, "Country",
                        validLocation(), 100.0, validArea()));
    }

    @Test
    void ensureCountryCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport(new AirportIATACode("LIS"), new AirportICAOCode("LPPT"),
                        "Name", "Town", null,
                        validLocation(), 100.0, validArea()));
    }

    @Test
    void ensureLocationCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport(new AirportIATACode("LIS"), new AirportICAOCode("LPPT"),
                        "Name", "Town", "Country",
                        null, 100.0, validArea()));
    }

    @Test
    void ensureAirControlAreaCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () ->
                new Airport(new AirportIATACode("LIS"), new AirportICAOCode("LPPT"),
                        "Name", "Town", "Country",
                        validLocation(), 100.0, null));
    }

    @Test
    void ensureTwoAirportsWithSameIATACodeAreEqual() {
        final Airport a1 = validAirport();
        final Airport a2 = new Airport(
                new AirportIATACode("LIS"), new AirportICAOCode("LPPT"),
                "Other Name", "Other Town", "Other Country",
                new GeoCoordinate(40.0, -8.0), 200.0, validArea()
        );
        assertEquals(a1, a2);
    }

    @Test
    void ensureTwoAirportsWithDifferentIATACodesAreNotEqual() {
        final Airport a1 = validAirport();
        final Airport a2 = new Airport(
                new AirportIATACode("OPO"), new AirportICAOCode("LPPR"),
                "Francisco Sá Carneiro", "Porto", "Portugal",
                new GeoCoordinate(41.2481, -8.6814), 69.0, validArea()
        );
        assertNotEquals(a1, a2);
    }

    @Test
    void ensureIdentityReturnsIATACode() {
        final Airport airport = validAirport();
        assertEquals(new AirportIATACode("LIS"), airport.identity());
    }

    @Test
    void ensureHashCodeIsConsistentWithEquals() {
        final Airport a1 = validAirport();
        final Airport a2 = validAirport();
        assertEquals(a1.hashCode(), a2.hashCode());
    }

    @Test
    void ensureToStringContainsIATACode() {
        final Airport airport = validAirport();
        assertTrue(airport.toString().contains("LIS"));
    }

    @Test
    void ensureSameAsReturnsTrueForEqualAirports() {
        final Airport a1 = validAirport();
        final Airport a2 = validAirport();
        assertTrue(a1.sameAs(a2));
    }

    @Test
    void ensureEqualsReturnsFalseForNull() {
        assertNotEquals(null, validAirport());
    }

    @Test
    void ensureEqualsReturnsTrueForSameInstance() {
        final Airport airport = validAirport();
        assertEquals(airport, airport);
    }
}
