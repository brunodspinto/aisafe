package aisafe.airport.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link AirportIATACode} value object.
 * Verifies IATA code format validation and equality semantics.
 */
class AirportIATACodeTest {

    @Test
    void ensureValidIATACodeIsAccepted() {
        final AirportIATACode code = new AirportIATACode("LIS");
        assertEquals("LIS", code.code());
    }

    @Test
    void ensureIATACodeCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> new AirportIATACode(null));
    }

    @Test
    void ensureIATACodeCannotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> new AirportIATACode("   "));
    }

    @Test
    void ensureIATACodeMustBeExactly3Letters() {
        assertThrows(IllegalArgumentException.class, () -> new AirportIATACode("LI"));
        assertThrows(IllegalArgumentException.class, () -> new AirportIATACode("LISB"));
    }

    @Test
    void ensureIATACodeMustBeUppercase() {
        assertThrows(IllegalArgumentException.class, () -> new AirportIATACode("lis"));
    }

    @Test
    void ensureTwoCodesWithSameValueAreEqual() {
        final AirportIATACode a = new AirportIATACode("LIS");
        final AirportIATACode b = new AirportIATACode("LIS");
        assertEquals(a, b);
    }

    @Test
    void ensureTwoCodesWithDifferentValuesAreNotEqual() {
        final AirportIATACode a = new AirportIATACode("LIS");
        final AirportIATACode b = new AirportIATACode("OPO");
        assertNotEquals(a, b);
    }

    @Test
    void ensureValueOfFactoryWorks() {
        final AirportIATACode code = AirportIATACode.valueOf("OPO");
        assertEquals("OPO", code.code());
    }

    @Test
    void ensureEqualsReturnsTrueForSameInstance() {
        final AirportIATACode code = new AirportIATACode("LIS");
        assertEquals(code, code);
    }

    @Test
    void ensureEqualsReturnsFalseForNull() {
        final AirportIATACode code = new AirportIATACode("LIS");
        assertNotEquals(null, code);
    }

    @Test
    void ensureEqualsReturnsFalseForDifferentType() {
        final AirportIATACode code = new AirportIATACode("LIS");
        assertNotEquals("LIS", code);
    }

    @Test
    void ensureToStringReturnsCode() {
        final AirportIATACode code = new AirportIATACode("LIS");
        assertEquals("LIS", code.toString());
    }

    @Test
    void ensureCompareToWorks() {
        final AirportIATACode a = new AirportIATACode("LIS");
        final AirportIATACode b = new AirportIATACode("OPO");
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(a));
    }
}
