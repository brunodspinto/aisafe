package aisafe.aircontrolarea.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link GeoBoundary} value object.
 * Verifies coordinate-boundary construction and validation rules.
 */
class GeoBoundaryTest {

    /**
     * Verifies that valid coordinates create a boundary successfully.
     */
    @Test
    void ensureValidCoordinatesCreateBoundary() {
        final GeoBoundary boundary = new GeoBoundary(42.15, 36.95, -6.18, -9.50);

        assertEquals(42.15, boundary.northLatitude());
        assertEquals(36.95, boundary.southLatitude());
        assertEquals(-6.18, boundary.eastLongitude());
        assertEquals(-9.50, boundary.westLongitude());
    }

    /**
     * Verifies that north latitude must be strictly greater than south latitude.
     */
    @Test
    void ensureNorthLatitudeMustBeGreaterThanSouthLatitude() {
        assertThrows(IllegalArgumentException.class,
                () -> new GeoBoundary(30.0, 40.0, -10.0, 10.0));
    }

    /**
     * Verifies that latitude values stay within valid geographic limits.
     */
    @Test
    void ensureCoordinatesMustBeWithinLatitudeLimits() {
        assertThrows(IllegalArgumentException.class,
                () -> new GeoBoundary(95.0, 20.0, -10.0, 10.0));
    }

    /**
     * Verifies that longitude values stay within valid geographic limits.
     */
    @Test
    void ensureCoordinatesMustBeWithinLongitudeLimits() {
        assertThrows(IllegalArgumentException.class,
                () -> new GeoBoundary(40.0, 30.0, 200.0, 10.0));
    }
}