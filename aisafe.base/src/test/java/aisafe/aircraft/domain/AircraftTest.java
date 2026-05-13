package aisafe.aircraft.domain;

import aisafe.aircraftmodel.domain.AircraftModel;
import aisafe.aircraftmodel.domain.AircraftType;
import aisafe.enginemodel.domain.EngineModel;
import aisafe.enginemodel.domain.EngineType;
import aisafe.maker.domain.Maker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AircraftTest {

    private static AircraftModel validModel() {
        final Maker maker = new Maker("Boeing", "USA");
        final EngineModel engine = new EngineModel("CFM56", "CFM International", EngineType.TURBOFAN, 120.0, 0.35);
        return new AircraftModel("737-800", maker, AircraftType.PASSENGER,
                41140, 79016, 62732, 20894,
                12500, 230, 34.3, 125.0,
                0.026, 1.5, engine);
    }

    private static AircraftModel validModelWithCapacity(final int maxCapacity) {
        return validModel().withMaxCapacity(maxCapacity);
    }

    private static CabinConfiguration validCabin() {
        return new CabinConfiguration(8, 20, 150);
    }

    @Test
    void ensureValidAircraftIsCreatedSuccessfully() {
        final Aircraft aircraft = new Aircraft("CS-TUA", "Portugal", 6, 2018, validCabin(), validModel());
        assertEquals("CS-TUA", aircraft.registrationNumber());
        assertEquals("Portugal", aircraft.registeredCountry());
        assertEquals(6, aircraft.numberOfCrewElements());
    }

    @Test
    void ensureAircraftIsCreatedWithActiveOperationalStatus() {
        final Aircraft aircraft = new Aircraft("CS-TUA", "Portugal", 6, 2018, validCabin(), validModel());
        assertEquals(OperationalStatus.ACTIVE, aircraft.operationalStatus());
    }

    @Test
    void ensureAircraftRegistrationIsNormalisedToUpperCase() {
        final Aircraft aircraft = new Aircraft("cs-tua", "Portugal", 6, 2018, validCabin(), validModel());
        assertEquals("CS-TUA", aircraft.registrationNumber());
    }

    @Test
    void ensureAircraftMustHaveValidRegistrationNumber() {
        assertThrows(IllegalArgumentException.class,
                () -> new Aircraft(null, "Portugal", 6, 2018, validCabin(), validModel()));
    }

    @Test
    void ensureAircraftRegistrationCannotBeEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> new Aircraft("  ", "Portugal", 6, 2018, validCabin(), validModel()));
    }

    @Test
    void ensureAircraftMustBeRegisteredToACountry() {
        assertThrows(IllegalArgumentException.class,
                () -> new Aircraft("CS-TUA", "  ", 6, 2018, validCabin(), validModel()));
    }

    @Test
    void ensureAircraftCannotExceedModelMaximumCapacity() {
        final AircraftModel model = validModelWithCapacity(100);
        final CabinConfiguration oversized = new CabinConfiguration(0, 0, 150);
        assertThrows(IllegalArgumentException.class,
                () -> new Aircraft("CS-TUA", "Portugal", 6, 2018, oversized, model));
    }

    @Test
    void ensureAircraftWithExactModelCapacityIsAccepted() {
        final AircraftModel model = validModelWithCapacity(189);
        final CabinConfiguration cabin = new CabinConfiguration(0, 0, 189);
        final Aircraft aircraft = new Aircraft("CS-TUA", "Portugal", 6, 2018, cabin, model);
        assertEquals(189, aircraft.cabinConfiguration().totalSeats());
    }

    @Test
    void ensureZeroCrewElementsThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new Aircraft("CS-TUA", "Portugal", 0, 2018, validCabin(), validModel()));
    }

    @Test
    void ensureNullCabinConfigurationThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new Aircraft("CS-TUA", "Portugal", 6, 2018, null, validModel()));
    }

    @Test
    void ensureNullAircraftModelThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new Aircraft("CS-TUA", "Portugal", 6, 2018, validCabin(), null));
    }

    @Test
    void ensureYearOfManufactureIsStored() {
        final Aircraft aircraft = new Aircraft("CS-TUA", "Portugal", 6, 2015, validCabin(), validModel());
        assertEquals(2015, aircraft.yearOfManufacture());
    }

    @Test
    void ensureYearOfManufactureBefore1900IsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Aircraft("CS-TUA", "Portugal", 6, 1899, validCabin(), validModel()));
    }

    @Test
    void ensureYearOfManufactureInFutureIsRejected() {
        final int futureYear = java.time.Year.now().getValue() + 1;
        assertThrows(IllegalArgumentException.class,
                () -> new Aircraft("CS-TUA", "Portugal", 6, futureYear, validCabin(), validModel()));
    }
    @Test
    void ensureDecommissionChangesStatus() {
        final Aircraft aircraft = new Aircraft("CS-TUA", "Portugal", 6, 2018, validCabin(), validModel());
        aircraft.decommission();
        assertEquals(OperationalStatus.DECOMMISSIONED, aircraft.operationalStatus());
    }

    @Test
    void ensureCannotDecommissionAlreadyDecommissioned() {
        final Aircraft aircraft = new Aircraft("CS-TUA", "Portugal", 6, 2018, validCabin(), validModel());
        aircraft.decommission();
        assertThrows(IllegalStateException.class, aircraft::decommission);
    }

    @Test
    void ensureIsActiveReturnsTrueForActiveAircraft() {
        final Aircraft aircraft = new Aircraft("CS-TUA", "Portugal", 6, 2018, validCabin(), validModel());
        assertTrue(aircraft.isActive());
    }

    @Test
    void ensureIsActiveReturnsFalseAfterDecommission() {
        final Aircraft aircraft = new Aircraft("CS-TUA", "Portugal", 6, 2018, validCabin(), validModel());
        aircraft.decommission();
        assertFalse(aircraft.isActive());
    }

    @Test
    void ensureTwoAircraftWithSameRegistrationAreEqual() {
        final Aircraft a = new Aircraft("CS-TUA", "Portugal", 6, 2018, validCabin(), validModel());
        final Aircraft b = new Aircraft("CS-TUA", "Spain", 4, 2018, validCabin(), validModel());
        assertEquals(a, b);
    }

    @Test
    void ensureTwoAircraftWithDifferentRegistrationAreNotEqual() {
        final Aircraft a = new Aircraft("CS-TUA", "Portugal", 6, 2018, validCabin(), validModel());
        final Aircraft b = new Aircraft("CS-TUB", "Portugal", 6, 2018, validCabin(), validModel());
        assertNotEquals(a, b);
    }

    @Test
    void ensureGettersReturnCorrectValues() {
        final AircraftModel model = validModel();
        final CabinConfiguration cabin = validCabin();
        final Aircraft aircraft = new Aircraft("CS-TUA", "Portugal", 6, 2018, cabin, model);
        assertEquals(cabin, aircraft.cabinConfiguration());
        assertEquals(model, aircraft.aircraftModel());
        assertEquals("Portugal", aircraft.registeredCountry());
        assertEquals(6, aircraft.numberOfCrewElements());
    }

    @Test
    void ensureIdentityReturnsRegistrationNumber() {
        final Aircraft aircraft = new Aircraft("CS-TUA", "Portugal", 6, 2018, validCabin(), validModel());
        assertEquals("CS-TUA", aircraft.identity());
    }

    @Test
    void ensureToStringContainsRegistrationNumber() {
        final Aircraft aircraft = new Aircraft("CS-TUA", "Portugal", 6, 2018, validCabin(), validModel());
        assertTrue(aircraft.toString().contains("CS-TUA"));
    }

    @Test
    void ensureEqualsReturnsTrueForSameInstance() {
        final Aircraft aircraft = new Aircraft("CS-TUA", "Portugal", 6, 2018, validCabin(), validModel());
        assertEquals(aircraft, aircraft);
    }

    @Test
    void ensureEqualsReturnsFalseForNull() {
        final Aircraft aircraft = new Aircraft("CS-TUA", "Portugal", 6, 2018, validCabin(), validModel());
        assertNotEquals(null, aircraft);
    }

    @Test
    void ensureSameAsReturnsTrueForSameInstance() {
        final Aircraft aircraft = new Aircraft("CS-TUA", "Portugal", 6, 2018, validCabin(), validModel());
        assertTrue(aircraft.sameAs(aircraft));
    }

    @Test
    void ensureHashCodeIsConsistent() {
        final Aircraft aircraft = new Aircraft("CS-TUA", "Portugal", 6, 2018, validCabin(), validModel());
        assertEquals(aircraft.hashCode(), aircraft.hashCode());
    }
}
