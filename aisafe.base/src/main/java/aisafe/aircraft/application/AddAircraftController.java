package aisafe.aircraft.application;

import aisafe.aircraft.domain.Aircraft;
import aisafe.aircraft.domain.CabinConfiguration;
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

@UseCaseController
public class AddAircraftController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AircraftRepository aircraftRepo = PersistenceContext.repositories().aircraft();
    private final AircraftModelRepository modelRepo = PersistenceContext.repositories().aircraftModels();
    private final AirTransportCompanyRepository companyRepo = PersistenceContext.repositories().airTransportCompanies();
    private final CollaboratorRepository collaboratorRepo = PersistenceContext.repositories().collaborators();

    public Iterable<AircraftModel> allAircraftModels() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ATCC, AiSafeRoles.ADMIN);
        return modelRepo.findAll();
    }

    public AirTransportCompany addAircraft(final AircraftModel model,
                                           final String registrationNumber,
                                           final String registeredCountry,
                                           final int numberOfCrewElements,
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
        final Aircraft aircraft = new Aircraft(registrationNumber, registeredCountry, numberOfCrewElements, cabin, model);
        aircraftRepo.save(aircraft);

        company.addAircraftToFleet(aircraft);
        return companyRepo.save(company);
    }
}
