package aisafe.airport.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AirportICAOCodeTest {

    @Test
    void ensureValidICAOCodeIsAccepted() {
        final AirportICAOCode code = new AirportICAOCode("LPPT");
        assertEquals("LPPT", code.code());
    }

    @Test
    void ensureICAOCodeCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> new AirportICAOCode(null));
    }

    @Test
    void ensureICAOCodeCannotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> new AirportICAOCode("   "));
    }

    @Test
    void ensureICAOCodeMustBeExactly4Letters() {
        assertThrows(IllegalArgumentException.class, () -> new AirportICAOCode("LPP"));
        assertThrows(IllegalArgumentException.class, () -> new AirportICAOCode("LPPTE"));
    }

    @Test
    void ensureICAOCodeMustBeUppercase() {
        assertThrows(IllegalArgumentException.class, () -> new AirportICAOCode("lppt"));
    }

    @Test
    void ensureTwoCodesWithSameValueAreEqual() {
        final AirportICAOCode a = new AirportICAOCode("LPPT");
        final AirportICAOCode b = new AirportICAOCode("LPPT");
        assertEquals(a, b);
    }

    @Test
    void ensureTwoCodesWithDifferentValuesAreNotEqual() {
        final AirportICAOCode a = new AirportICAOCode("LPPT");
        final AirportICAOCode b = new AirportICAOCode("LPPR");
        assertNotEquals(a, b);
    }

    @Test
    void ensureValueOfFactoryWorks() {
        final AirportICAOCode code = AirportICAOCode.valueOf("LPPR");
        assertEquals("LPPR", code.code());
    }

    @Test
    void ensureEqualsReturnsTrueForSameInstance() {
        final AirportICAOCode code = new AirportICAOCode("LPPT");
        assertEquals(code, code);
    }

    @Test
    void ensureEqualsReturnsFalseForNull() {
        final AirportICAOCode code = new AirportICAOCode("LPPT");
        assertNotEquals(null, code);
    }

    @Test
    void ensureEqualsReturnsFalseForDifferentType() {
        final AirportICAOCode code = new AirportICAOCode("LPPT");
        assertNotEquals("LPPT", code);
    }

    @Test
    void ensureToStringReturnsCode() {
        final AirportICAOCode code = new AirportICAOCode("LPPT");
        assertEquals("LPPT", code.toString());
    }

    @Test
    void ensureHashCodeIsConsistentWithEquals() {
        final AirportICAOCode a = new AirportICAOCode("LPPT");
        final AirportICAOCode b = new AirportICAOCode("LPPT");
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void ensureCodeGetterReturnsValue() {
        final AirportICAOCode code = new AirportICAOCode("LPPT");
        assertEquals("LPPT", code.code());
    }
}
