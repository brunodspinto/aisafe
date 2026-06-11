package aisafe.simulation.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link FlightExecutionStatus} value object (AC111.2).
 */
class FlightExecutionStatusTest {

    @Test
    void ensureExecutionStatusKeepsDesignatorAndStatus() {
        final FlightExecutionStatus s = new FlightExecutionStatus("FLIGHT_01", "COMPLETED");
        assertEquals("FLIGHT_01", s.flightDesignator());
        assertEquals("COMPLETED", s.status());
    }

    @Test
    void ensureTwoWithSameValuesAreEqual() {
        assertEquals(new FlightExecutionStatus("FLIGHT_01", "COMPLETED"),
                new FlightExecutionStatus("FLIGHT_01", "COMPLETED"));
    }

    @Test
    void ensureDifferentStatusAreNotEqual() {
        assertNotEquals(new FlightExecutionStatus("FLIGHT_01", "COMPLETED"),
                new FlightExecutionStatus("FLIGHT_01", "STOPPED"));
    }

    @Test
    void ensureBlankDesignatorIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightExecutionStatus("  ", "COMPLETED"));
    }

    @Test
    void ensureBlankStatusIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightExecutionStatus("FLIGHT_01", "  "));
    }
}
