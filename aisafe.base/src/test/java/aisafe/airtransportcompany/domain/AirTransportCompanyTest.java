package aisafe.airtransportcompany.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AirTransportCompanyTest {

    // AC060.2 — IATA code validation

    @Test
    void ensureIATACodeRejectsOneChar() {
        assertThrows(IllegalArgumentException.class, () -> IATACode.valueOf("T"));
    }

    @Test
    void ensureIATACodeRejectsThreeChars() {
        assertThrows(IllegalArgumentException.class, () -> IATACode.valueOf("TAP"));
    }

    @Test
    void ensureIATACodeRejectsDigits() {
        assertThrows(IllegalArgumentException.class, () -> IATACode.valueOf("T1"));
    }

    @Test
    void ensureIATACodeRejectsLowercase() {
        assertThrows(IllegalArgumentException.class, () -> IATACode.valueOf("tp"));
    }

    @Test
    void ensureValidIATACodeIsAccepted() {
        final IATACode code = IATACode.valueOf("TP");
        assertEquals("TP", code.toString());
    }

    // AC060.3 — ICAO code validation

    @Test
    void ensureICAOCodeRejectsOneChar() {
        assertThrows(IllegalArgumentException.class, () -> ICAOCode.valueOf("T"));
    }

    @Test
    void ensureICAOCodeRejectsFourChars() {
        assertThrows(IllegalArgumentException.class, () -> ICAOCode.valueOf("TAPT"));
    }

    @Test
    void ensureICAOCodeAcceptsTwoChars() {
        final ICAOCode code = ICAOCode.valueOf("TP");
        assertEquals("TP", code.toString());
    }

    @Test
    void ensureICAOCodeAcceptsThreeChars() {
        final ICAOCode code = ICAOCode.valueOf("TAP");
        assertEquals("TAP", code.toString());
    }

    // AC060.1 — Company name validation

    @Test
    void ensureCompanyNameCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirTransportCompany(null, IATACode.valueOf("TP"), ICAOCode.valueOf("TAP")));
    }

    @Test
    void ensureCompanyNameCannotBeBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> new AirTransportCompany("   ", IATACode.valueOf("TP"), ICAOCode.valueOf("TAP")));
    }

    // AC060.4 — Aggregate identity

    @Test
    void ensureCompanyIdentityIsIATACode() {
        final AirTransportCompany company =
                new AirTransportCompany("TAP Air Portugal", IATACode.valueOf("TP"), ICAOCode.valueOf("TAP"));
        assertEquals(IATACode.valueOf("TP"), company.identity());
    }

    @Test
    void ensureCompaniesWithSameIATACodeAreEqual() {
        final AirTransportCompany a =
                new AirTransportCompany("TAP Air Portugal", IATACode.valueOf("TP"), ICAOCode.valueOf("TAP"));
        final AirTransportCompany b =
                new AirTransportCompany("TAP", IATACode.valueOf("TP"), ICAOCode.valueOf("TP"));
        assertEquals(a, b);
    }
}
