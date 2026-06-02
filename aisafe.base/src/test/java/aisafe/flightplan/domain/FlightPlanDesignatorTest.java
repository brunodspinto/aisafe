package aisafe.flightplan.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link FlightPlanDesignator} value object.
 * Verifies the format rule xxN(N)(N)(N)(a) and normalisation.
 */
class FlightPlanDesignatorTest {

    @Test
    void ensureValidDesignatorIsAccepted() {
        assertEquals("TP1234", FlightPlanDesignator.valueOf("TP1234").toString());
    }

    @Test
    void ensureSingleDigitDesignatorIsAccepted() {
        assertEquals("TP1", FlightPlanDesignator.valueOf("TP1").toString());
    }

    @Test
    void ensureOperationalSuffixIsAccepted() {
        assertEquals("TP1234A", FlightPlanDesignator.valueOf("TP1234A").toString());
    }

    @Test
    void ensureDesignatorIsNormalisedToUpperCase() {
        assertEquals("TP1234", FlightPlanDesignator.valueOf("tp1234").toString());
    }

    @Test
    void ensureNullIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> FlightPlanDesignator.valueOf(null));
    }

    @Test
    void ensureBlankIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> FlightPlanDesignator.valueOf("   "));
    }

    @Test
    void ensureMissingDigitsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> FlightPlanDesignator.valueOf("TP"));
    }

    @Test
    void ensureSingleLetterPrefixIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> FlightPlanDesignator.valueOf("T1234"));
    }

    @Test
    void ensureTooManyDigitsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> FlightPlanDesignator.valueOf("TP12345"));
    }

    @Test
    void ensureEqualDesignatorsAreEqual() {
        assertEquals(FlightPlanDesignator.valueOf("TP1234"), FlightPlanDesignator.valueOf("tp1234"));
    }

    @Test
    void ensureDifferentDesignatorsAreNotEqual() {
        assertNotEquals(FlightPlanDesignator.valueOf("TP1234"), FlightPlanDesignator.valueOf("TP5678"));
    }
}
