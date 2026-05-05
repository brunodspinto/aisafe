package aisafe.aircontrolarea.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeoBoundaryTest {

    @Test
    void ensureValidCoordinatesCreateBoundary() {
        final GeoBoundary boundary = new GeoBoundary(42.15, 36.95, -6.18, -9.50);

        assertEquals(42.15, boundary.northLatitude());
        assertEquals(36.95, boundary.southLatitude());
        assertEquals(-6.18, boundary.eastLongitude());
        assertEquals(-9.50, boundary.westLongitude());
    }

    @Test
    void ensureNorthLatitudeMustBeGreaterThanSouthLatitude() {
        assertThrows(IllegalArgumentException.class,
                () -> new GeoBoundary(30.0, 40.0, -10.0, 10.0));
    }

    @Test
    void ensureCoordinatesMustBeWithinLatitudeLimits() {
        assertThrows(IllegalArgumentException.class,
                () -> new GeoBoundary(95.0, 20.0, -10.0, 10.0));
    }

    @Test
    void ensureCoordinatesMustBeWithinLongitudeLimits() {
        assertThrows(IllegalArgumentException.class,
                () -> new GeoBoundary(40.0, 30.0, 200.0, 10.0));
    }
}