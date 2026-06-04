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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@code ConsultWeatherDataController} (US043).
 * Uses in-memory repositories and real authentication infrastructure.
 */
class ConsultWeatherDataControllerTest {

    private static final String WEATHER_PERSON_USERNAME = "weather-us043";
    private static final String WEATHER_PERSON_PASSWORD = "Password1";
    private static final String PILOT_USERNAME = "pilot-us043";
    private static final String PILOT_PASSWORD = "Password1";
    private static final String FCO_USERNAME = "fco-us043";
    private static final String FCO_PASSWORD = "Password1";
    private static final String BACKOFFICE_USERNAME = "bo-us043";
    private static final String BACKOFFICE_PASSWORD = "Password1";

    private final ConsultWeatherDataController controller = new ConsultWeatherDataController();

    @BeforeAll
    static void configureAuthz() {
        AuthzRegistry.configure(
                PersistenceContext.repositories().systemUsers(),
                new AiSafePasswordPolicy(),
                new PlainTextEncoder());

        ensureUserExists(WEATHER_PERSON_USERNAME, WEATHER_PERSON_PASSWORD,
                "Weather", "Person", "weather-us043@aisafe.com", AiSafeRoles.WEATHER_PERSON);
        ensureUserExists(PILOT_USERNAME, PILOT_PASSWORD,
                "Pilot", "User", "pilot-us043@aisafe.com", AiSafeRoles.PILOT);
        ensureUserExists(FCO_USERNAME, FCO_PASSWORD,
                "Flight", "Controller", "fco-us043@aisafe.com", AiSafeRoles.FLIGHT_CONTROL_OPERATOR);
        ensureUserExists(BACKOFFICE_USERNAME, BACKOFFICE_PASSWORD,
                "Backoffice", "Operator", "bo-us043@aisafe.com", AiSafeRoles.BACKOFFICE_OPERATOR);
    }

    @AfterEach
    void tearDown() {
        AuthenticationContext.clear();
    }

    // AC043.1 - records are queried by day and air control area
    @Test
    void ensureWeatherDataCanBeQueriedByDateAndArea() {
        AuthenticationContext.authenticate(WEATHER_PERSON_USERNAME, WEATHER_PERSON_PASSWORD);
        ensureAreaExists("T43-A");
        saveWeatherData("T43-A", LocalDateTime.of(2026, 6, 4, 10, 0), 20.0);
        saveWeatherData("T43-A", LocalDateTime.of(2026, 6, 4, 18, 30), 18.0);
        saveWeatherData("T43-A", LocalDateTime.of(2026, 6, 5, 10, 0), 22.0);
        saveWeatherData("T43-B", LocalDateTime.of(2026, 6, 4, 10, 0), 25.0);

        final List<WeatherData> result = toList(controller.consultWeatherData(
                LocalDate.of(2026, 6, 4), "T43-A"));

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(record -> record.areaCode().equals("T43-A")));
        assertTrue(result.stream().allMatch(record -> record.date().toLocalDate().equals(LocalDate.of(2026, 6, 4))));
    }

    // AC043.1b - date/area combinations with no records return an empty result
    @Test
    void ensureEmptyResultIsReturnedWhenNoWeatherDataExists() {
        AuthenticationContext.authenticate(WEATHER_PERSON_USERNAME, WEATHER_PERSON_PASSWORD);
        ensureAreaExists("T43-C");

        final List<WeatherData> result = toList(controller.consultWeatherData(
                LocalDate.of(2026, 7, 1), "T43-C"));

        assertTrue(result.isEmpty());
    }

    // AC043.2 - returned records include all relevant meteorological fields
    @Test
    void ensureReturnedWeatherDataIncludesMeteorologicalInformation() {
        AuthenticationContext.authenticate(PILOT_USERNAME, PILOT_PASSWORD);
        ensureAreaExists("T43-D");
        saveWeatherData("T43-D", LocalDateTime.of(2026, 8, 10, 9, 15), 16.5);

        final WeatherData record = toList(controller.consultWeatherData(
                LocalDate.of(2026, 8, 10), "T43-D")).get(0);

        assertEquals("T43-D", record.areaCode());
        assertEquals("IPMA", record.source().provider());
        assertEquals("CSV", record.source().format());
        assertEquals(LocalDateTime.of(2026, 8, 10, 9, 15), record.date());
        assertEquals(16.5, record.temperature());
        assertEquals(12.0, record.windSpeed());
        assertEquals("NE", record.windDirection());
        assertEquals(1012.0, record.pressure());
        assertEquals(9.5, record.visibility());
    }

    // AC043.3 - Flight Control Operator is authorized to consult weather data
    @Test
    void ensureFlightControlOperatorCanConsultWeatherData() {
        AuthenticationContext.authenticate(FCO_USERNAME, FCO_PASSWORD);
        ensureAreaExists("T43-E");

        final Iterable<AirControlArea> areas = controller.activeAirControlAreas();

        assertTrue(toList(areas).stream().anyMatch(area -> area.areaCode().toString().equals("T43-E")));
    }

    // AC043.3 - unrelated roles cannot consult weather data
    @Test
    void ensureUnauthorizedUserCannotConsultWeatherData() {
        AuthenticationContext.authenticate(BACKOFFICE_USERNAME, BACKOFFICE_PASSWORD);

        assertThrows(UnauthorizedException.class,
                () -> controller.consultWeatherData(LocalDate.of(2026, 6, 4), "T43-A"));
    }

    @Test
    void ensureUnknownAreaCodeIsRejected() {
        AuthenticationContext.authenticate(WEATHER_PERSON_USERNAME, WEATHER_PERSON_PASSWORD);

        assertThrows(IllegalArgumentException.class,
                () -> controller.consultWeatherData(LocalDate.of(2026, 6, 4), "T43-UNKNOWN"));
    }

    private static void ensureAreaExists(final String code) {
        final var repo = PersistenceContext.repositories().airControlAreas();
        final AirControlAreaCode areaCode = AirControlAreaCode.valueOf(code);
        if (repo.ofIdentity(areaCode).isEmpty()) {
            repo.save(new AirControlArea(
                    areaCode,
                    "Test Area " + code,
                    500.0,
                    new GeoBoundary(42.5, 36.5, -6.0, -10.0)));
        }
    }

    private static void saveWeatherData(final String areaCode, final LocalDateTime date,
                                        final double temperature) {
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
