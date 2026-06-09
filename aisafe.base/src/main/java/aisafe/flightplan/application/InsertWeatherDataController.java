package aisafe.flightplan.application;

import aisafe.flightplan.domain.FlightPlan;
import aisafe.flightplan.domain.FlightPlanDesignator;
import aisafe.flightplan.repositories.FlightPlanRepository;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.pilot.domain.Pilot;
import aisafe.pilot.repositories.PilotRepository;
import aisafe.usermanagement.domain.AiSafeRoles;
import aisafe.weatherdata.domain.WeatherData;
import aisafe.weatherdata.repositories.WeatherDataRepository;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;

import java.util.stream.StreamSupport;

/**
 * Application-layer controller for the "Insert Weather Data in a Flight" use case (US082).
 * The authenticated Pilot attaches existing weather data to one of their own flight plans.
 * If the plan had been tested, the test is voided (status reverts to VALIDATED).
 */
@UseCaseController
public class InsertWeatherDataController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final PilotRepository pilotRepo = PersistenceContext.repositories().pilots();
    private final FlightPlanRepository flightPlanRepo = PersistenceContext.repositories().flightPlans();
    private final WeatherDataRepository weatherDataRepo = PersistenceContext.repositories().weatherData();

    /**
     * Returns the flight plans assigned to the authenticated pilot, for selection in the UI.
     *
     * @return the authenticated pilot's flight plans
     */
    public Iterable<FlightPlan> myFlightPlans() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.PILOT);
        final Long pilotId = authenticatedPilot().identity();
        return StreamSupport.stream(flightPlanRepo.findAll().spliterator(), false)
                .filter(p -> pilotId.equals(p.assignedPilotId()))
                .toList();
    }

    /**
     * Returns all weather data records registered in the system, for selection in the UI.
     *
     * @return all {@link WeatherData} records
     */
    public Iterable<WeatherData> availableWeatherData() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.PILOT);
        return weatherDataRepo.findAll();
    }

    /**
     * Attaches the given weather data to the given flight plan of the authenticated pilot.
     *
     * @param designatorStr the designator of the flight plan
     * @param weatherDataId the identity of an existing weather data record
     * @return the updated and persisted {@link FlightPlan}
     * @throws IllegalArgumentException if the flight plan or the weather data does not exist
     * @throws IllegalStateException    if there is no active session, the user is not a pilot,
     *                                  or the flight plan does not belong to the authenticated pilot
     */
    public FlightPlan insertWeatherData(final String designatorStr, final Long weatherDataId) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.PILOT);

        final Long pilotId = authenticatedPilot().identity();

        final FlightPlan plan = flightPlanRepo.ofIdentity(FlightPlanDesignator.valueOf(designatorStr))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Flight plan not found: " + designatorStr));

        // AC082.2 — the flight plan must belong to the authenticated pilot
        if (!pilotId.equals(plan.assignedPilotId()))
            throw new IllegalStateException(
                    "The flight plan does not belong to the authenticated pilot.");

        // AC082.4 — the weather data must exist
        weatherDataRepo.ofIdentity(weatherDataId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Weather data not found: " + weatherDataId));

        plan.addWeatherData(weatherDataId);

        return flightPlanRepo.save(plan);
    }

    /**
     * Resolves the {@link Pilot} backing the currently authenticated session user.
     *
     * @return the authenticated pilot
     * @throws IllegalStateException if there is no active session or the user is not a pilot
     */
    private Pilot authenticatedPilot() {
        final SystemUser su = authz.session()
                .orElseThrow(() -> new IllegalStateException("No active session."))
                .authenticatedUser();
        return pilotRepo.findBySystemUser(su)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user is not a registered pilot."));
    }
}
