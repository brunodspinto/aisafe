package aisafe.pilot.application;

import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.airtransportcompany.repositories.AirTransportCompanyRepository;
import aisafe.collaborator.repositories.CollaboratorRepository;
import aisafe.flightplan.repositories.FlightPlanRepository;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.pilot.domain.Pilot;
import aisafe.pilot.repositories.PilotRepository;
import aisafe.usermanagement.domain.AiSafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Application-layer controller for the "Remove a Pilot" use case (US077).
 * The authenticated ATCC can make a pilot of their own company inactive,
 * provided the pilot has no flight plans assigned.
 */
@UseCaseController
public class RemovePilotController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final PilotRepository pilotRepo =
            PersistenceContext.repositories().pilots();
    private final FlightPlanRepository flightPlanRepo =
            PersistenceContext.repositories().flightPlans();
    private final AirTransportCompanyRepository companyRepo =
            PersistenceContext.repositories().airTransportCompanies();
    private final CollaboratorRepository collaboratorRepo =
            PersistenceContext.repositories().collaborators();

    /**
     * Returns all active pilots belonging to the authenticated collaborator's company.
     *
     * @return active pilots of the company
     */
    public Iterable<Pilot> allActivePilotsOfCompany() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ATCC);
        final AirTransportCompany company = authenticatedCollaboratorCompany();
        final List<Pilot> active = new ArrayList<>();
        for (final Pilot p : pilotRepo.findByAirTransportCompany(company)) {
            if (p.isActive()) active.add(p);
        }
        return active;
    }

    /**
     * Deactivates the pilot with the given id and persists the change.
     *
     * @param pilotId the identity of the pilot to deactivate
     * @return the deactivated pilot
     * @throws IllegalArgumentException if the pilot is not found, does not belong to the
     *                                  authenticated user's company, or has flight plans assigned
     * @throws IllegalStateException    if the pilot is already inactive
     */
    public Pilot deactivatePilot(final Long pilotId) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ATCC);
        final AirTransportCompany company = authenticatedCollaboratorCompany();

        final Pilot pilot = pilotRepo.ofIdentity(pilotId)
                .orElseThrow(() -> new IllegalArgumentException("Pilot not found: " + pilotId));

        if (!pilot.companyIataCode().equals(company.identity()))
            throw new IllegalArgumentException("Pilot does not belong to your company.");

        if (flightPlanRepo.hasFlightPlanAssignedTo(pilotId))
            throw new IllegalArgumentException(
                    "Cannot deactivate a pilot with flight plans assigned.");

        pilot.deactivate();
        return pilotRepo.save(pilot);
    }

    private AirTransportCompany authenticatedCollaboratorCompany() {
        final SystemUser su = authz.session()
                .orElseThrow(() -> new IllegalStateException("No active session."))
                .authenticatedUser();
        return collaboratorRepo.findBySystemUser(su)
                .map(c -> {
                    if (!c.isCompanyCollaborator())
                        throw new IllegalStateException(
                                "Authenticated user is not a company collaborator.");
                    return companyRepo.ofIdentity(c.companyIataCode())
                            .orElseThrow(() -> new IllegalStateException(
                                    "Company not found for IATA code: " + c.companyIataCode()));
                })
                .orElseThrow(() -> new IllegalStateException(
                        "No collaborator found for authenticated user."));
    }
}
