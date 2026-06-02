package aisafe.weatherdata.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@code CsvWeatherDataParser}.
 */
class CsvWeatherDataParserTest {

    private static final String HEADER =
            "area_code,provider,format,date,temperature,windSpeed,windDirection,pressure,visibility";

    private final List<Path> tempFiles = new ArrayList<>();

    @AfterEach
    void deleteTempFiles() throws IOException {
        for (final Path p : tempFiles) {
            Files.deleteIfExists(p);
        }
    }

    private Path writeTempCsv(final String... lines) throws IOException {
        final Path path = Files.createTempFile("us042-test-", ".csv");
        Files.write(path, List.of(lines));
        tempFiles.add(path);
        return path;
    }

    @Test
    void ensureValidCsvRowProducesCorrectParsedRecord() throws Exception {
        final Path csv = writeTempCsv(
                HEADER,
                "PT-N,IPMA,CSV,2025-05-14T10:00:00,20.0,15.0,N,1013.0,10.0");

        final List<ParsedWeatherRecord> records = new CsvWeatherDataParser().parse(csv.toString());

        assertEquals(1, records.size());
        final ParsedWeatherRecord r = records.get(0);
        assertEquals("PT-N", r.areaCode());
        assertEquals("IPMA", r.provider());
        assertEquals("CSV", r.format());
        assertEquals(LocalDateTime.of(2025, 5, 14, 10, 0), r.date());
        assertEquals(20.0, r.temperature());
        assertEquals(15.0, r.windSpeed());
        assertEquals("N", r.windDirection());
        assertEquals(1013.0, r.pressure());
        assertEquals(10.0, r.visibility());
    }

    @Test
    void ensureHeaderOnlyFileReturnsEmptyList() throws Exception {
        final Path csv = writeTempCsv(HEADER);

        final List<ParsedWeatherRecord> records = new CsvWeatherDataParser().parse(csv.toString());

        assertTrue(records.isEmpty());
    }

    @Test
    void ensureMultipleValidRowsAreAllParsed() throws Exception {
        final Path csv = writeTempCsv(
                HEADER,
                "PT-N,IPMA,CSV,2025-05-14T10:00:00,20.0,15.0,N,1013.0,10.0",
                "PT-S,IPMA,CSV,2025-05-14T11:00:00,18.0,10.0,SW,1010.0,8.0");

        final List<ParsedWeatherRecord> records = new CsvWeatherDataParser().parse(csv.toString());

        assertEquals(2, records.size());
    }

    @Test
    void ensureRowWithMissingFieldIsSkipped() throws Exception {
        final Path csv = writeTempCsv(
                HEADER,
                "PT-N,IPMA,CSV,2025-05-14T10:00:00,20.0,15.0,N,1013.0"); // missing visibility

        final List<ParsedWeatherRecord> records = new CsvWeatherDataParser().parse(csv.toString());

        assertTrue(records.isEmpty());
    }

    @Test
    void ensureRowWithInvalidDateFormatIsSkipped() throws Exception {
        final Path csv = writeTempCsv(
                HEADER,
                "PT-N,IPMA,CSV,14-05-2025,20.0,15.0,N,1013.0,10.0"); // wrong date format

        final List<ParsedWeatherRecord> records = new CsvWeatherDataParser().parse(csv.toString());

        assertTrue(records.isEmpty());
    }

    @Test
    void ensureRowWithNonNumericTemperatureIsSkipped() throws Exception {
        final Path csv = writeTempCsv(
                HEADER,
                "PT-N,IPMA,CSV,2025-05-14T10:00:00,WARM,15.0,N,1013.0,10.0");

        final List<ParsedWeatherRecord> records = new CsvWeatherDataParser().parse(csv.toString());

        assertTrue(records.isEmpty());
    }
}
