package aisafe.usermanagement.application;

import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafeRoles;
import aisafe.usermanagement.domain.User;
import aisafe.usermanagement.repositories.UserRepository;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

@UseCaseController
public class ListUsersController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final UserRepository userRepo = PersistenceContext.repositories().users();

    public Iterable<User> allUsers() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ADMIN);
        return userRepo.findAll();
    }
}
