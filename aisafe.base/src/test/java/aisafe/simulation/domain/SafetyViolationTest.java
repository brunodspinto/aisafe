package aisafe.simulation.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link SafetyViolation} value object (AC111.3).
 */
class SafetyViolationTest {

    private static SafetyViolation violation() {
        return new SafetyViolation("proximity with FLIGHT_02", "FLIGHT_01",
                LocalDateTime.of(2026, 6, 1, 12, 0), 41.2, -8.6, 9000.0, 250.0, 42.5);
    }

    @Test
    void ensureViolationCarriesTimestampAndPosition() {
        final SafetyViolation v = violation();
        assertEquals(LocalDateTime.of(2026, 6, 1, 12, 0), v.timestamp());
        assertEquals(41.2, v.latitude());
        assertEquals(-8.6, v.longitude());
        assertEquals(9000.0, v.altitude());
        assertEquals("FLIGHT_01", v.flightDesignator());
    }

    @Test
    void ensureTwoViolationsWithSameValuesAreEqual() {
        assertEquals(violation(), violation());
        assertEquals(violation().hashCode(), violation().hashCode());
    }

    @Test
    void ensureViolationsWithDifferentPositionAreNotEqual() {
        final SafetyViolation other = new SafetyViolation("proximity with FLIGHT_02", "FLIGHT_01",
                LocalDateTime.of(2026, 6, 1, 12, 0), 99.9, -8.6, 9000.0, 250.0, 42.5);
        assertNotEquals(violation(), other);
    }

    @Test
    void ensureBlankFlightDesignatorIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SafetyViolation(
                "x", "  ", LocalDateTime.now(), 0, 0, 0, 0, 0));
    }

    @Test
    void ensureNullTimestampIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SafetyViolation(
                "x", "FLIGHT_01", null, 0, 0, 0, 0, 0));
    }
}
