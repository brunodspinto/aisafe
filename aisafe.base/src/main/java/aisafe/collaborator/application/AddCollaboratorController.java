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
import aisafe.usermanagement.domain.MecanographicNumber;
import aisafe.usermanagement.domain.SecurityClearance;
import aisafe.usermanagement.domain.User;
import aisafe.usermanagement.repositories.UserRepository;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserManagementService;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.time.util.CurrentTimeCalendars;

import java.time.LocalDate;
import java.util.Set;

@UseCaseController
public class AddCollaboratorController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final UserManagementService userSvc = AuthzRegistry.userService();
    private final UserRepository userRepo = PersistenceContext.repositories().users();
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

        final User user = createUser(username, password, firstName, lastName,
                emailStr, roles, phoneNumber, position, email,
                securityClearance, skillsAssessmentDate);

        return collaboratorRepo.save(new Collaborator(user, company));
    }

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

        final AirControlArea area = areaRepo.ofIdentity(areaCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Air Control Area '" + areaCode + "' not found."));

        final User user = createUser(username, password, firstName, lastName,
                emailStr, roles, phoneNumber, position, email,
                securityClearance, skillsAssessmentDate);

        return collaboratorRepo.save(new Collaborator(user, area));
    }

    private User createUser(final String username, final String password,
                            final String firstName, final String lastName,
                            final String emailStr, final Set<Role> roles,
                            final String phoneNumber, final String position,
                            final Email email, final SecurityClearance securityClearance,
                            final LocalDate skillsAssessmentDate) {

        final SystemUser systemUser = userSvc.registerNewUser(
                username, password, firstName, lastName, emailStr, roles,
                CurrentTimeCalendars.now());

        final MecanographicNumber mecNumber =
                MecanographicNumber.valueOf(String.valueOf(System.currentTimeMillis()));

        final User user = new User(systemUser, mecNumber, phoneNumber, email,
                position, securityClearance, skillsAssessmentDate);

        return userRepo.save(user);
    }
}
