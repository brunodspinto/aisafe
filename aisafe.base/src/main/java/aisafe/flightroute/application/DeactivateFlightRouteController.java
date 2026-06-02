package aisafe.flightroute.application;

import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.airtransportcompany.repositories.AirTransportCompanyRepository;
import aisafe.collaborator.repositories.CollaboratorRepository;
import aisafe.flightroute.domain.FlightRoute;
import aisafe.flightroute.domain.RouteName;
import aisafe.flightroute.repositories.FlightRepository;
import aisafe.flightroute.repositories.FlightRouteRepository;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;

import java.time.LocalDate;

/**
 * Application-layer controller for the "Deactivate Flight Route" use case (US074).
 * Requires an authenticated Air Transport Company Collaborator (ATCC) role.
 */
@UseCaseController
public class DeactivateFlightRouteController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final FlightRouteRepository flightRouteRepository =
            PersistenceContext.repositories().flightRoutes();
    private final FlightRepository flightRepository =
            PersistenceContext.repositories().flights();
    private final CollaboratorRepository collaboratorRepository =
            PersistenceContext.repositories().collaborators();
    private final AirTransportCompanyRepository companyRepository =
            PersistenceContext.repositories().airTransportCompanies();

    /**
     * Returns all active routes belonging to the authenticated ATCC's company.
     *
     * @return active {@link FlightRoute} instances for the company
     */
    public Iterable<FlightRoute> activeRoutesByCompany() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ATCC);
        return flightRouteRepository.findActiveByCompany(authenticatedCompany().identity());
    }

    /**
     * Deactivates the given route from the specified date onwards.
     *
     * @param routeName        the identity of the route to deactivate
     * @param deactivationDate the date from which the route becomes inactive (inclusive)
     * @return the saved, now-inactive {@link FlightRoute}
     * @throws IllegalArgumentException if the route is not found, does not belong to the ATCC's
     *                                  company, or planned flights exist on or after the given date
     */
    public FlightRoute deactivateFlightRoute(final RouteName routeName,
                                             final LocalDate deactivationDate) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ATCC);

        final AirTransportCompany company = authenticatedCompany();

        final FlightRoute route = flightRouteRepository.ofIdentity(routeName)
                .filter(r -> r.companyIataCode().equals(company.identity()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Route '" + routeName + "' not found or does not belong to your company."));

        if (flightRepository.hasFlightsAfter(route, deactivationDate))
            throw new IllegalArgumentException(
                    "Cannot deactivate: there are planned flights on this route from "
                            + deactivationDate + " onwards.");

        route.deactivate(deactivationDate);
        return flightRouteRepository.save(route);
    }

    private AirTransportCompany authenticatedCompany() {
        final SystemUser su = authz.session()
                .orElseThrow(() -> new IllegalStateException("No active session."))
                .authenticatedUser();

        return collaboratorRepository.findBySystemUser(su)
                .map(c -> companyRepository.ofIdentity(c.companyIataCode())
                        .orElseThrow(() -> new IllegalStateException(
                                "Company not found for IATA code: " + c.companyIataCode())))
                .orElseThrow(() -> new IllegalStateException(
                        "No collaborator found for authenticated user."));
    }
}
