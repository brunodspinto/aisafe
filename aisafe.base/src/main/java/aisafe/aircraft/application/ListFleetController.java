package aisafe.aircraft.application;

import aisafe.aircraft.domain.Aircraft;
import aisafe.aircraft.domain.RegistrationNumber;
import aisafe.aircraft.repositories.AircraftRepository;
import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.collaborator.repositories.CollaboratorRepository;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Application-layer controller for the "List Company Fleet" use case (US072).
 * Resolves the authenticated user's company and provides filtered views of its fleet.
 */
@UseCaseController
public class ListFleetController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AircraftRepository aircraftRepo = PersistenceContext.repositories().aircraft();
    private final CollaboratorRepository collaboratorRepo = PersistenceContext.repositories().collaborators();

    /**
     * Resolves the air transport company of the currently authenticated user.
     *
     * @return the company associated with the authenticated user
     * @throws IllegalStateException if there is no active session, the user is not a company collaborator,
     *                               or no collaborator is found for the user
     */
    private AirTransportCompany resolveCompany() {
        final SystemUser su = authz.session()
                .orElseThrow(() -> new IllegalStateException("No active session."))
                .authenticatedUser();
        return collaboratorRepo.findBySystemUser(su)
                .map(c -> {
                    if (!c.isCompanyCollaborator())
                        throw new IllegalStateException("Authenticated user is not an Air Transport Company collaborator.");
                    return c.airTransportCompany();
                })
                .orElseThrow(() -> new IllegalStateException("No collaborator found for the authenticated user."));
    }

    /**
     * Loads the full list of {@link Aircraft} objects for the given company by resolving
     * each registration number from the repository.
     *
     * @param company the company whose fleet to load
     * @return list of aircraft found in the repository for that company
     */
    private List<Aircraft> loadFleet(final AirTransportCompany company) {
        final List<Aircraft> result = new ArrayList<>();
        for (final String reg : company.fleet()) {
            aircraftRepo.ofIdentity(RegistrationNumber.valueOf(reg)).ifPresent(result::add);
        }
        return result;
    }

    /**
     * Returns the full fleet of the authenticated user's company.
     *
     * @return list of all aircraft belonging to the company
     */
    public List<Aircraft> companyFleet() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ATCC, AiSafeRoles.ADMIN);
        return loadFleet(resolveCompany());
    }

    /**
     * Returns fleet aircraft whose model name matches (case-insensitive).
     *
     * @param modelName model name to filter by
     * @return matching aircraft
     */
    public List<Aircraft> fleetByModel(final String modelName) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ATCC, AiSafeRoles.ADMIN);
        final List<Aircraft> result = new ArrayList<>();
        for (final Aircraft a : loadFleet(resolveCompany())) {
            if (a.aircraftModel().modelName().equalsIgnoreCase(modelName.trim()))
                result.add(a);
        }
        return result;
    }

    /**
     * Returns fleet aircraft manufactured by the given maker (case-insensitive).
     *
     * @param makerName maker name to filter by
     * @return matching aircraft
     */
    public List<Aircraft> fleetByMaker(final String makerName) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ATCC, AiSafeRoles.ADMIN);
        final List<Aircraft> result = new ArrayList<>();
        for (final Aircraft a : loadFleet(resolveCompany())) {
            if (a.aircraftModel().maker().name().equalsIgnoreCase(makerName.trim()))
                result.add(a);
        }
        return result;
    }

    /**
     * Returns fleet aircraft whose total seat count is at least {@code minSeats}.
     *
     * @param minSeats minimum number of total seats (inclusive)
     * @return matching aircraft
     */
    public List<Aircraft> fleetByMinCapacity(final int minSeats) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ATCC, AiSafeRoles.ADMIN);
        final List<Aircraft> result = new ArrayList<>();
        for (final Aircraft a : loadFleet(resolveCompany())) {
            if (a.cabinConfiguration() != null && a.cabinConfiguration().totalSeats() >= minSeats)
                result.add(a);
        }
        return result;
    }

    /**
     * Returns fleet aircraft manufactured in or after the given year.
     *
     * @param fromYear lower bound year (inclusive)
     * @return matching aircraft
     */
    public List<Aircraft> fleetByManufactureYearFrom(final int fromYear) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ATCC, AiSafeRoles.ADMIN);
        final List<Aircraft> result = new ArrayList<>();
        for (final Aircraft a : loadFleet(resolveCompany())) {
            if (a.yearOfManufacture() >= fromYear)
                result.add(a);
        }
        return result;
    }
}
