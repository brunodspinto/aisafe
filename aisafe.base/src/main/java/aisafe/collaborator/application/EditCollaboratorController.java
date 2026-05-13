package aisafe.collaborator.application;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.airtransportcompany.repositories.AirTransportCompanyRepository;
import aisafe.collaborator.domain.Collaborator;
import aisafe.collaborator.repositories.CollaboratorRepository;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafeRoles;
import aisafe.usermanagement.domain.Email;
import aisafe.usermanagement.repositories.UserRepository;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * Application-layer controller for the "Edit Customer's Collaborator" use case (US060).
 * Allows updating a collaborator's contact information (email and phone).
 * Requires a Back-Office Operator or Admin role.
 */
@UseCaseController
public class EditCollaboratorController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final CollaboratorRepository collaboratorRepo =
            PersistenceContext.repositories().collaborators();
    private final AirTransportCompanyRepository companyRepo =
            PersistenceContext.repositories().airTransportCompanies();
    private final AirControlAreaRepository areaRepo =
            PersistenceContext.repositories().airControlAreas();
    private final UserRepository userRepo =
            PersistenceContext.repositories().users();

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
     * Updates the email and phone of the given collaborator and persists the changes.
     *
     * @param collaborator the collaborator to update
     * @param newEmail     new email address
     * @param newPhone     new phone number
     * @return the updated {@link Collaborator}
     */
    public Collaborator updateContact(final Collaborator collaborator,
                                      final String newEmail,
                                      final String newPhone) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.BACKOFFICE_OPERATOR, AiSafeRoles.ADMIN);
        collaborator.user().updateContact(new Email(newEmail), newPhone);
        userRepo.save(collaborator.user());
        return collaboratorRepo.save(collaborator);
    }
}
