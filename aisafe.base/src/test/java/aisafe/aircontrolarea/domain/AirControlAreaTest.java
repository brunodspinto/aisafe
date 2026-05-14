package aisafe.aircontrolarea.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link AirControlArea} aggregate root.
 * Verifies construction validation, identity, and equality.
 */
class AirControlAreaTest {

    /**
     * Builds a valid boundary used by the acceptance tests.
     */
    private static GeoBoundary validBoundary() {
        return new GeoBoundary(42.15, 36.95, -6.18, -9.50);
    }

    /**
     * Verifies that a valid air control area can be created.
     */
    @Test
    void ensureValidAirControlAreaCanBeCreated() {
        final AirControlArea area = new AirControlArea(AirControlAreaCode.valueOf("PT-N"), "Northern Portugal", 1200.0, validBoundary());

        assertEquals("PT-N", area.areaCode());
        assertEquals("Northern Portugal", area.name());
        assertEquals(1200.0, area.minimumFuelRequired());
        assertEquals(validBoundary(), area.boundaries());
    }

    /**
     * Verifies that the area code is mandatory and cannot be blank.
     */
    @Test
    void ensureAreaCodeCannotBeNullOrBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirControlArea((AirControlAreaCode) null, "Area", 1200.0, validBoundary()));

        assertThrows(IllegalArgumentException.class,
                () -> new AirControlArea(AirControlAreaCode.valueOf("   "), "Area", 1200.0, validBoundary()));
    }

    /**
     * Verifies that the area name is mandatory and cannot be blank.
     */
    @Test
    void ensureNameCannotBeNullOrBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirControlArea(AirControlAreaCode.valueOf("PT-N"), null, 1200.0, validBoundary()));

        assertThrows(IllegalArgumentException.class,
                () -> new AirControlArea(AirControlAreaCode.valueOf("PT-N"), "   ", 1200.0, validBoundary()));
    }

    /**
     * Verifies that minimum fuel cannot be negative.
     */
    @Test
    void ensureMinimumFuelCannotBeNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirControlArea(AirControlAreaCode.valueOf("PT-N"), "Area", -1.0, validBoundary()));
    }

    /**
     * Verifies that boundaries are mandatory for an air control area.
     */
    @Test
    void ensureBoundariesCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirControlArea(AirControlAreaCode.valueOf("PT-N"), "Area", 1200.0, null));
    }
}