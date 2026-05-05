package aisafe.aircontrolarea.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GeoBoundaryTest {

    @Test
    public void ensureValidGeoBoundaryIsCreatedSuccessfully() {
        // Arrange & Act
        GeoBoundary boundary = new GeoBoundary(45.0, 30.0, 10.0, -10.0);
        
        // Assert
        assertNotNull(boundary);
        assertEquals(45.0, boundary.northLatitude(), 0.001);
        assertEquals(30.0, boundary.southLatitude(), 0.001);
    }

    @Test
    public void ensureNorthLatitudeCannotBeGreaterThan90() {
        assertThrows(IllegalArgumentException.class, () -> new GeoBoundary(91.0, 30.0, 10.0, -10.0));
    }

    @Test
    public void ensureSouthLatitudeCannotBeLessThanMinus90() {
        assertThrows(IllegalArgumentException.class, () -> new GeoBoundary(45.0, -91.0, 10.0, -10.0));
    }

    @Test
    public void ensureNorthLatitudeMustBeStrictlyGreaterThanSouthLatitude() {
        // Norte é menor que o Sul, o que é geograficamente impossível
        assertThrows(IllegalArgumentException.class, () -> new GeoBoundary(20.0, 40.0, 10.0, -10.0));
    }

    @Test
    public void ensureNorthLatitudeCannotBeEqualToSouthLatitude() {
        // Se forem iguais, é uma linha e não uma área
        assertThrows(IllegalArgumentException.class, () -> new GeoBoundary(30.0, 30.0, 10.0, -10.0));
    }

    @Test
    public void ensureEastLongitudeCannotBeInvalid() {
        assertThrows(IllegalArgumentException.class, () -> new GeoBoundary(45.0, 30.0, 181.0, -10.0));
    }

    @Test
    public void ensureWestLongitudeCannotBeInvalid() {
        assertThrows(IllegalArgumentException.class, () -> new GeoBoundary(45.0, 30.0, 10.0, -181.0));
    }
}
