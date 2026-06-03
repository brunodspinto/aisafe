package aisafe.pilot.application;

import aisafe.aircraftmodel.repositories.AircraftModelRepository;
import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.airtransportcompany.repositories.AirTransportCompanyRepository;
import aisafe.collaborator.repositories.CollaboratorRepository;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.pilot.domain.Pilot;
import aisafe.pilot.repositories.PilotRepository;
import aisafe.usermanagement.domain.AiSafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

import java.util.List;

/**
 * Application-layer controller for the "List Company Pilot Roster" use case (US076).
 * Resolves the authenticated ATCC's company and provides filtered views of its pilot roster.
 */
@UseCaseController
public class ListPilotRosterController {

    private final AuthorizationService authz;
    private final PilotRepository pilotRepo;
    private final AircraftModelRepository modelRepo;
    private final AirTransportCompanyRepository companyRepo;
    private final CollaboratorRepository collaboratorRepo;

    /** Runtime constructor — pulls repositories from {@link PersistenceContext}. */
    public ListPilotRosterController() {
        this.authz = AuthzRegistry.authorizationService();
        this.pilotRepo = PersistenceContext.repositories().pilots();
        this.modelRepo = PersistenceContext.repositories().aircraftModels();
        this.companyRepo = PersistenceContext.repositories().airTransportCompanies();
        this.collaboratorRepo = PersistenceContext.repositories().collaborators();
    }

    /**
     * Testing constructor — accepts injected repositories so no JPA context is required.
     * {@code authz} is intentionally null; only the package-private method overloads are
     * called from tests and none of them perform the auth check.
     * Package-private; not intended for production use.
     */
    ListPilotRosterController(final PilotRepository pilotRepo,
                               final AircraftModelRepository modelRepo,
                               final AirTransportCompanyRepository companyRepo,
                               final CollaboratorRepository collaboratorRepo) {
        this.authz = null;
        this.pilotRepo = pilotRepo;
        this.modelRepo = modelRepo;
        this.companyRepo = companyRepo;
        this.collaboratorRepo = collaboratorRepo;
    }

    /**
     * Returns all pilots registered to the authenticated ATCC's company.
     *
     * @return full pilot roster (active and inactive)
     * @throws IllegalStateException if there is no active session or the user is not an ATCC
     */
    public List<Pilot> allPilots() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ATCC);
        return allPilots(authenticatedCollaboratorCompany());
    }

    /** Package-private overload used by tests — bypasses auth and company resolution. */
    List<Pilot> allPilots(final AirTransportCompany company) {
        return loadRoster(company);
    }

    /**
     * Returns only the active pilots of the authenticated ATCC's company.
     *
     * @return pilots where {@code isActive()} == true
     * @throws IllegalStateException if there is no active session or the user is not an ATCC
     */
    public List<Pilot> activePilots() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ATCC);
        return activePilots(authenticatedCollaboratorCompany());
    }

    /** Package-private overload used by tests — bypasses auth and company resolution. */
    List<Pilot> activePilots(final AirTransportCompany company) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Returns pilots of the authenticated ATCC's company certified for the given aircraft model name.
     * The comparison is case-insensitive.
     *
     * @param modelName aircraft model name to filter by
     * @return matching pilots
     * @throws IllegalStateException if there is no active session or the user is not an ATCC
     */
    public List<Pilot> pilotsByCertifiedModel(final String modelName) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ATCC);
        return pilotsByCertifiedModel(authenticatedCollaboratorCompany(), modelName);
    }

    /** Package-private overload used by tests — bypasses auth and company resolution. */
    List<Pilot> pilotsByCertifiedModel(final AirTransportCompany company, final String modelName) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private List<Pilot> loadRoster(final AirTransportCompany company) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private AirTransportCompany authenticatedCollaboratorCompany() {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
