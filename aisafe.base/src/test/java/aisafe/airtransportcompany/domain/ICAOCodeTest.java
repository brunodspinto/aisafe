package aisafe.airtransportcompany.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link ICAOCode} value object.
 * Verifies 2–3 letter format validation and equality semantics.
 */
class ICAOCodeTest {

    @Test
    void ensureValid2LetterICAOCodeIsAccepted() {
        final ICAOCode code = new ICAOCode("TP");
        assertEquals("TP", code.code());
    }

    @Test
    void ensureValid3LetterICAOCodeIsAccepted() {
        final ICAOCode code = new ICAOCode("TAP");
        assertEquals("TAP", code.code());
    }

    @Test
    void ensureICAOCodeCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> new ICAOCode(null));
    }

    @Test
    void ensureICAOCodeCannotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> new ICAOCode("  "));
    }

    @Test
    void ensureICAOCodeCannotBe1Letter() {
        assertThrows(IllegalArgumentException.class, () -> new ICAOCode("T"));
    }

    @Test
    void ensureICAOCodeCannotBe4Letters() {
        assertThrows(IllegalArgumentException.class, () -> new ICAOCode("TAPA"));
    }

    @Test
    void ensureICAOCodeMustBeUppercase() {
        assertThrows(IllegalArgumentException.class, () -> new ICAOCode("tap"));
    }

    @Test
    void ensureICAOCodeRejectsDigits() {
        assertThrows(IllegalArgumentException.class, () -> new ICAOCode("T1P"));
    }

    @Test
    void ensureValueOfFactoryWorks() {
        final ICAOCode code = ICAOCode.valueOf("BAW");
        assertEquals("BAW", code.code());
    }

    @Test
    void ensureTwoCodesWithSameValueAreEqual() {
        final ICAOCode a = new ICAOCode("TAP");
        final ICAOCode b = new ICAOCode("TAP");
        assertEquals(a, b);
    }

    @Test
    void ensureTwoCodesWithDifferentValuesAreNotEqual() {
        final ICAOCode a = new ICAOCode("TAP");
        final ICAOCode b = new ICAOCode("BAW");
        assertNotEquals(a, b);
    }

    @Test
    void ensureEqualsReturnsTrueForSameInstance() {
        final ICAOCode code = new ICAOCode("TAP");
        assertEquals(code, code);
    }

    @Test
    void ensureEqualsReturnsFalseForNull() {
        final ICAOCode code = new ICAOCode("TAP");
        assertNotEquals(null, code);
    }

    @Test
    void ensureEqualsReturnsFalseForDifferentType() {
        final ICAOCode code = new ICAOCode("TAP");
        assertNotEquals("TAP", code);
    }

    @Test
    void ensureHashCodeIsConsistentWithEquals() {
        final ICAOCode a = new ICAOCode("TAP");
        final ICAOCode b = new ICAOCode("TAP");
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void ensureToStringReturnsCode() {
        final ICAOCode code = new ICAOCode("TAP");
        assertEquals("TAP", code.toString());
    }
}
