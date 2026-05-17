package aisafe.aircraft.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link RegistrationNumber} value object.
 * Verifies validation, normalisation and equality semantics.
 */
class RegistrationNumberTest {

    @Test
    void ensureValidRegistrationNumberIsAccepted() {
        final RegistrationNumber rn = new RegistrationNumber("CS-TUA");
        assertEquals("CS-TUA", rn.toString());
    }

    @Test
    void ensureRegistrationNumberCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> new RegistrationNumber(null));
    }

    @Test
    void ensureRegistrationNumberCannotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> new RegistrationNumber("   "));
    }

    @Test
    void ensureRegistrationNumberIsTrimmed() {
        final RegistrationNumber rn = new RegistrationNumber("  CS-TUA  ");
        assertEquals("CS-TUA", rn.toString());
    }

    @Test
    void ensureRegistrationNumberIsUpperCased() {
        final RegistrationNumber rn = new RegistrationNumber("cs-tua");
        assertEquals("CS-TUA", rn.toString());
    }

    @Test
    void ensureValueOfFactoryWorks() {
        final RegistrationNumber rn = RegistrationNumber.valueOf("CS-TUB");
        assertEquals("CS-TUB", rn.toString());
    }

    @Test
    void ensureTwoRegistrationNumbersWithSameValueAreEqual() {
        final RegistrationNumber a = new RegistrationNumber("CS-TUA");
        final RegistrationNumber b = new RegistrationNumber("CS-TUA");
        assertEquals(a, b);
    }

    @Test
    void ensureTwoRegistrationNumbersWithDifferentValuesAreNotEqual() {
        final RegistrationNumber a = new RegistrationNumber("CS-TUA");
        final RegistrationNumber b = new RegistrationNumber("CS-TUB");
        assertNotEquals(a, b);
    }

    @Test
    void ensureEqualsReturnsTrueForSameInstance() {
        final RegistrationNumber rn = new RegistrationNumber("CS-TUA");
        assertEquals(rn, rn);
    }

    @Test
    void ensureEqualsReturnsFalseForNull() {
        final RegistrationNumber rn = new RegistrationNumber("CS-TUA");
        assertNotEquals(null, rn);
    }

    @Test
    void ensureEqualsReturnsFalseForDifferentType() {
        final RegistrationNumber rn = new RegistrationNumber("CS-TUA");
        assertNotEquals("CS-TUA", rn);
    }

    @Test
    void ensureHashCodeIsConsistentWithEquals() {
        final RegistrationNumber a = new RegistrationNumber("CS-TUA");
        final RegistrationNumber b = new RegistrationNumber("CS-TUA");
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void ensureCompareToWorks() {
        final RegistrationNumber a = new RegistrationNumber("CS-TUA");
        final RegistrationNumber b = new RegistrationNumber("CS-TUB");
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(a));
    }
}
