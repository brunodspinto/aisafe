package aisafe.enginemodel.application;

import aisafe.enginemodel.domain.EngineModel;
import aisafe.enginemodel.domain.EngineType;
import aisafe.enginemodel.repositories.EngineModelRepository;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.maker.domain.MakerName;
import aisafe.usermanagement.domain.AiSafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * Controller responsible for the use case "Register an Engine Model" (US056).
 */
@UseCaseController
public class RegisterEngineModelController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();

    private final EngineModelRepository repository =
            PersistenceContext.repositories().engineModels();

    /**
     * Returns the available engine types for the UI to present.
     *
     * @return all EngineType enum values
     */
    public EngineType[] engineTypes() {
        return EngineType.values();
    }

    /**
     * Registers a new engine model in the system.
     *
     * @param name       the model name
     * @param makerName  the manufacturer name
     * @param engineType the engine type
     * @param thrust     the thrust in kN (must be &gt; 0)
     * @param tsfc       the thrust-specific fuel consumption (must be &gt; 0)
     * @return the saved EngineModel
     * @throws IllegalArgumentException if a model with the same name and maker already exists
     */
    public EngineModel registerEngineModel(final String name, final String makerName,
                                           final EngineType engineType,
                                           final double thrustAtStandstill,
                                           final double thrustAtCruiseSpeed, final double tsfc) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.BACKOFFICE_OPERATOR, AiSafeRoles.ADMIN);

        final String trimmedName = name == null ? null : name.trim();
        final String trimmedMaker = makerName == null ? null : makerName.trim();

        if (repository.findByNameAndMaker(trimmedName, trimmedMaker).isPresent()) {
            throw new IllegalArgumentException(
                    "An engine model with name '" + trimmedName + "' and maker '" + trimmedMaker + "' already exists.");
        }

        final EngineModel model = new EngineModel(trimmedName, MakerName.valueOf(trimmedMaker), engineType, thrustAtStandstill, thrustAtCruiseSpeed, tsfc);
        return repository.save(model);
    }
}
