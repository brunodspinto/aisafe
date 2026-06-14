package aisafe.weatherdata.application;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.aircontrolarea.domain.AirControlAreaCode;
import aisafe.aircontrolarea.domain.GeoBoundary;
import aisafe.auth.AuthenticationContext;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafePasswordPolicy;
import aisafe.usermanagement.domain.AiSafeRoles;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.exceptions.UnauthorizedException;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;
import eapli.framework.infrastructure.authz.domain.model.Username;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@code ImportBulkWeatherDataController} (US042).
 * Uses in-memory repositories and real authentication infrastructure.
 */
class ImportBulkWeatherDataControllerTest {

    private static final String WEATHER_PERSON_USERNAME = "weather-us042";
    private static final String WEATHER_PERSON_PASSWORD = "Password1";
    private static final String BACKOFFICE_USERNAME = "bo-us042";
    private static final String BACKOFFICE_PASSWORD = "Password1";

    private static final String HEADER =
            "area_code,provider,format,date,temperature,windSpeed,windDirection,pressure,visibility";

    private final ImportBulkWeatherDataController controller = new ImportBulkWeatherDataController();
    private final List<Path> tempFiles = new ArrayList<>();

    @BeforeAll
    static void configureAuthz() {
        AuthzRegistry.configure(
                PersistenceContext.repositories().systemUsers(),
                new AiSafePasswordPolicy(),
                new PlainTextEncoder());

        ensureWeatherPersonExists();
        ensureBackofficeOperatorExists();
    }

    @AfterEach
    void tearDown() throws IOException {
        AuthenticationContext.clear();
        for (final Path p : tempFiles) {
            Files.deleteIfExists(p);
        }
    }

