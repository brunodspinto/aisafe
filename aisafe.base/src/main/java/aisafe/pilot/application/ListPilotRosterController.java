package aisafe.pilot.application;

import aisafe.aircraftmodel.domain.AircraftModel;
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
import eapli.framework.infrastructure.authz.domain.model.SystemUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Application-layer controller for the "List Company Pilot Roster" use case (US076).
 * Resolves the authenticated ATCC's company and provides filtered views of its pilot roster.
 *
 * <p>Filtering is applied in memory after loading the full roster from the repository.
 * No new JPQL queries are introduced — consistent with the US072 approach.</p>
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
     * Returns all pilots registered to the authenticated ATCC's company
     * (both active and inactive).
     *
     * @return full pilot roster
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
     * Returns only the active pilots ({@code isActive()} == true) of the authenticated
     * ATCC's company.
     *
     * @return active pilots
     * @throws IllegalStateException if there is no active session or the user is not an ATCC
     */
    public List<Pilot> activePilots() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ATCC);
        return activePilots(authenticatedCollaboratorCompany());
    }

    /** Package-private overload used by tests — bypasses auth and company resolution. */
    List<Pilot> activePilots(final AirTransportCompany company) {
        final List<Pilot> result = new ArrayList<>();
        for (final Pilot p : allPilots(company)) {
            if (p.isActive()) result.add(p);
        }
        return result;
    }

    /**
     * Returns pilots of the authenticated ATCC's company that are certified for the given
     * aircraft model name. The comparison is case-insensitive.
     *
     * @param modelName aircraft model name to filter by (case-insensitive)
     * @return matching pilots
     * @throws IllegalStateException if there is no active session or the user is not an ATCC
     */
    public List<Pilot> pilotsByCertifiedModel(final String modelName) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ATCC);
        return pilotsByCertifiedModel(authenticatedCollaboratorCompany(), modelName);
    }

    /** Package-private overload used by tests — bypasses auth and company resolution. */
    List<Pilot> pilotsByCertifiedModel(final AirTransportCompany company, final String modelName) {
        final String trimmed = modelName.trim();
        final List<Long> matchingIds = new ArrayList<>();
        for (final AircraftModel m : modelRepo.findAll()) {
            if (m.modelName().equalsIgnoreCase(trimmed)) {
                matchingIds.add(m.identity());
            }
        }
        final List<Pilot> result = new ArrayList<>();
        for (final Pilot p : allPilots(company)) {
            for (final Long id : matchingIds) {
                if (p.isCertifiedFor(id)) {
                    result.add(p);
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Loads the full roster for the given company by streaming the repository result into a list.
     * All three public filter methods delegate to this helper.
     *
     * @param company the company whose pilots to load
     * @return mutable list of all pilots belonging to the company
     */
    private List<Pilot> loadRoster(final AirTransportCompany company) {
        final List<Pilot> result = new ArrayList<>();
        for (final Pilot p : pilotRepo.findByAirTransportCompany(company)) {
            result.add(p);
        }
        return result;
    }

    /**
     * Resolves the air transport company of the currently authenticated collaborator.
     * Identical logic to {@code AddPilotController.authenticatedCollaboratorCompany()}.
     *
     * @return the authenticated collaborator's company
     * @throws IllegalStateException if there is no active session or the user is not a company collaborator
     */
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
