package aisafe.weatherdata.application;

import aisafe.aircontrolarea.domain.AirControlArea;
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
import java.time.LocalDateTime;

/**
 * Controller responsible for the use case "Register Weather Data" (US041).
 */
@UseCaseController
public class RegisterWeatherDataController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();

    private final AirControlAreaRepository areaRepository =
            PersistenceContext.repositories().airControlAreas();

    private final WeatherDataRepository weatherDataRepository =
            PersistenceContext.repositories().weatherData();

    /**
     * Returns all registered Air Control Areas so the UI can present them for selection.
     */
    public Iterable<AirControlArea> activeAirControlAreas() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.WEATHER_PERSON);
        return areaRepository.findAll();
    }

    /**
     * Registers weather data linked to the given Air Control Area.
     * The UI passes only primitive types; the controller builds the domain objects.
     */
    public WeatherData registerWeatherData(final String areaCode, final String provider,
                                           final String format, final LocalDateTime date,
                                           final double temperature, final double windSpeed,
                                           final String windDirection, final double pressure,
                                           final double visibility) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.WEATHER_PERSON);

        final WeatherSource source = new WeatherSource(provider, format);
        final WeatherData weatherData = new WeatherData(
                AirControlAreaCode.valueOf(areaCode), source, date,
                temperature, windSpeed, windDirection,
                pressure, visibility);

        return weatherDataRepository.save(weatherData);
    }
}
