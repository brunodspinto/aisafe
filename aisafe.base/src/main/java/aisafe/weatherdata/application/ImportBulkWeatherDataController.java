package aisafe.weatherdata.application;

import aisafe.aircontrolarea.domain.AirControlAreaCode;
import aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafeRoles;
import aisafe.weatherdata.domain.WeatherData;
import aisafe.weatherdata.domain.WeatherSource;
import aisafe.weatherdata.repositories.WeatherDataRepository;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller responsible for the use case "Import Bulk Weather Data" (US042).
 *
 * <p>Enforces {@code WEATHER_PERSON} role (AC042.3), delegates file parsing to a
 * {@link WeatherDataParser} implementation (AC042.6), validates each parsed record
 * against the {@link AirControlAreaRepository} (AC042.2), and persists valid records
 * via {@link WeatherDataRepository} (AC042.1). Invalid records are skipped and
 * reported in the returned {@link ImportResult} (AC042.4, AC042.5).</p>
 */
@UseCaseController
public class ImportBulkWeatherDataController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();

    private final AirControlAreaRepository areaRepository =
            PersistenceContext.repositories().airControlAreas();

    private final WeatherDataRepository weatherDataRepository =
            PersistenceContext.repositories().weatherData();

    /**
     * Parses the file at {@code filePath} and imports all valid weather records.
     *
     * @param filePath path to the import file (CSV format)
     * @return an {@link ImportResult} with the count of saved records and any failure messages
     * @throws IllegalStateException if the authenticated user does not have the WEATHER_PERSON role
     */
    public ImportResult importWeatherData(final String filePath) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.WEATHER_PERSON);

        final WeatherDataParser parser = new CsvWeatherDataParser();
        final List<ParsedWeatherRecord> parsed = parser.parse(filePath);

        int saved = 0;
        final List<String> failures = new ArrayList<>();

        for (final ParsedWeatherRecord record : parsed) {
            final AirControlAreaCode areaCode = AirControlAreaCode.valueOf(record.areaCode());
            if (areaRepository.ofIdentity(areaCode).isEmpty()) {
                failures.add("Unknown area code '" + record.areaCode() + "'");
                continue;
            }
            try {
                final WeatherSource source = new WeatherSource(record.provider(), record.format());
                final WeatherData weatherData = new WeatherData(
                        areaCode, source, record.date(),
                        record.temperature(), record.windSpeed(), record.windDirection(),
                        record.pressure(), record.visibility());
                weatherDataRepository.save(weatherData);
                saved++;
            } catch (final IllegalArgumentException e) {
                failures.add("Invalid record for area '" + record.areaCode() + "': " + e.getMessage());
            }
        }

        return new ImportResult(saved, failures);
    }
}
