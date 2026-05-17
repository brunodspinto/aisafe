package aisafe.airtransportcompany.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link IATACode} value object.
 * Verifies 2-letter format validation and equality semantics.
 */
class IATACodeTest {

    @Test
    void ensureValidIATACodeIsAccepted() {
        final IATACode code = new IATACode("TP");
        assertEquals("TP", code.code());
    }

    @Test
    void ensureIATACodeCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> new IATACode(null));
    }

    @Test
    void ensureIATACodeCannotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> new IATACode("  "));
    }

    @Test
    void ensureIATACodeMustBeExactly2Letters() {
        assertThrows(IllegalArgumentException.class, () -> new IATACode("T"));
        assertThrows(IllegalArgumentException.class, () -> new IATACode("TAP"));
    }

    @Test
    void ensureIATACodeMustBeUppercase() {
        assertThrows(IllegalArgumentException.class, () -> new IATACode("tp"));
    }

    @Test
    void ensureIATACodeRejectsDigits() {
        assertThrows(IllegalArgumentException.class, () -> new IATACode("T1"));
    }

    @Test
    void ensureValueOfFactoryWorks() {
        final IATACode code = IATACode.valueOf("BA");
        assertEquals("BA", code.code());
    }

    @Test
    void ensureTwoCodesWithSameValueAreEqual() {
        final IATACode a = new IATACode("TP");
        final IATACode b = new IATACode("TP");
        assertEquals(a, b);
    }

    @Test
    void ensureTwoCodesWithDifferentValuesAreNotEqual() {
        final IATACode a = new IATACode("TP");
        final IATACode b = new IATACode("BA");
        assertNotEquals(a, b);
    }

    @Test
    void ensureEqualsReturnsTrueForSameInstance() {
        final IATACode code = new IATACode("TP");
        assertEquals(code, code);
    }

    @Test
    void ensureEqualsReturnsFalseForNull() {
        final IATACode code = new IATACode("TP");
        assertNotEquals(null, code);
    }

    @Test
    void ensureEqualsReturnsFalseForDifferentType() {
        final IATACode code = new IATACode("TP");
        assertNotEquals("TP", code);
    }

    @Test
    void ensureHashCodeIsConsistentWithEquals() {
        final IATACode a = new IATACode("TP");
        final IATACode b = new IATACode("TP");
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void ensureToStringReturnsCode() {
        final IATACode code = new IATACode("TP");
        assertEquals("TP", code.toString());
    }

    @Test
    void ensureCompareToWorks() {
        final IATACode a = new IATACode("BA");
        final IATACode b = new IATACode("TP");
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(a));
    }
}
