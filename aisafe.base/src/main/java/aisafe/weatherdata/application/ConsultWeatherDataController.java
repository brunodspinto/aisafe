package aisafe.weatherdata.application;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.aircontrolarea.domain.AirControlAreaCode;
import aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafeRoles;
import aisafe.weatherdata.domain.WeatherData;
import aisafe.weatherdata.repositories.WeatherDataRepository;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import java.time.LocalDate;

/**
 * Controller responsible for the use case "Consult Weather Data" (US043).
 */
@UseCaseController
public class ConsultWeatherDataController {

    private final AirControlAreaRepository areaRepository;

    private final WeatherDataRepository weatherDataRepository;

    private final Runnable authorizationGuard;

    public ConsultWeatherDataController() {
        this(PersistenceContext.repositories().airControlAreas(),
                PersistenceContext.repositories().weatherData(),
                null);
    }

    ConsultWeatherDataController(final AirControlAreaRepository areaRepository,
                                 final WeatherDataRepository weatherDataRepository,
                                 final Runnable authorizationGuard) {
        if (areaRepository == null) {
            throw new IllegalArgumentException("Air Control Area repository cannot be null.");
        }
        if (weatherDataRepository == null) {
            throw new IllegalArgumentException("Weather Data repository cannot be null.");
        }
        this.areaRepository = areaRepository;
        this.weatherDataRepository = weatherDataRepository;
        this.authorizationGuard = authorizationGuard != null ? authorizationGuard : this::ensureAuthenticatedUserRole;
    }

    public Iterable<AirControlArea> activeAirControlAreas() {
        ensureAuthorized();
        return areaRepository.findAll();
    }

    public Iterable<WeatherData> consultWeatherData(final LocalDate date, final String areaCode) {
        ensureAuthorized();

        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null.");
        }

        final AirControlAreaCode code = AirControlAreaCode.valueOf(areaCode);
        if (areaRepository.ofIdentity(code).isEmpty()) {
            throw new IllegalArgumentException("Unknown Air Control Area code: " + code);
        }

        return weatherDataRepository.findByDateAndAirControlArea(date, code);
    }

    private void ensureAuthorized() {
        authorizationGuard.run();
    }

    private void ensureAuthenticatedUserRole() {
        AuthzRegistry.authorizationService().ensureAuthenticatedUserHasAnyOf(
                AiSafeRoles.WEATHER_PERSON,
                AiSafeRoles.PILOT,
                AiSafeRoles.FLIGHT_CONTROL_OPERATOR);
    }
}
