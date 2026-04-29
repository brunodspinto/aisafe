package aisafe.usermanagement.application;

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
public class AddUserController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final UserManagementService userSvc = AuthzRegistry.userService();
    private final UserRepository userRepo = PersistenceContext.repositories().users();

    public Role[] getRoleTypes() {
        return AiSafeRoles.nonUserValues();
    }

    public User addUser(final String username, final String password,
                        final String firstName, final String lastName,
                        final String emailStr, final Set<Role> roles,
                        final String phoneNumber, final String position,
                        final Email email,
                        final SecurityClearance securityClearance,
                        final LocalDate skillsAssessmentDate) {

        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ADMIN);

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
