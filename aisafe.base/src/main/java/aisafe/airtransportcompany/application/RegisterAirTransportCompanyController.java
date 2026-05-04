package aisafe.airtransportcompany.application;

import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.airtransportcompany.domain.IATACode;
import aisafe.airtransportcompany.domain.ICAOCode;
import aisafe.airtransportcompany.repositories.AirTransportCompanyRepository;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

@UseCaseController
public class RegisterAirTransportCompanyController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AirTransportCompanyRepository repo = PersistenceContext.repositories().airTransportCompanies();

    public AirTransportCompany registerCompany(final String name, final String iataCode, final String icaoCode) {

        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.BACKOFFICE_OPERATOR);
        final AirTransportCompany company = new AirTransportCompany(
                name, IATACode.valueOf(iataCode), ICAOCode.valueOf(icaoCode));
        return repo.save(company);
    }
}
