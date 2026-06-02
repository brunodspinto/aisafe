package aisafe.weatherdata.application;

import java.util.List;

/**
 * Strategy interface for parsing weather data from a file.
 *
 * <p>Implementations provide support for a specific file format (CSV, JSON, XML, etc.).
 * The controller depends only on this interface, so new formats can be added without
 * modifying the controller or domain (AC042.6).</p>
 */
public interface WeatherDataParser {

    /**
     * Parses the file at the given path and returns all successfully parsed records.
     * Rows that cannot be parsed (missing fields, wrong types, bad date format) are
     * silently skipped — the caller is responsible for counting failures via
     * {@link ImportResult}.
     *
     * @param filePath absolute or relative path to the file to parse
     * @return list of parsed records; empty if the file has no data rows
     */
    List<ParsedWeatherRecord> parse(String filePath);
}
