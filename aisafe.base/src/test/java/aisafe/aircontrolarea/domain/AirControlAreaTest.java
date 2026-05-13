package aisafe.aircontrolarea.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link AirControlArea} aggregate root.
 * Verifies construction validation, identity, and equality.
 */
class AirControlAreaTest {

    private static GeoBoundary validBoundary() {
        return new GeoBoundary(42.15, 36.95, -6.18, -9.50);
    }

    @Test
    void ensureValidAirControlAreaCanBeCreated() {
        final AirControlArea area = new AirControlArea("PT-N", "Northern Portugal", 1200.0, validBoundary());

        assertEquals("PT-N", area.areaCode());
        assertEquals("Northern Portugal", area.name());
        assertEquals(1200.0, area.minimumFuelRequired());
        assertEquals(validBoundary(), area.boundaries());
    }

    @Test
    void ensureAreaCodeCannotBeNullOrBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirControlArea(null, "Area", 1200.0, validBoundary()));

        assertThrows(IllegalArgumentException.class,
                () -> new AirControlArea("   ", "Area", 1200.0, validBoundary()));
    }

    @Test
    void ensureNameCannotBeNullOrBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirControlArea("PT-N", null, 1200.0, validBoundary()));

        assertThrows(IllegalArgumentException.class,
                () -> new AirControlArea("PT-N", "   ", 1200.0, validBoundary()));
    }

    @Test
    void ensureMinimumFuelCannotBeNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirControlArea("PT-N", "Area", -1.0, validBoundary()));
    }

    @Test
    void ensureBoundariesCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirControlArea("PT-N", "Area", 1200.0, null));
    }
}