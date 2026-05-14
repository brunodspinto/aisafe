package aisafe.aircraft.application;

import aisafe.aircraft.domain.Aircraft;
import aisafe.aircraft.domain.CabinConfiguration;
import aisafe.aircraft.domain.RegistrationNumber;
import aisafe.aircraft.repositories.AircraftRepository;
import aisafe.aircraftmodel.domain.AircraftModel;
import aisafe.aircraftmodel.repositories.AircraftModelRepository;
import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.airtransportcompany.repositories.AirTransportCompanyRepository;
import aisafe.collaborator.repositories.CollaboratorRepository;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;

/**
 * Application-layer controller for the "Add Aircraft to Fleet" use case (US070).
 * Requires the authenticated user to be an ATCC or Admin collaborator of an air transport company.
 */
@UseCaseController
public class AddAircraftController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AircraftRepository aircraftRepo = PersistenceContext.repositories().aircraft();
    private final AircraftModelRepository modelRepo = PersistenceContext.repositories().aircraftModels();
    private final AirTransportCompanyRepository companyRepo = PersistenceContext.repositories().airTransportCompanies();
    private final CollaboratorRepository collaboratorRepo = PersistenceContext.repositories().collaborators();

    /**
     * Returns all registered aircraft models for selection in the UI.
     *
     * @return all {@link AircraftModel} instances
     */
    public Iterable<AircraftModel> allAircraftModels() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ATCC, AiSafeRoles.ADMIN);
        return modelRepo.findAll();
    }

    /**
     * Creates and persists a new {@link Aircraft}, adds it to the authenticated user's company fleet,
     * and returns the updated {@link AirTransportCompany}.
     *
     * @param model                the aircraft model
     * @param registrationNumber   unique registration number
     * @param registeredCountry    country of registration
     * @param numberOfCrewElements minimum crew size
     * @param yearOfManufacture    year of manufacture
     * @param firstClassSeats      number of first-class seats
     * @param businessClassSeats   number of business-class seats
     * @param economyClassSeats    number of economy-class seats
     * @return the updated company after adding the aircraft
     * @throws IllegalStateException if the session is invalid or the user is not a company collaborator
     */
    public AirTransportCompany addAircraft(final AircraftModel model,
                                           final String registrationNumber,
                                           final String registeredCountry,
                                           final int numberOfCrewElements,
                                           final int yearOfManufacture,
                                           final int firstClassSeats,
                                           final int businessClassSeats,
                                           final int economyClassSeats) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ATCC, AiSafeRoles.ADMIN);

        final SystemUser su = authz.session()
                .orElseThrow(() -> new IllegalStateException("No active session."))
                .authenticatedUser();

        final AirTransportCompany company = collaboratorRepo.findBySystemUser(su)
                .map(c -> {
                    if (!c.isCompanyCollaborator())
                        throw new IllegalStateException("Authenticated user is not a company collaborator.");
                    return c.airTransportCompany();
                })
                .orElseThrow(() -> new IllegalStateException("No collaborator found for authenticated user."));

        final CabinConfiguration cabin = new CabinConfiguration(firstClassSeats, businessClassSeats, economyClassSeats);
        final Aircraft aircraft = new Aircraft(RegistrationNumber.valueOf(registrationNumber), registeredCountry, numberOfCrewElements, yearOfManufacture, cabin, model);
        aircraftRepo.save(aircraft);

        company.addAircraftToFleet(aircraft);
        return companyRepo.save(company);
    }
}
