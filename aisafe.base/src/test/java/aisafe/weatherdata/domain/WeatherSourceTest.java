package aisafe.weatherdata.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WeatherSourceTest {

    @Test
    void ensureValidWeatherSourceCanBeCreated() {
        final WeatherSource ws = new WeatherSource("IPMA", "JSON");
        assertEquals("IPMA", ws.provider());
        assertEquals("JSON", ws.format());
    }

    @Test
    void ensureProviderCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> new WeatherSource(null, "JSON"));
    }

    @Test
    void ensureProviderCannotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> new WeatherSource("   ", "JSON"));
    }

    @Test
    void ensureFormatCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> new WeatherSource("IPMA", null));
    }

    @Test
    void ensureFormatCannotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> new WeatherSource("IPMA", "   "));
    }

    @Test
    void ensureEqualityForSameValues() {
        final WeatherSource ws1 = new WeatherSource("IPMA", "JSON");
        final WeatherSource ws2 = new WeatherSource("IPMA", "JSON");
        assertEquals(ws1, ws2);
        assertEquals(ws1.hashCode(), ws2.hashCode());
    }

    @Test
    void ensureInequalityForDifferentProvider() {
        assertNotEquals(new WeatherSource("IPMA", "JSON"), new WeatherSource("METAR", "JSON"));
    }

    @Test
    void ensureInequalityForDifferentFormat() {
        assertNotEquals(new WeatherSource("IPMA", "JSON"), new WeatherSource("IPMA", "XML"));
    }

    @Test
    void ensureProviderIsStoredTrimmed() {
        final WeatherSource ws = new WeatherSource("  IPMA  ", "JSON");
        assertEquals("IPMA", ws.provider());
    }

    @Test
    void ensureFormatIsStoredTrimmed() {
        final WeatherSource ws = new WeatherSource("IPMA", "  JSON  ");
        assertEquals("JSON", ws.format());
    }
}
