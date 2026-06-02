package aisafe.weatherdata.application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV implementation of {@link WeatherDataParser}.
 *
 * <p>Expects the first line to be a header and each subsequent line to contain
 * exactly 9 comma-separated fields in this order:</p>
 * <pre>area_code, provider, format, date, temperature, windSpeed, windDirection, pressure, visibility</pre>
 *
 * <p>Rows that are missing fields, have non-numeric numeric fields, or have an
 * unparseable date are silently skipped (AC042.4). Domain-level validation
 * (e.g. invalid wind direction, negative wind speed) is performed by the
 * controller when constructing the {@code WeatherData} aggregate.</p>
 */
public class CsvWeatherDataParser implements WeatherDataParser {

    private static final int EXPECTED_FIELDS = 9;

    @Override
    public List<ParsedWeatherRecord> parse(final String filePath) {
        final List<ParsedWeatherRecord> records = new ArrayList<>();
        final List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(filePath));
        } catch (final IOException e) {
            return records;
        }

        // skip header (first line)
        for (int i = 1; i < lines.size(); i++) {
            final String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            final ParsedWeatherRecord record = parseLine(line);
            if (record != null) {
                records.add(record);
            }
        }
        return records;
    }

    private ParsedWeatherRecord parseLine(final String line) {
        final String[] fields = line.split(",", -1);
        if (fields.length != EXPECTED_FIELDS) {
            return null;
        }
        try {
            final String areaCode = fields[0].trim();
            final String provider = fields[1].trim();
            final String format = fields[2].trim();
            final LocalDateTime date = LocalDateTime.parse(fields[3].trim());
            final double temperature = Double.parseDouble(fields[4].trim());
            final double windSpeed = Double.parseDouble(fields[5].trim());
            final String windDirection = fields[6].trim();
            final double pressure = Double.parseDouble(fields[7].trim());
            final double visibility = Double.parseDouble(fields[8].trim());
            return new ParsedWeatherRecord(areaCode, provider, format, date,
                    temperature, windSpeed, windDirection, pressure, visibility);
        } catch (final DateTimeParseException | NumberFormatException e) {
            return null;
        }
    }
}