    private Path writeTempCsv(final String... lines) throws IOException {
        final Path path = Files.createTempFile("us042-ctrl-test-", ".csv");
        Files.write(path, List.of(lines));
        tempFiles.add(path);
        return path;
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

    // AC042.1 — all valid records are persisted
    @Test
    void ensureAllValidRecordsAreImported() throws Exception {
        AuthenticationContext.authenticate(WEATHER_PERSON_USERNAME, WEATHER_PERSON_PASSWORD);
        ensureAreaExists("PT-N");

        final Path csv = writeTempCsv(
                HEADER,
                "PT-N,IPMA,CSV,2025-05-14T10:00:00,20.0,15.0,N,1013.0,10.0",
                "PT-N,IPMA,CSV,2025-05-14T11:00:00,21.0,12.0,NE,1012.0,9.0");

        final ImportResult result = controller.importWeatherData(csv.toString());

        assertEquals(2, result.saved());
        assertTrue(result.failures().isEmpty());
    }

    // AC042.2 — record with unknown area code is rejected; valid one is still saved
    @Test
    void ensureRecordWithUnknownAreaCodeIsRejected() throws Exception {
        AuthenticationContext.authenticate(WEATHER_PERSON_USERNAME, WEATHER_PERSON_PASSWORD);
        ensureAreaExists("PT-N");

        final Path csv = writeTempCsv(
                HEADER,
                "PT-N,IPMA,CSV,2025-05-14T10:00:00,20.0,15.0,N,1013.0,10.0",
                "XX-UNKNOWN,IPMA,CSV,2025-05-14T11:00:00,21.0,12.0,NE,1012.0,9.0");

        final ImportResult result = controller.importWeatherData(csv.toString());

        assertEquals(1, result.saved());
        assertEquals(1, result.failures().size());
        assertTrue(result.failures().get(0).contains("XX-UNKNOWN"));
    }

    // AC042.3 — only WEATHER_PERSON role may invoke this action
    @Test
    void ensureUnauthorizedUserCannotImport() throws Exception {
        AuthenticationContext.authenticate(BACKOFFICE_USERNAME, BACKOFFICE_PASSWORD);

        final Path csv = writeTempCsv(
                HEADER,
                "PT-N,IPMA,CSV,2025-05-14T10:00:00,20.0,15.0,N,1013.0,10.0");

        assertThrows(UnauthorizedException.class,
                () -> controller.importWeatherData(csv.toString()));
    }

    // AC042.4 — malformed row (invalid domain value) is skipped; valid row is saved
    @Test
    void ensureMalformedRowIsSkippedAndValidRowIsSaved() throws Exception {
        AuthenticationContext.authenticate(WEATHER_PERSON_USERNAME, WEATHER_PERSON_PASSWORD);
        ensureAreaExists("PT-N");

        final Path csv = writeTempCsv(
                HEADER,
                "PT-N,IPMA,CSV,2025-05-14T10:00:00,20.0,-5.0,N,1013.0,10.0",  // negative wind speed
                "PT-N,IPMA,CSV,2025-05-14T11:00:00,21.0,12.0,NE,1012.0,9.0");  // valid

        final ImportResult result = controller.importWeatherData(csv.toString());

        assertEquals(1, result.saved());
        assertEquals(1, result.failures().size());
    }

    // AC042.5 — result reports saved count and per-failure reasons
    @Test
    void ensureImportResultContainsCountAndFailureReasons() throws Exception {
        AuthenticationContext.authenticate(WEATHER_PERSON_USERNAME, WEATHER_PERSON_PASSWORD);
        ensureAreaExists("PT-N");

        final Path csv = writeTempCsv(
                HEADER,
                "PT-N,IPMA,CSV,2025-05-14T10:00:00,20.0,15.0,N,1013.0,10.0",
                "UNKNOWN,IPMA,CSV,2025-05-14T11:00:00,21.0,12.0,NE,1012.0,9.0",
                "PT-N,IPMA,CSV,2025-05-14T12:00:00,21.0,12.0,INVALID,1012.0,9.0"); // invalid wind direction

        final ImportResult result = controller.importWeatherData(csv.toString());

        assertEquals(1, result.saved());
        assertEquals(2, result.failures().size());
        assertFalse(result.failures().get(0).isBlank());
        assertFalse(result.failures().get(1).isBlank());
    }

    // AC042.4 — a row with a blank area code is skipped and reported; valid rows still saved
    // (regression: previously a blank area code aborted the entire import)
    @Test
    void ensureBlankAreaCodeRowIsSkippedAndValidRowIsSaved() throws Exception {
        AuthenticationContext.authenticate(WEATHER_PERSON_USERNAME, WEATHER_PERSON_PASSWORD);
        ensureAreaExists("PT-N");

        final Path csv = writeTempCsv(
                HEADER,
                ",IPMA,CSV,2025-05-14T10:00:00,20.0,15.0,N,1013.0,10.0",        // blank area code
                "PT-N,IPMA,CSV,2025-05-14T11:00:00,21.0,12.0,NE,1012.0,9.0");   // valid

        final ImportResult result = controller.importWeatherData(csv.toString());

        assertEquals(1, result.saved());
        assertEquals(1, result.failures().size());
        assertFalse(result.failures().get(0).isBlank());
    }

    // AC042.5 — edge case: header-only file produces zero saved, zero failures
    @Test
    void ensureHeaderOnlyCsvProducesZeroImports() throws Exception {
        AuthenticationContext.authenticate(WEATHER_PERSON_USERNAME, WEATHER_PERSON_PASSWORD);

        final Path csv = writeTempCsv(HEADER);

        final ImportResult result = controller.importWeatherData(csv.toString());

        assertEquals(0, result.saved());
        assertTrue(result.failures().isEmpty());
    }

    // File-not-found produces a failure entry (not a silent empty result)
    @Test
    void ensureFileNotFoundProducesFailureInResult() {
        AuthenticationContext.authenticate(WEATHER_PERSON_USERNAME, WEATHER_PERSON_PASSWORD);

        final ImportResult result =
                controller.importWeatherData("/nonexistent/path/missing-file.csv");

        assertEquals(0, result.saved());
        assertFalse(result.failures().isEmpty());
    }

    // Blank lines interspersed in CSV are skipped and valid records are still imported
    @Test
    void ensureBlankLinesInCsvAreSkipped() throws Exception {
        AuthenticationContext.authenticate(WEATHER_PERSON_USERNAME, WEATHER_PERSON_PASSWORD);
        ensureAreaExists("PT-N");

        final Path csv = writeTempCsv(
                HEADER,
                "",
                "PT-N,IPMA,CSV,2025-05-14T10:00:00,20.0,15.0,N,1013.0,10.0",
                "",
                "PT-N,IPMA,CSV,2025-05-14T11:00:00,21.0,12.0,NE,1012.0,9.0",
                "");

        final ImportResult result = controller.importWeatherData(csv.toString());

        assertEquals(2, result.saved());
        assertTrue(result.failures().isEmpty());
    }

    // AC042.6 — WeatherDataParser is an interface; any implementation can be substituted
    @Test
    void ensureParserInterfaceCanBeImplementedWithAlternativeFormat() {
        final WeatherDataParser stubParser = filePath -> List.of(
                new ParsedWeatherRecord(
                        "PT-N", "STUB", "JSON",
                        java.time.LocalDateTime.of(2025, 1, 1, 0, 0),
                        10.0, 5.0, "N", 1013.0, 10.0));

        final List<ParsedWeatherRecord> records = stubParser.parse("any-path");
        assertEquals(1, records.size());
        assertEquals("PT-N", records.get(0).areaCode());
    }

    private static void ensureWeatherPersonExists() {
        final var repo = PersistenceContext.repositories().systemUsers();
        if (repo.ofIdentity(Username.valueOf(WEATHER_PERSON_USERNAME)).isPresent()) {
            return;
        }
        final var builder = new SystemUserBuilder(new AiSafePasswordPolicy(), new PlainTextEncoder());
        builder.withUsername(WEATHER_PERSON_USERNAME)
                .withPassword(WEATHER_PERSON_PASSWORD)
                .withName("Weather", "Person")
                .withEmail("weather-us042@aisafe.com")
                .withRoles(AiSafeRoles.WEATHER_PERSON);
        repo.save(builder.build());
    }

    private static void ensureBackofficeOperatorExists() {
        final var repo = PersistenceContext.repositories().systemUsers();
        if (repo.ofIdentity(Username.valueOf(BACKOFFICE_USERNAME)).isPresent()) {
            return;
        }
        final var builder = new SystemUserBuilder(new AiSafePasswordPolicy(), new PlainTextEncoder());
        builder.withUsername(BACKOFFICE_USERNAME)
                .withPassword(BACKOFFICE_PASSWORD)
                .withName("Backoffice", "Operator")
                .withEmail("bo-us042@aisafe.com")
                .withRoles(AiSafeRoles.BACKOFFICE_OPERATOR);
        repo.save(builder.build());
    }
}
