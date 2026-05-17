package aisafe.aircraft.application;

import aisafe.aircraft.domain.Aircraft;
import aisafe.aircraft.domain.RegistrationNumber;
import aisafe.aircraft.repositories.AircraftRepository;
import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.airtransportcompany.domain.IATACode;
import aisafe.airtransportcompany.repositories.AirTransportCompanyRepository;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Application-layer controller for the "Decommission Aircraft" use case (US071).
 * Only ATCC or Admin roles may decommission aircraft.
 */
@UseCaseController
public class DecommissionAircraftController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AircraftRepository aircraftRepo =
            PersistenceContext.repositories().aircraft();
    private final AirTransportCompanyRepository companyRepo =
            PersistenceContext.repositories().airTransportCompanies();

    /**
     * Returns all registered air transport companies for selection in the UI.
     *
     * @return all {@link AirTransportCompany} instances
     */
    public Iterable<AirTransportCompany> allCompanies() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ATCC, AiSafeRoles.ADMIN);
        return companyRepo.findAll();
    }

    /**
     * Returns all active aircraft for the given company IATA code.
     *
     * @param companyIataCode IATA code of the company
     * @return active aircraft belonging to that company
     */
    public Iterable<Aircraft> activeAircraftByCompany(final String companyIataCode) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ATCC, AiSafeRoles.ADMIN);
        final AirTransportCompany company = companyRepo.ofIdentity(IATACode.valueOf(companyIataCode))
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyIataCode));
        final List<Aircraft> result = new ArrayList<>();
        for (final String reg : company.fleet()) {
            aircraftRepo.ofIdentity(RegistrationNumber.valueOf(reg))
                    .filter(Aircraft::isActive)
                    .ifPresent(result::add);
        }
        return result;
    }

    /**
     * Decommissions the given aircraft and persists the change.
     *
     * @param aircraft the aircraft to decommission; must be currently active
     * @return the updated (decommissioned) aircraft
     * @throws IllegalStateException if the aircraft is already decommissioned
     */
    public Aircraft decommission(final Aircraft aircraft) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ATCC, AiSafeRoles.ADMIN);
        aircraft.decommission();
        return aircraftRepo.save(aircraft);
    }
}
