package aisafe.aircontrolarea.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        assertEquals("PT-N", area.areaCode().toString());
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

    @Test
    void ensureEqualityBetweenSameAreaCode() {
        final AirControlArea a = new AirControlArea(AirControlAreaCode.valueOf("PT-N"), "Northern Portugal", 1200.0, validBoundary());
        final AirControlArea b = new AirControlArea(AirControlAreaCode.valueOf("PT-N"), "Different Name", 500.0, validBoundary());
        assertEquals(a, b);
    }

    @Test
    void ensureInequalityWithDifferentAreaCode() {
        final AirControlArea a = new AirControlArea(AirControlAreaCode.valueOf("PT-N"), "Northern Portugal", 1200.0, validBoundary());
        final AirControlArea b = new AirControlArea(AirControlAreaCode.valueOf("PT-S"), "Southern Portugal", 1000.0, new GeoBoundary(37.0, 36.0, -7.0, -9.0));
        assertNotEquals(a, b);
    }

    @Test
    void ensureHashCodeConsistencyWithEquals() {
        final AirControlArea a = new AirControlArea(AirControlAreaCode.valueOf("PT-N"), "Northern Portugal", 1200.0, validBoundary());
        final AirControlArea b = new AirControlArea(AirControlAreaCode.valueOf("PT-N"), "Different Name", 500.0, validBoundary());
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void ensureHashCodeDiffersForUnequalAreas() {
        final AirControlArea a = new AirControlArea(AirControlAreaCode.valueOf("PT-N"), "Northern Portugal", 1200.0, validBoundary());
        final AirControlArea b = new AirControlArea(AirControlAreaCode.valueOf("PT-S"), "Southern Portugal", 1000.0, new GeoBoundary(37.0, 36.0, -7.0, -9.0));
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void ensureGettersReturnCorrectValues() {
        final GeoBoundary boundary = validBoundary();
        final AirControlArea area = new AirControlArea(AirControlAreaCode.valueOf("PT-N"), "Northern Portugal", 1200.0, boundary);
        assertEquals("Northern Portugal", area.name());
        assertEquals(boundary, area.boundaries());
        assertEquals(1200.0, area.minimumFuelRequired());
    }

    @Test
    void ensureToStringContainsAreaCode() {
        final AirControlArea area = new AirControlArea(AirControlAreaCode.valueOf("PT-N"), "Northern Portugal", 1200.0, validBoundary());
        assertTrue(area.toString().contains("PT-N"));
    }

    @Test
    void ensureSameAsReturnsTrueForSameInstance() {
        final AirControlArea area = new AirControlArea(AirControlAreaCode.valueOf("PT-N"), "Northern Portugal", 1200.0, validBoundary());
        assertTrue(area.sameAs(area));
    }

    @Test
    void ensureIdentityReturnsAreaCode() {
        final AirControlArea area = new AirControlArea(AirControlAreaCode.valueOf("PT-N"), "Northern Portugal", 1200.0, validBoundary());
        assertEquals(AirControlAreaCode.valueOf("PT-N"), area.identity());
    }

    @Test
    void ensureNorthLatitudeMustBeGreaterThanSouthLatitude() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirControlArea(AirControlAreaCode.valueOf("PT-N"), "Area", 1200.0,
                        new GeoBoundary(36.95, 42.15, -6.18, -9.50)));
    }
}