package aisafe.flightplan.application;

import aisafe.aircraft.domain.Aircraft;
import aisafe.aircraft.domain.RegistrationNumber;
import aisafe.aircraft.repositories.AircraftRepository;
import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.airtransportcompany.domain.IATACode;
import aisafe.airtransportcompany.repositories.AirTransportCompanyRepository;
import aisafe.dsl.ast.FlightType;
import aisafe.flightplan.domain.FlightPlan;
import aisafe.flightplan.domain.FlightPlanDesignator;
import aisafe.flightplan.domain.FuelQuantity;
import aisafe.flightplan.repositories.FlightPlanRepository;
import aisafe.flightroute.domain.FlightRoute;
import aisafe.flightroute.domain.RouteName;
import aisafe.flightroute.repositories.FlightRouteRepository;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.pilot.domain.Pilot;
import aisafe.pilot.repositories.PilotRepository;
import aisafe.usermanagement.domain.AiSafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Application-layer controller for the "Create a Flight Plan" use case (US080).
 * The authenticated Pilot creates a flight plan, in DRAFT status, for a route of their company.
 */
@UseCaseController
public class CreateFlightPlanController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final PilotRepository pilotRepo = PersistenceContext.repositories().pilots();
    private final FlightRouteRepository flightRouteRepo = PersistenceContext.repositories().flightRoutes();
    private final AircraftRepository aircraftRepo = PersistenceContext.repositories().aircraft();
    private final AirTransportCompanyRepository companyRepo = PersistenceContext.repositories().airTransportCompanies();
    private final FlightPlanRepository flightPlanRepo = PersistenceContext.repositories().flightPlans();

    /**
     * Returns the active flight routes of the authenticated pilot's company, for selection in the UI.
     *
     * @return active routes of the pilot's company
     */
    public Iterable<FlightRoute> availableRoutes() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.PILOT);
        final IATACode company = authenticatedPilot().companyIataCode();
        return StreamSupport.stream(flightRouteRepo.findByCompany(company).spliterator(), false)
                .filter(FlightRoute::isActive)
                .toList();
    }

    /**
     * Returns the active aircraft of the authenticated pilot's company fleet, for selection in the UI.
     *
     * @return active aircraft of the pilot's company
     */
    public Iterable<Aircraft> availableAircraft() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.PILOT);
        final AirTransportCompany company = company(authenticatedPilot().companyIataCode());
        final List<Aircraft> result = new ArrayList<>();
        for (final String registration : company.fleet()) {
            aircraftRepo.ofIdentity(RegistrationNumber.valueOf(registration))
                    .filter(Aircraft::isActive)
                    .ifPresent(result::add);
        }
        return result;
    }

    /**
     * Returns the active pilots of the authenticated pilot's company, for selection in the UI as
     * the pilot to be assigned to the plan.
     *
     * @return active pilots of the pilot's company
     */
    public Iterable<Pilot> availablePilots() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.PILOT);
        final AirTransportCompany company = company(authenticatedPilot().companyIataCode());
        return StreamSupport.stream(pilotRepo.findByAirTransportCompany(company).spliterator(), false)
                .filter(Pilot::isActive)
                .toList();
    }

    /**
     * Creates and persists a new flight plan in DRAFT status.
     *
     * @param routeNameStr           the route name the plan is for
     * @param aircraftRegistrationStr the assigned aircraft's registration number
     * @param assignedPilotId        the identity of the pilot to assign
     * @param flightType             the flight type (REGULAR or CHARTER)
     * @param designatorStr          the unique flight plan designator
     * @param departureDateTime      the planned departure date/time (must be in the future)
     * @param fuelAmount             the planned fuel quantity (must be strictly positive)
     * @return the persisted {@link FlightPlan}
     * @throws IllegalArgumentException if any referenced entity does not exist or a business rule is violated
     */
    public FlightPlan createFlightPlan(final String routeNameStr,
                                       final String aircraftRegistrationStr,
                                       final Long assignedPilotId,
                                       final FlightType flightType,
                                       final String designatorStr,
                                       final LocalDateTime departureDateTime,
                                       final double fuelAmount) {

        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.PILOT);

        final FlightRoute route = flightRouteRepo.ofIdentity(new RouteName(routeNameStr))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Flight route not found: " + routeNameStr));

        // AC080 — the authenticated pilot may only create plans for their own company's routes.
        // (availableRoutes() filters by company for the UI, but the controller is the real
        //  security boundary and must enforce ownership for any caller.)
        final Pilot creator = authenticatedPilot();
        if (!creator.companyIataCode().equals(route.companyIataCode()))
            throw new IllegalArgumentException(
                    "You can only create flight plans for your own company's routes.");

        final RegistrationNumber registration = RegistrationNumber.valueOf(aircraftRegistrationStr);
        final Aircraft aircraft = aircraftRepo.ofIdentity(registration)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aircraft not found: " + aircraftRegistrationStr));

        final Pilot assignedPilot = pilotRepo.ofIdentity(assignedPilotId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pilot not found: " + assignedPilotId));

        // AC080.3 — assigned pilot must belong to the route's company
        if (!assignedPilot.companyIataCode().equals(route.companyIataCode()))
            throw new IllegalArgumentException(
                    "The assigned pilot does not belong to the route's company.");

        final AirTransportCompany routeCompany = company(route.companyIataCode());

        // AC080.9 — aircraft must belong to the route's company
        if (!routeCompany.fleet().contains(registration.toString()))
            throw new IllegalArgumentException(
                    "The aircraft does not belong to the route's company.");

        // AC080.10 — aircraft must be active
        if (!aircraft.isActive())
            throw new IllegalArgumentException(
                    "The aircraft is not active and cannot be assigned to a flight plan.");

        final FlightPlanDesignator designator = FlightPlanDesignator.valueOf(designatorStr);

        // AC080.8 — designator must be unique
        if (flightPlanRepo.ofIdentity(designator).isPresent())
            throw new IllegalArgumentException(
                    "A flight plan with designator '" + designator + "' already exists.");

        final FlightPlan plan = new FlightPlan(
                designator, flightType, route.routeName(), registration,
                assignedPilot.identity(), departureDateTime, FuelQuantity.valueOf(fuelAmount));

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

    /**
     * Resolves an {@link AirTransportCompany} by its IATA code.
     *
     * @param iata the company IATA code
     * @return the company
     * @throws IllegalStateException if the company is not found
     */
    private AirTransportCompany company(final IATACode iata) {
        return companyRepo.ofIdentity(iata)
                .orElseThrow(() -> new IllegalStateException(
                        "Company not found for IATA code: " + iata));
    }
}
