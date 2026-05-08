package aisafe.maker.application;

import aisafe.maker.domain.Maker;
import aisafe.maker.repositories.MakerRepository;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

@UseCaseController
public class RegisterMakerController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final MakerRepository makerRepository =
            PersistenceContext.repositories().makers();

    public Maker registerMaker(final String name, final String country) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.BACKOFFICE_OPERATOR, AiSafeRoles.ADMIN);

        if (makerRepository.ofIdentity(name.trim()).isPresent()) {
            throw new IllegalArgumentException(
                    "A maker with name '" + name + "' already exists.");
        }

        return makerRepository.save(new Maker(name, country));
    }

    public Iterable<Maker> allMakers() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.BACKOFFICE_OPERATOR, AiSafeRoles.ADMIN);
        return makerRepository.findAll();
    }
}
