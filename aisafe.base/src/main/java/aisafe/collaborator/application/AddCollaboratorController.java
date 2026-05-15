package aisafe.collaborator.application;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.aircontrolarea.domain.AirControlAreaCode;
import aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.airtransportcompany.repositories.AirTransportCompanyRepository;
import aisafe.collaborator.domain.Collaborator;
import aisafe.collaborator.repositories.CollaboratorRepository;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafePasswordPolicy;
import aisafe.usermanagement.domain.AiSafeRoles;
import aisafe.usermanagement.domain.Email;
import aisafe.usermanagement.domain.MecanographicNumber;
import aisafe.usermanagement.domain.SecurityClearance;
import aisafe.usermanagement.domain.User;
import aisafe.usermanagement.repositories.UserRepository;
import eapli.framework.application.UseCaseController;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserManagementService;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.time.util.CurrentTimeCalendars;

import java.time.LocalDate;
import java.util.Set;

/**
 * Application-layer controller for the "Add Customer's Collaborator" use case (US060).
 * Supports creating collaborators for both air transport companies and air control areas.
 * Requires a Back-Office Operator or Admin role.
 */
@UseCaseController
public class AddCollaboratorController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();

    private final TransactionalContext tx =
            PersistenceContext.repositories().newTransactionalContext();
    private final eapli.framework.infrastructure.authz.domain.repositories.UserRepository systemUserRepo =
            PersistenceContext.repositories().systemUsers(tx);
    private final UserManagementService txUserSvc =
            new UserManagementService(systemUserRepo, new AiSafePasswordPolicy(), new PlainTextEncoder());
    private final UserRepository userRepo =
            PersistenceContext.repositories().users(tx);
    private final CollaboratorRepository collaboratorRepo =
            PersistenceContext.repositories().collaborators(tx);
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
     * Creates a new system user and registers them as a collaborator of the given company.
     *
     * @param username             system username
     * @param password             system password
     * @param firstName            first name
     * @param lastName             last name
     * @param emailStr             e-mail for the system user
     * @param roles                roles to assign in the system
     * @param phoneNumber          phone number
     * @param position             job position
     * @param email                e-mail value object for the AISafe user
     * @param securityClearance    security clearance level and expiry
     * @param skillsAssessmentDate date of the last skills assessment
     * @param companyIataCode      IATA code of the company to associate with
     * @return the saved {@link Collaborator}
     * @throws IllegalArgumentException if the company code is not found
     */
    public Collaborator addCompanyCollaborator(final String username,
                                               final String password,
                                               final String firstName,
                                               final String lastName,
                                               final String emailStr,
                                               final Set<Role> roles,
                                               final String phoneNumber,
                                               final String position,
                                               final Email email,
                                               final SecurityClearance securityClearance,
                                               final LocalDate skillsAssessmentDate,
                                               final String companyIataCode) {

        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.BACKOFFICE_OPERATOR, AiSafeRoles.ADMIN);

        final AirTransportCompany company = companyRepo.ofIdentity(
                aisafe.airtransportcompany.domain.IATACode.valueOf(companyIataCode))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Air Transport Company '" + companyIataCode + "' not found."));

        if (tx != null) tx.beginTransaction();
        final User user = createUser(username, password, firstName, lastName,
                emailStr, roles, phoneNumber, position, email,
                securityClearance, skillsAssessmentDate);
        final Collaborator collab = collaboratorRepo.save(new Collaborator(user, company));
        if (tx != null) tx.commit();
        return collab;
    }

    /**
     * Creates a new system user and registers them as a collaborator of the given air control area.
     *
     * @param username             system username
     * @param password             system password
     * @param firstName            first name
     * @param lastName             last name
     * @param emailStr             e-mail for the system user
     * @param roles                roles to assign in the system
     * @param phoneNumber          phone number
     * @param position             job position
     * @param email                e-mail value object for the AISafe user
     * @param securityClearance    security clearance level and expiry
     * @param skillsAssessmentDate date of the last skills assessment
     * @param areaCode             code of the air control area to associate with
     * @return the saved {@link Collaborator}
     * @throws IllegalArgumentException if the area code is not found
     */
    public Collaborator addAreaCollaborator(final String username,
                                            final String password,
                                            final String firstName,
                                            final String lastName,
                                            final String emailStr,
                                            final Set<Role> roles,
                                            final String phoneNumber,
                                            final String position,
                                            final Email email,
                                            final SecurityClearance securityClearance,
                                            final LocalDate skillsAssessmentDate,
                                            final String areaCode) {

        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.BACKOFFICE_OPERATOR, AiSafeRoles.ADMIN);

        final AirControlArea area = areaRepo.ofIdentity(AirControlAreaCode.valueOf(areaCode))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Air Control Area '" + areaCode + "' not found."));

        if (tx != null) tx.beginTransaction();
        final User user = createUser(username, password, firstName, lastName,
                emailStr, roles, phoneNumber, position, email,
                securityClearance, skillsAssessmentDate);
        final Collaborator collab = collaboratorRepo.save(new Collaborator(user, area));
        if (tx != null) tx.commit();
        return collab;
    }

    private User createUser(final String username, final String password,
                            final String firstName, final String lastName,
                            final String emailStr, final Set<Role> roles,
                            final String phoneNumber, final String position,
                            final Email email, final SecurityClearance securityClearance,
                            final LocalDate skillsAssessmentDate) {

        final SystemUser systemUser = txUserSvc.registerNewUser(
                username, password, firstName, lastName, emailStr, roles,
                CurrentTimeCalendars.now());

        final MecanographicNumber mecNumber =
                MecanographicNumber.valueOf(java.util.UUID.randomUUID().toString());

        final User user = new User(systemUser, mecNumber, phoneNumber, email,
                position, securityClearance, skillsAssessmentDate);

        return userRepo.save(user);
    }
}
