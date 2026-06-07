package aisafe.weatherdata.application;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.aircontrolarea.domain.AirControlAreaCode;
import aisafe.aircontrolarea.domain.GeoBoundary;
import aisafe.infrastructure.persistence.inmemory.InMemoryAirControlAreaRepository;
import aisafe.infrastructure.persistence.inmemory.InMemoryWeatherDataRepository;
import aisafe.weatherdata.domain.WeatherData;
import aisafe.weatherdata.domain.WeatherSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ConsultWeatherDataController} (US043).
 *
 * <p>These tests inject in-memory repositories and a local authorization guard, avoiding
 * {@code PersistenceContext}, JPA, and the real authentication infrastructure.</p>
 */
class ConsultWeatherDataControllerUnitTest {

    private InMemoryAirControlAreaRepository areaRepository;
    private InMemoryWeatherDataRepository weatherDataRepository;
    private AtomicInteger authorizationCalls;
    private ConsultWeatherDataController controller;

    @BeforeEach
    void setUp() throws Exception {
        resetInMemoryRepositories();
        areaRepository = new InMemoryAirControlAreaRepository();
        weatherDataRepository = new InMemoryWeatherDataRepository();
        authorizationCalls = new AtomicInteger();
        controller = new ConsultWeatherDataController(
                areaRepository,
                weatherDataRepository,
                authorizationCalls::incrementAndGet);
    }

    @Test
    void activeAirControlAreasReturnsRepositoryAreas() {
        final AirControlArea area = area("T43-U1");
        areaRepository.save(area);

        final List<AirControlArea> result = toList(controller.activeAirControlAreas());

        assertEquals(1, result.size());
        assertTrue(result.contains(area));
        assertEquals(1, authorizationCalls.get());
    }

    @Test
    void consultWeatherDataReturnsOnlySelectedDateAndArea() {
        areaRepository.save(area("T43-U2"));
        weatherDataRepository.save(weather("T43-U2", LocalDateTime.of(2026, 6, 4, 8, 0), 18.0));
        weatherDataRepository.save(weather("T43-U2", LocalDateTime.of(2026, 6, 4, 16, 0), 20.0));
        weatherDataRepository.save(weather("T43-U2", LocalDateTime.of(2026, 6, 5, 8, 0), 21.0));
        weatherDataRepository.save(weather("T43-U3", LocalDateTime.of(2026, 6, 4, 8, 0), 22.0));

        final List<WeatherData> result = toList(controller.consultWeatherData(
                LocalDate.of(2026, 6, 4), "T43-U2"));

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(record -> record.areaCode().equals("T43-U2")));
        assertTrue(result.stream().allMatch(record -> record.date().toLocalDate().equals(LocalDate.of(2026, 6, 4))));
        assertEquals(1, authorizationCalls.get());
    }

    @Test
    void consultWeatherDataRejectsNullDate() {
        areaRepository.save(area("T43-U4"));

        assertThrows(IllegalArgumentException.class,
                () -> controller.consultWeatherData(null, "T43-U4"));
        assertEquals(1, authorizationCalls.get());
    }

    @Test
    void consultWeatherDataRejectsUnknownAreaCode() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.consultWeatherData(LocalDate.of(2026, 6, 4), "T43-UX"));
        assertEquals(1, authorizationCalls.get());
    }

    @Test
    void consultWeatherDataStopsWhenAuthorizationFails() {
        controller = new ConsultWeatherDataController(
                areaRepository,
                weatherDataRepository,
                () -> {
                    throw new IllegalStateException("not authorized");
                });

        assertThrows(IllegalStateException.class,
                () -> controller.consultWeatherData(LocalDate.of(2026, 6, 4), "T43-U5"));
    }

    private static AirControlArea area(final String code) {
        return new AirControlArea(
                AirControlAreaCode.valueOf(code),
                "Unit Area " + code,
                500.0,
                new GeoBoundary(42.5, 36.5, -6.0, -10.0));
    }

    private static WeatherData weather(final String areaCode, final LocalDateTime date,
                                       final double temperature) {
        return new WeatherData(
                AirControlAreaCode.valueOf(areaCode),
                new WeatherSource("IPMA", "CSV"),
                date,
                temperature,
                12.0,
                "NE",
                1012.0,
                9.5);
    }

    private static <T> List<T> toList(final Iterable<T> iterable) {
        final List<T> result = new ArrayList<>();
        iterable.forEach(result::add);
        return result;
    }

    private static void resetInMemoryRepositories() throws Exception {
        final var reset = Class.forName(
                        "eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryRepository")
                .getDeclaredMethod("reset");
        reset.setAccessible(true);
        reset.invoke(null);
    }
}
