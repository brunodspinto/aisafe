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

/**
 * Application-layer controller for the "Disable/Enable User" use case (US032).
 * Toggles the active state of a system user's account.
 * Requires the authenticated user to have the {@code ADMIN} role.
 */
@UseCaseController
public class DisableEnableUserController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final UserManagementService userSvc = AuthzRegistry.userService();
    private final UserRepository userRepo = PersistenceContext.repositories().users();

    /**
     * Returns all registered users (active and inactive).
     *
     * @return iterable of all {@link User} aggregates
     */
    public Iterable<User> allUsers() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ADMIN);
        return userRepo.findAll();
    }

    /**
     * Toggles the active/inactive state of the user identified by {@code username}.
     * If the user is currently active, their account is deactivated; otherwise it is activated.
     *
     * @param username the username of the account to toggle
     * @return {@code true} if the account is now active; {@code false} if it is now disabled
     * @throws IllegalArgumentException if no user is found for the given username
     */
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
