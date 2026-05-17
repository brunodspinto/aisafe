package aisafe.aircraftmodel.application;

import aisafe.aircraftmodel.domain.AircraftModel;
import aisafe.aircraftmodel.repositories.AircraftModelRepository;
import aisafe.enginemodel.domain.EngineModel;
import aisafe.enginemodel.repositories.EngineModelRepository;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * Application-layer controller for the "Add Engine to Aircraft Model" use case (US056).
 * Requires a Back-Office Operator or Admin role.
 */
@UseCaseController
public class AddEngineToAircraftModelController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AircraftModelRepository aircraftModelRepository =
            PersistenceContext.repositories().aircraftModels();
    private final EngineModelRepository engineModelRepository =
            PersistenceContext.repositories().engineModels();

    /**
     * Returns all registered aircraft models for selection in the UI.
     *
     * @return all {@link AircraftModel} instances
     */
    public Iterable<AircraftModel> allAircraftModels() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.BACKOFFICE_OPERATOR, AiSafeRoles.ADMIN);
        return aircraftModelRepository.findAll();
    }

    /**
     * Returns all registered engine models for selection in the UI.
     *
     * @return all {@link EngineModel} instances
     */
    public Iterable<EngineModel> allEngineModels() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.BACKOFFICE_OPERATOR, AiSafeRoles.ADMIN);
        return engineModelRepository.findAll();
    }

    /**
     * Certifies the given engine for the given aircraft model and persists the change.
     *
     * @param aircraftModel the target aircraft model
     * @param engine        the engine to certify
     * @return the updated {@link AircraftModel}
     * @throws IllegalArgumentException if the engine is already certified or of a different type
     */
    public AircraftModel addEngine(final AircraftModel aircraftModel, final EngineModel engine) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.BACKOFFICE_OPERATOR, AiSafeRoles.ADMIN);
        aircraftModel.addEngine(engine);
        return aircraftModelRepository.save(aircraftModel);
    }
}
