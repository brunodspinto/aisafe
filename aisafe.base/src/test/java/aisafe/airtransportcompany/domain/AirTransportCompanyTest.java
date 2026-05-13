package aisafe.airtransportcompany.domain;

import aisafe.aircraft.domain.Aircraft;
import aisafe.aircraft.domain.CabinConfiguration;
import aisafe.aircraftmodel.domain.AircraftModel;
import aisafe.aircraftmodel.domain.AircraftType;
import aisafe.enginemodel.domain.EngineModel;
import aisafe.enginemodel.domain.EngineType;
import aisafe.maker.domain.Maker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link AirTransportCompany} aggregate root.
 * Verifies IATA/ICAO code validation and fleet management behaviour.
 */
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

    // AC070 — Fleet management

    private static Aircraft validAircraft(final String registration) {
        final Maker maker = new Maker("Boeing", "USA");
        final EngineModel engine = new EngineModel("CFM56", "CFM International", EngineType.TURBOFAN, 120.0, 0.35);
        final AircraftModel model = new AircraftModel("737-800", maker, AircraftType.PASSENGER,
                41140, 79016, 62732, 20894, 12500, 230, 34.3, 125.0, 0.026, 1.5, engine);
        final CabinConfiguration cabin = new CabinConfiguration(0, 20, 150);
        return new Aircraft(registration, "Portugal", 6, 2018, cabin, model);
    }

    @Test
    void ensureAircraftCanBeAddedToCompanyFleet() {
        final AirTransportCompany company =
                new AirTransportCompany("TAP Air Portugal", IATACode.valueOf("TP"), ICAOCode.valueOf("TAP"));
        final Aircraft aircraft = validAircraft("CS-TUA");
        company.addAircraftToFleet(aircraft);
        assertTrue(company.fleet().contains("CS-TUA"));
        assertEquals(1, company.fleet().size());
    }

    @Test
    void ensureDuplicateAircraftRegistrationCannotBeAddedToFleet() {
        final AirTransportCompany company =
                new AirTransportCompany("TAP Air Portugal", IATACode.valueOf("TP"), ICAOCode.valueOf("TAP"));
        final Aircraft aircraft = validAircraft("CS-TUA");
        company.addAircraftToFleet(aircraft);
        company.addAircraftToFleet(aircraft);
        assertEquals(1, company.fleet().size());
    }
}
