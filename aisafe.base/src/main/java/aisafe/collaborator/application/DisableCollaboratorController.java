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
import eapli.framework.infrastructure.authz.application.UserManagementService;

/**
 * Application-layer controller for the "Disable Customer's Collaborator" use case (US064).
 * Deactivates the system user of the selected collaborator.
 * Requires a Back-Office Operator or Admin role.
 */
@UseCaseController
public class DisableCollaboratorController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final UserManagementService userSvc = AuthzRegistry.userService();
    private final CollaboratorRepository collaboratorRepo =
            PersistenceContext.repositories().collaborators();
    private final AirTransportCompanyRepository companyRepo =
            PersistenceContext.repositories().airTransportCompanies();
    private final AirControlAreaRepository areaRepo =
            PersistenceContext.repositories().airControlAreas();

    /**
     * Returns all registered air transport companies for selection in the UI.
     *
     * @return all {@link AirTransportCompany} instances
     */
    public Iterable<AirTransportCompany> allCompanies() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.BACKOFFICE_OPERATOR, AiSafeRoles.ADMIN);
        return companyRepo.findAll();
    }

    /**
     * Returns all registered air control areas for selection in the UI.
     *
     * @return all {@link AirControlArea} instances
     */
    public Iterable<AirControlArea> allAreas() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.BACKOFFICE_OPERATOR, AiSafeRoles.ADMIN);
        return areaRepo.findAll();
    }

    /**
     * Returns active collaborators of the given company.
     *
     * @param company the company to filter by
     * @return active collaborators
     */
    public Iterable<Collaborator> activeCollaboratorsByCompany(final AirTransportCompany company) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.BACKOFFICE_OPERATOR, AiSafeRoles.ADMIN);
        return collaboratorRepo.findActiveByAirTransportCompany(company);
    }

    /**
     * Returns active collaborators of the given air control area.
     *
     * @param area the area to filter by
     * @return active collaborators
     */
    public Iterable<Collaborator> activeCollaboratorsByArea(final AirControlArea area) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.BACKOFFICE_OPERATOR, AiSafeRoles.ADMIN);
        return collaboratorRepo.findActiveByAirControlArea(area);
    }

    /**
     * Deactivates the system user account of the given collaborator.
     *
     * @param collaborator the collaborator to disable
     */
    public void disableCollaborator(final Collaborator collaborator) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.BACKOFFICE_OPERATOR, AiSafeRoles.ADMIN);
        userSvc.deactivateUser(collaborator.user().systemUser());
    }
}
