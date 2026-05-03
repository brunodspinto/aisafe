package aisafe.usermanagement.application;

import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafeRoles;
import aisafe.usermanagement.domain.User;
import aisafe.usermanagement.repositories.UserRepository;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserManagementService;
import eapli.framework.infrastructure.authz.domain.model.Username;

@UseCaseController
public class DisableEnableUserController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final UserManagementService userSvc = AuthzRegistry.userService();
    private final UserRepository userRepo = PersistenceContext.repositories().users();

    public Iterable<User> allUsers() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ADMIN);
        return userRepo.findAll();
    }

    public boolean toggleUser(final Username username) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ADMIN);
        final User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        final var systemUser = user.systemUser();
        if (systemUser.isActive()) {
            userSvc.deactivateUser(systemUser);
            return false;
        } else {
            userSvc.activateUser(systemUser);
            return true;
        }
    }
}
