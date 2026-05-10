package aisafe.collaborator.application;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.airtransportcompany.repositories.AirTransportCompanyRepository;
import aisafe.collaborator.domain.Collaborator;
import aisafe.collaborator.repositories.CollaboratorRepository;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

@UseCaseController
public class ListCollaboratorsByCustomerController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final CollaboratorRepository collaboratorRepo =
            PersistenceContext.repositories().collaborators();
    private final AirTransportCompanyRepository companyRepo =
            PersistenceContext.repositories().airTransportCompanies();
    private final AirControlAreaRepository areaRepo =
            PersistenceContext.repositories().airControlAreas();

    public Iterable<AirTransportCompany> allCompanies() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.BACKOFFICE_OPERATOR, AiSafeRoles.ADMIN);
        return companyRepo.findAll();
    }

    public Iterable<AirControlArea> allAreas() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.BACKOFFICE_OPERATOR, AiSafeRoles.ADMIN);
        return areaRepo.findAll();
    }

    public Iterable<Collaborator> listActiveByCompany(final AirTransportCompany company) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.BACKOFFICE_OPERATOR, AiSafeRoles.ADMIN);
        return collaboratorRepo.findActiveByAirTransportCompany(company);
    }

    public Iterable<Collaborator> listActiveByArea(final AirControlArea area) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.BACKOFFICE_OPERATOR, AiSafeRoles.ADMIN);
        return collaboratorRepo.findActiveByAirControlArea(area);
    }
}
