package aisafe.flightroute.application;

import aisafe.airport.domain.Airport;
import aisafe.airport.domain.AirportIATACode;
import aisafe.airport.repositories.AirportRepository;
import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.airtransportcompany.repositories.AirTransportCompanyRepository;
import aisafe.collaborator.repositories.CollaboratorRepository;
import aisafe.flightroute.domain.FlightRoute;
import aisafe.flightroute.domain.RouteName;
import aisafe.flightroute.repositories.FlightRouteRepository;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;

/**
 * Application-layer controller for the "Create Flight Route" use case (US073).
 * Requires an authenticated Air Transport Company Collaborator (ATCC) role.
 * The company is resolved automatically from the authenticated user's session.
 */
@UseCaseController
public class CreateFlightRouteController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final FlightRouteRepository flightRouteRepository =
            PersistenceContext.repositories().flightRoutes();
    private final AirportRepository airportRepository =
            PersistenceContext.repositories().airports();
    private final CollaboratorRepository collaboratorRepository =
            PersistenceContext.repositories().collaborators();
    private final AirTransportCompanyRepository companyRepository =
            PersistenceContext.repositories().airTransportCompanies();

    /**
     * Returns all registered airports for selection in the UI.
     *
     * @return all {@link Airport} instances
     */
    public Iterable<Airport> allAirports() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ATCC);
        return airportRepository.findAll();
    }

    /**
     * Creates and persists a new flight route for the authenticated collaborator's company.
     *
     * @param routeNameStr    route name string (format [A-Z]{2}[0-9]{1,4})
     * @param originCodeStr   IATA code of the origin airport
     * @param destCodeStr     IATA code of the destination airport
     * @return the saved {@link FlightRoute}
     * @throws IllegalArgumentException if the route name already exists, airports are not found,
     *                                  or origin equals destination
     * @throws IllegalStateException    if there is no active session or the user has no company
     */
    public FlightRoute createFlightRoute(final String routeNameStr,
                                         final String originCodeStr,
                                         final String destCodeStr) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ATCC);

        final AirTransportCompany company = authenticatedCollaboratorCompany();

        final RouteName routeName = new RouteName(routeNameStr.trim().toUpperCase());

        final String iata = company.identity().toString().toUpperCase();
        if (!routeName.name().startsWith(iata))
            throw new IllegalArgumentException(
                    "Route name must start with the company's IATA code '" + iata + "'.");

        if (flightRouteRepository.existsByName(routeName))
            throw new IllegalArgumentException(
                    "A flight route with name '" + routeName + "' already exists.");

        final AirportIATACode originCode =
                AirportIATACode.valueOf(originCodeStr.trim().toUpperCase());
        final AirportIATACode destCode =
                AirportIATACode.valueOf(destCodeStr.trim().toUpperCase());

        airportRepository.ofIdentity(originCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Origin airport '" + originCode + "' not found."));

        airportRepository.ofIdentity(destCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Destination airport '" + destCode + "' not found."));

        final FlightRoute route = new FlightRoute(
                routeName, originCode, destCode, company.identity());

        return flightRouteRepository.save(route);
    }

    /**
     * Resolves the air transport company of the currently authenticated collaborator.
     *
     * @return the authenticated collaborator's company
     * @throws IllegalStateException if there is no active session or the user is not a company collaborator
     */
    private AirTransportCompany authenticatedCollaboratorCompany() {
        final SystemUser su = authz.session()
                .orElseThrow(() -> new IllegalStateException("No active session."))
                .authenticatedUser();

        return collaboratorRepository.findBySystemUser(su)
                .map(c -> {
                    if (!c.isCompanyCollaborator())
                        throw new IllegalStateException(
                                "Authenticated user is not a company collaborator.");
                    return companyRepository.ofIdentity(c.companyIataCode())
                            .orElseThrow(() -> new IllegalStateException(
                                    "Company not found for IATA code: " + c.companyIataCode()));
                })
                .orElseThrow(() -> new IllegalStateException(
                        "No collaborator found for authenticated user."));
    }
}