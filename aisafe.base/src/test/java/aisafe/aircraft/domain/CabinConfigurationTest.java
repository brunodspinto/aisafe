package aisafe.aircraft.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link CabinConfiguration} value object.
 * Verifies construction validation and total-seat-count constraints.
 */
class CabinConfigurationTest {

    @Test
    void ensureValidCabinConfigurationIsCreatedSuccessfully() {
        final CabinConfiguration cabin = new CabinConfiguration(8, 20, 150);
        assertEquals(8, cabin.firstClassSeats());
        assertEquals(20, cabin.businessClassSeats());
        assertEquals(150, cabin.economyClassSeats());
        assertEquals(178, cabin.totalSeats());
    }

    @Test
    void ensureCabinConfigurationCannotHaveNegativeFirstClassSeats() {
        assertThrows(IllegalArgumentException.class,
                () -> new CabinConfiguration(-1, 20, 150));
    }

    @Test
    void ensureCabinConfigurationCannotHaveNegativeBusinessClassSeats() {
        assertThrows(IllegalArgumentException.class,
                () -> new CabinConfiguration(8, -1, 150));
    }

    @Test
    void ensureCabinConfigurationCannotHaveNegativeEconomyClassSeats() {
        assertThrows(IllegalArgumentException.class,
                () -> new CabinConfiguration(8, 20, -1));
    }

    @Test
    void ensureTotalSeatsIsCalculatedCorrectly() {
        final CabinConfiguration cabin = new CabinConfiguration(4, 12, 100);
        assertEquals(4 + 12 + 100, cabin.totalSeats());
    }

    @Test
    void ensureCabinWithZeroAllSeatsThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new CabinConfiguration(0, 0, 0));
    }

    @Test
    void ensureEconomyOnlyConfigurationIsValid() {
        final CabinConfiguration cabin = new CabinConfiguration(0, 0, 189);
        assertEquals(189, cabin.totalSeats());
    }
}
