package aisafe.weatherdata.domain;

import aisafe.aircontrolarea.domain.AirControlAreaCode;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@code WeatherData} domain object.
 * Verifies construction and weather-attribute validation.
 */
class WeatherDataTest {

    private static final LocalDateTime VALID_DATE = LocalDateTime.of(2025, 5, 1, 12, 0);
    private static final WeatherSource VALID_SOURCE = new WeatherSource("IPMA", "JSON");

    private static final AirControlAreaCode VALID_AREA_CODE = AirControlAreaCode.valueOf("PT-N");

    private WeatherData validWeatherData() {
        return new WeatherData(VALID_AREA_CODE, VALID_SOURCE, VALID_DATE, 20.0, 15.0, "N", 1013.0, 10.0);
    }

    @Test
    void ensureValidWeatherDataCanBeCreated() {
        final WeatherData wd = validWeatherData();

        assertEquals("PT-N", wd.areaCode());
        assertEquals(VALID_SOURCE, wd.source());
        assertEquals(VALID_DATE, wd.date());
        assertEquals(20.0, wd.temperature());
        assertEquals(15.0, wd.windSpeed());
        assertEquals("N", wd.windDirection());
        assertEquals(1013.0, wd.pressure());
        assertEquals(10.0, wd.visibility());
    }

    @Test
    void ensureAreaCodeIsNormalisedToUpperCase() {
        final WeatherData wd = new WeatherData(AirControlAreaCode.valueOf("pt-n"), VALID_SOURCE, VALID_DATE, 20.0, 15.0, "N", 1013.0, 10.0);
        assertEquals("PT-N", wd.areaCode());
    }

    @Test
    void ensureAreaCodeCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData(null, VALID_SOURCE, VALID_DATE, 20.0, 15.0, "N", 1013.0, 10.0));
    }

    @Test
    void ensureSourceCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData(VALID_AREA_CODE, null, VALID_DATE, 20.0, 15.0, "N", 1013.0, 10.0));
    }

    @Test
    void ensureDateCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData(VALID_AREA_CODE, VALID_SOURCE, null, 20.0, 15.0, "N", 1013.0, 10.0));
    }

    @Test
    void ensureWindSpeedCannotBeNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData(VALID_AREA_CODE, VALID_SOURCE, VALID_DATE, 20.0, -1.0, "N", 1013.0, 10.0));
    }

    @Test
    void ensureVisibilityCannotBeNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData(VALID_AREA_CODE, VALID_SOURCE, VALID_DATE, 20.0, 15.0, "N", 1013.0, -1.0));
    }

    @Test
    void ensureWindSpeedOfZeroIsValid() {
        final WeatherData wd = new WeatherData(VALID_AREA_CODE, VALID_SOURCE, VALID_DATE, 20.0, 0.0, "N", 1013.0, 10.0);
        assertEquals(0.0, wd.windSpeed());
    }

    @Test
    void ensureVisibilityOfZeroIsValid() {
        final WeatherData wd = new WeatherData(VALID_AREA_CODE, VALID_SOURCE, VALID_DATE, 20.0, 15.0, "N", 1013.0, 0.0);
        assertEquals(0.0, wd.visibility());
    }

    @Test
    void ensureWindDirectionCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData(VALID_AREA_CODE, VALID_SOURCE, VALID_DATE, 20.0, 15.0, null, 1013.0, 10.0));
    }

    @Test
    void ensureWindDirectionCannotBeBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData(VALID_AREA_CODE, VALID_SOURCE, VALID_DATE, 20.0, 15.0, "   ", 1013.0, 10.0));
    }

    @Test
    void ensureWindDirectionMustBeValidCompassDirection() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherData(VALID_AREA_CODE, VALID_SOURCE, VALID_DATE, 20.0, 15.0, "NORTHEAST", 1013.0, 10.0));

        final WeatherData wd = new WeatherData(VALID_AREA_CODE, VALID_SOURCE, VALID_DATE, 20.0, 15.0, "NE", 1013.0, 10.0);
        assertEquals("NE", wd.windDirection());
    }
}
