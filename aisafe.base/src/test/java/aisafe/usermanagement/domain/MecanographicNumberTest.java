package aisafe.usermanagement.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link MecanographicNumber} value object.
 * Verifies validation and equality semantics.
 */
class MecanographicNumberTest {

    @Test
    void ensureValidMecanographicNumberIsAccepted() {
        final MecanographicNumber mn = new MecanographicNumber("EMP001");
        assertEquals("EMP001", mn.toString());
    }

    @Test
    void ensureMecanographicNumberCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> new MecanographicNumber(null));
    }

    @Test
    void ensureMecanographicNumberCannotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> new MecanographicNumber("   "));
    }

    @Test
    void ensureValueOfFactoryWorks() {
        final MecanographicNumber mn = MecanographicNumber.valueOf("EMP002");
        assertEquals("EMP002", mn.toString());
    }

    @Test
    void ensureTwoNumbersWithSameValueAreEqual() {
        final MecanographicNumber a = new MecanographicNumber("EMP001");
        final MecanographicNumber b = new MecanographicNumber("EMP001");
        assertEquals(a, b);
    }

    @Test
    void ensureTwoNumbersWithDifferentValuesAreNotEqual() {
        final MecanographicNumber a = new MecanographicNumber("EMP001");
        final MecanographicNumber b = new MecanographicNumber("EMP002");
        assertNotEquals(a, b);
    }

    @Test
    void ensureEqualsReturnsTrueForSameInstance() {
        final MecanographicNumber mn = new MecanographicNumber("EMP001");
        assertEquals(mn, mn);
    }

    @Test
    void ensureEqualsReturnsFalseForNull() {
        final MecanographicNumber mn = new MecanographicNumber("EMP001");
        assertNotEquals(null, mn);
    }

    @Test
    void ensureEqualsReturnsFalseForDifferentType() {
        final MecanographicNumber mn = new MecanographicNumber("EMP001");
        assertNotEquals("EMP001", mn);
    }

    @Test
    void ensureHashCodeIsConsistentWithEquals() {
        final MecanographicNumber a = new MecanographicNumber("EMP001");
        final MecanographicNumber b = new MecanographicNumber("EMP001");
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void ensureToStringReturnsNumber() {
        final MecanographicNumber mn = new MecanographicNumber("EMP001");
        assertEquals("EMP001", mn.toString());
    }

    @Test
    void ensureCompareToWorks() {
        final MecanographicNumber a = new MecanographicNumber("EMP001");
        final MecanographicNumber b = new MecanographicNumber("EMP002");
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(a));
    }
}
