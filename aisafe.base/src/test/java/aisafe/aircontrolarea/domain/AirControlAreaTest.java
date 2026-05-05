package aisafe.aircontrolarea.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AirControlAreaTest {

    private final GeoBoundary validBoundaries = new GeoBoundary(40.0, 30.0, -5.0, -15.0);

    @Test
    public void ensureValidAirControlAreaIsCreatedSuccessfully() {
        // Arrange & Act
        AirControlArea area = new AirControlArea("PT-N", "Porto Control", 1500.0, validBoundaries);

        // Assert
        assertNotNull(area);
        assertEquals("PT-N", area.areaCode());
        assertEquals("PT-N", area.identity());
    }

    @Test
    public void ensureAirControlAreaMustHaveValidCode() {
        assertThrows(IllegalArgumentException.class, () -> new AirControlArea(null, "Porto Control", 1500.0, validBoundaries));
    }

    @Test
    public void ensureAirControlAreaCannotHaveEmptyCode() {
        assertThrows(IllegalArgumentException.class, () -> new AirControlArea("   ", "Porto Control", 1500.0, validBoundaries));
    }

    @Test
    public void ensureAirControlAreaMustHaveValidName() {
        assertThrows(IllegalArgumentException.class, () -> new AirControlArea("PT-N", null, 1500.0, validBoundaries));
    }

    @Test
    public void ensureAirControlAreaCannotHaveNegativeFuel() {
        assertThrows(IllegalArgumentException.class, () -> new AirControlArea("PT-N", "Porto Control", -10.0, validBoundaries));
    }

    @Test
    public void ensureAirControlAreaMustHaveBoundaries() {
        assertThrows(IllegalArgumentException.class, () -> new AirControlArea("PT-N", "Porto Control", 1500.0, null));
    }
}
