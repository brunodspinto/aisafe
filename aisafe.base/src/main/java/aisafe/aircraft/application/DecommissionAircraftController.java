package aisafe.aircraft.application;

import aisafe.aircraft.domain.Aircraft;
import aisafe.aircraft.repositories.AircraftRepository;
import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.airtransportcompany.repositories.AirTransportCompanyRepository;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

import java.util.ArrayList;
import java.util.List;

@UseCaseController
public class DecommissionAircraftController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AircraftRepository aircraftRepo =
            PersistenceContext.repositories().aircraft();
    private final AirTransportCompanyRepository companyRepo =
            PersistenceContext.repositories().airTransportCompanies();

    public Iterable<AirTransportCompany> allCompanies() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ATCC, AiSafeRoles.ADMIN);
        return companyRepo.findAll();
    }

    public Iterable<Aircraft> activeAircraftByCompany(final String companyIataCode) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ATCC, AiSafeRoles.ADMIN);
        final List<Aircraft> result = new ArrayList<>();
        for (final Aircraft a : aircraftRepo.findAll()) {
            if (a.isActive()) {
                result.add(a);
            }
        }
        return result;
    }

    public Aircraft decommission(final Aircraft aircraft) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ATCC, AiSafeRoles.ADMIN);
        aircraft.decommission();
        return aircraftRepo.save(aircraft);
    }
}
