package aisafe.airport.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeoCoordinateTest {

    @Test
    void ensureValidCoordinateCanBeCreated() {
        final GeoCoordinate coord = new GeoCoordinate(38.7756, -9.1354);
        assertEquals(38.7756, coord.latitude());
        assertEquals(-9.1354, coord.longitude());
    }

    @Test
    void ensureLatitudeCannotBeAbove90() {
        assertThrows(IllegalArgumentException.class, () -> new GeoCoordinate(91.0, 0.0));
    }

    @Test
    void ensureLatitudeCannotBeBelow90() {
        assertThrows(IllegalArgumentException.class, () -> new GeoCoordinate(-91.0, 0.0));
    }

    @Test
    void ensureLongitudeCannotBeAbove180() {
        assertThrows(IllegalArgumentException.class, () -> new GeoCoordinate(0.0, 181.0));
    }

    @Test
    void ensureLongitudeCannotBeBelow180() {
        assertThrows(IllegalArgumentException.class, () -> new GeoCoordinate(0.0, -181.0));
    }

    @Test
    void ensureTwoCoordinatesWithSameValuesAreEqual() {
        final GeoCoordinate a = new GeoCoordinate(38.7756, -9.1354);
        final GeoCoordinate b = new GeoCoordinate(38.7756, -9.1354);
        assertEquals(a, b);
    }

    @Test
    void ensureTwoCoordinatesWithDifferentValuesAreNotEqual() {
        final GeoCoordinate a = new GeoCoordinate(38.7756, -9.1354);
        final GeoCoordinate b = new GeoCoordinate(41.2481, -8.6814);
        assertNotEquals(a, b);
    }

    @Test
    void ensureHashCodeIsConsistentWithEquals() {
        final GeoCoordinate a = new GeoCoordinate(38.7756, -9.1354);
        final GeoCoordinate b = new GeoCoordinate(38.7756, -9.1354);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void ensureToStringContainsCoordinates() {
        final GeoCoordinate coord = new GeoCoordinate(38.7756, -9.1354);
        assertTrue(coord.toString().contains("38"));
        assertTrue(coord.toString().contains("-9"));
    }

    @Test
    void ensureEqualsReturnsTrueForSameInstance() {
        final GeoCoordinate coord = new GeoCoordinate(38.7756, -9.1354);
        assertEquals(coord, coord);
    }

    @Test
    void ensureEqualsReturnsFalseForNull() {
        final GeoCoordinate coord = new GeoCoordinate(38.7756, -9.1354);
        assertNotEquals(null, coord);
    }

    @Test
    void ensureEqualsReturnsFalseForDifferentType() {
        final GeoCoordinate coord = new GeoCoordinate(38.7756, -9.1354);
        assertNotEquals("38.7756", coord);
    }
}
