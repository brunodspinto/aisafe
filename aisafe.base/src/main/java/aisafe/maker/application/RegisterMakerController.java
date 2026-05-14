package aisafe.maker.application;

import aisafe.maker.domain.Maker;
import aisafe.maker.domain.MakerName;
import aisafe.maker.repositories.MakerRepository;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * Application-layer controller for the "Register Maker" use case (US056).
 * Requires a Back-Office Operator or Admin role.
 */
@UseCaseController
public class RegisterMakerController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final MakerRepository makerRepository =
            PersistenceContext.repositories().makers();

    /**
     * Validates and persists a new maker.
     * Enforces uniqueness of the maker name.
     *
     * @param name    manufacturer name (non-blank); must be unique
     * @param country country of origin (non-blank)
     * @return the saved {@link Maker}
     * @throws IllegalArgumentException if a maker with the same name already exists
     */
    public Maker registerMaker(final String name, final String country) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.BACKOFFICE_OPERATOR, AiSafeRoles.ADMIN);

        if (makerRepository.ofIdentity(MakerName.valueOf(name)).isPresent()) {
            throw new IllegalArgumentException(
                    "A maker with name '" + name + "' already exists.");
        }

        return makerRepository.save(new Maker(MakerName.valueOf(name.trim()), country));
    }

    /**
     * Returns all registered makers.
     *
     * @return all {@link Maker} instances
     */
    public Iterable<Maker> allMakers() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.BACKOFFICE_OPERATOR, AiSafeRoles.ADMIN);
        return makerRepository.findAll();
    }
}
