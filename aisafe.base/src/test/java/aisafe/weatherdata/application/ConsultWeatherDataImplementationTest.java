package aisafe.weatherdata.application;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.aircontrolarea.domain.AirControlAreaCode;
import aisafe.aircontrolarea.domain.GeoBoundary;
import aisafe.auth.AuthenticationContext;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafePasswordPolicy;
import aisafe.usermanagement.domain.AiSafeRoles;
import aisafe.weatherdata.domain.WeatherData;
import aisafe.weatherdata.domain.WeatherSource;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.exceptions.UnauthorizedException;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;
import eapli.framework.infrastructure.authz.domain.model.Username;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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
 * Implementation tests for US043 - Consult Weather Data.
 *
 * <p>These tests exercise the full application flow behind the console UI:
 * authentication, air control area listing, date/area query, returned meteorological
 * fields, and role restriction.</p>
 */
class ConsultWeatherDataImplementationTest {

    private static final String WEATHER_PERSON_USERNAME = "weather-flow-us043";
    private static final String WEATHER_PERSON_PASSWORD = "Password1";
    private static final String PILOT_USERNAME = "pilot-flow-us043";
    private static final String PILOT_PASSWORD = "Password1";
    private static final String BACKOFFICE_USERNAME = "bo-flow-us043";
    private static final String BACKOFFICE_PASSWORD = "Password1";
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private final ConsultWeatherDataController controller = new ConsultWeatherDataController();

    @BeforeAll
    static void configureAuthz() {
        AuthzRegistry.configure(
                PersistenceContext.repositories().systemUsers(),
                new AiSafePasswordPolicy(),
                new PlainTextEncoder());

        ensureUserExists(WEATHER_PERSON_USERNAME, WEATHER_PERSON_PASSWORD,
                "Weather", "Flow", "weather-flow-us043@aisafe.com", AiSafeRoles.WEATHER_PERSON);
        ensureUserExists(PILOT_USERNAME, PILOT_PASSWORD,
                "Pilot", "Flow", "pilot-flow-us043@aisafe.com", AiSafeRoles.PILOT);
        ensureUserExists(BACKOFFICE_USERNAME, BACKOFFICE_PASSWORD,
                "Backoffice", "Flow", "bo-flow-us043@aisafe.com", AiSafeRoles.BACKOFFICE_OPERATOR);
    }

    @AfterEach
    void tearDown() {
        AuthenticationContext.clear();
    }

    @Test
    void ensureWeatherPersonCanRunTheWholeConsultWeatherDataFlow() {
        AuthenticationContext.authenticate(WEATHER_PERSON_USERNAME, WEATHER_PERSON_PASSWORD);
        final String areaCode = nextAreaCode("FLOW");
        final String otherAreaCode = nextAreaCode("OTHER");
        ensureAreaExists(areaCode);
        saveWeatherData(areaCode, LocalDateTime.of(2026, 6, 8, 9, 0), 17.5);
        saveWeatherData(areaCode, LocalDateTime.of(2026, 6, 8, 15, 30), 21.0);
        saveWeatherData(areaCode, LocalDateTime.of(2026, 6, 9, 9, 0), 22.0);
        saveWeatherData(otherAreaCode, LocalDateTime.of(2026, 6, 8, 9, 0), 12.0);

        final List<AirControlArea> availableAreas = toList(controller.activeAirControlAreas());
        assertTrue(availableAreas.stream()
                .anyMatch(area -> area.areaCode().toString().equals(areaCode)));

        final List<WeatherData> records = toList(controller.consultWeatherData(
                LocalDate.of(2026, 6, 8), areaCode));

        assertEquals(2, records.size());
        assertTrue(records.stream().allMatch(record -> record.areaCode().equals(areaCode)));
        assertTrue(records.stream().allMatch(record ->
                record.date().toLocalDate().equals(LocalDate.of(2026, 6, 8))));

        final WeatherData firstRecord = records.get(0);
        assertEquals("IPMA", firstRecord.source().provider());
        assertEquals("CSV", firstRecord.source().format());
        assertEquals(17.5, firstRecord.temperature());
        assertEquals(12.0, firstRecord.windSpeed());
        assertEquals("NE", firstRecord.windDirection());
        assertEquals(1012.0, firstRecord.pressure());
        assertEquals(9.5, firstRecord.visibility());
    }

    @Test
    void ensurePilotCanRunTheConsultWeatherDataFlow() {
        AuthenticationContext.authenticate(PILOT_USERNAME, PILOT_PASSWORD);
        final String areaCode = nextAreaCode("PILOT");
        ensureAreaExists(areaCode);
        saveWeatherData(areaCode, LocalDateTime.of(2026, 6, 8, 10, 0), 19.0);

        final List<AirControlArea> availableAreas = toList(controller.activeAirControlAreas());
        final List<WeatherData> records = toList(controller.consultWeatherData(
                LocalDate.of(2026, 6, 8), areaCode));

        assertTrue(availableAreas.stream()
                .anyMatch(area -> area.areaCode().toString().equals(areaCode)));
        assertEquals(1, records.size());
        assertEquals(areaCode, records.get(0).areaCode());
    }

    @Test
    void ensureUnauthorizedUserCannotRunTheConsultWeatherDataFlow() {
        AuthenticationContext.authenticate(BACKOFFICE_USERNAME, BACKOFFICE_PASSWORD);

        assertThrows(UnauthorizedException.class, controller::activeAirControlAreas);
        assertThrows(UnauthorizedException.class,
                () -> controller.consultWeatherData(LocalDate.of(2026, 6, 8), "T43-FLOW"));
    }

    private static String nextAreaCode(final String prefix) {
        return "T43-" + prefix + "-" + SEQUENCE.incrementAndGet();
    }

    private static void ensureAreaExists(final String code) {
        final var repo = PersistenceContext.repositories().airControlAreas();
        final AirControlAreaCode areaCode = AirControlAreaCode.valueOf(code);
        if (repo.ofIdentity(areaCode).isEmpty()) {
            repo.save(new AirControlArea(
                    areaCode,
                    "Implementation Area " + code,
                    500.0,
                    new GeoBoundary(42.5, 36.5, -6.0, -10.0)));
        }
    }

    private static void saveWeatherData(final String areaCode, final LocalDateTime date,
                                        final double temperature) {
        ensureAreaExists(areaCode);
        PersistenceContext.repositories().weatherData().save(new WeatherData(
                AirControlAreaCode.valueOf(areaCode),
                new WeatherSource("IPMA", "CSV"),
                date,
                temperature,
                12.0,
                "NE",
                1012.0,
                9.5));
    }

    private static <T> List<T> toList(final Iterable<T> iterable) {
        final List<T> result = new ArrayList<>();
        iterable.forEach(result::add);
        return result;
    }

    private static void ensureUserExists(final String username, final String password,
                                         final String firstName, final String lastName,
                                         final String email, final Role role) {
        final var repo = PersistenceContext.repositories().systemUsers();
        if (repo.ofIdentity(Username.valueOf(username)).isPresent()) {
            return;
        }
        final var builder = new SystemUserBuilder(new AiSafePasswordPolicy(), new PlainTextEncoder());
        builder.withUsername(username)
                .withPassword(password)
                .withName(firstName, lastName)
                .withEmail(email)
                .withRoles(role);
        repo.save(builder.build());
    }
}
