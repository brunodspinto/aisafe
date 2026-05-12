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
        final Aircraft aircraft = new Aircraft("CS-TUA", "Portugal", 6, validCabin(), validModel());
        assertEquals("CS-TUA", aircraft.registrationNumber());
        assertEquals("Portugal", aircraft.registeredCountry());
        assertEquals(6, aircraft.numberOfCrewElements());
    }

    @Test
    void ensureAircraftIsCreatedWithActiveOperationalStatus() {
        final Aircraft aircraft = new Aircraft("CS-TUA", "Portugal", 6, validCabin(), validModel());
        assertEquals(OperationalStatus.ACTIVE, aircraft.operationalStatus());
    }

    @Test
    void ensureAircraftRegistrationIsNormalisedToUpperCase() {
        final Aircraft aircraft = new Aircraft("cs-tua", "Portugal", 6, validCabin(), validModel());
        assertEquals("CS-TUA", aircraft.registrationNumber());
    }

    @Test
    void ensureAircraftMustHaveValidRegistrationNumber() {
        assertThrows(IllegalArgumentException.class,
                () -> new Aircraft(null, "Portugal", 6, validCabin(), validModel()));
    }

    @Test
    void ensureAircraftRegistrationCannotBeEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> new Aircraft("  ", "Portugal", 6, validCabin(), validModel()));
    }

    @Test
    void ensureAircraftMustBeRegisteredToACountry() {
        assertThrows(IllegalArgumentException.class,
                () -> new Aircraft("CS-TUA", "  ", 6, validCabin(), validModel()));
    }

    @Test
    void ensureAircraftCannotExceedModelMaximumCapacity() {
        final AircraftModel model = validModelWithCapacity(100);
        final CabinConfiguration oversized = new CabinConfiguration(0, 0, 150);
        assertThrows(IllegalArgumentException.class,
                () -> new Aircraft("CS-TUA", "Portugal", 6, oversized, model));
    }

    @Test
    void ensureAircraftWithExactModelCapacityIsAccepted() {
        final AircraftModel model = validModelWithCapacity(189);
        final CabinConfiguration cabin = new CabinConfiguration(0, 0, 189);
        final Aircraft aircraft = new Aircraft("CS-TUA", "Portugal", 6, cabin, model);
        assertEquals(189, aircraft.cabinConfiguration().totalSeats());
    }

    @Test
    void ensureZeroCrewElementsThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new Aircraft("CS-TUA", "Portugal", 0, validCabin(), validModel()));
    }

    @Test
    void ensureNullCabinConfigurationThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new Aircraft("CS-TUA", "Portugal", 6, null, validModel()));
    }

    @Test
    void ensureNullAircraftModelThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new Aircraft("CS-TUA", "Portugal", 6, validCabin(), null));
    }
}
