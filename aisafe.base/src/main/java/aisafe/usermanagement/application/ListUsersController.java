package aisafe.usermanagement.application;

import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafeRoles;
import aisafe.usermanagement.domain.User;
import aisafe.usermanagement.repositories.UserRepository;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * Application-layer controller for the "List Users" use case (US030).
 * Retrieves all registered users for display by an administrator.
 * Requires the authenticated user to have the {@code ADMIN} role.
 */
@UseCaseController
public class ListUsersController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final UserRepository userRepo = PersistenceContext.repositories().users();

    /**
     * Returns all registered users regardless of their active/inactive status.
     *
     * @return iterable of all {@link User} aggregates
     */
    public Iterable<User> allUsers() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ADMIN);
        return userRepo.findAll();
    }
}
