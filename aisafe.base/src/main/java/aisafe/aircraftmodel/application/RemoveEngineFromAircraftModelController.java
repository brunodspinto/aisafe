package aisafe.aircraftmodel.application;

import aisafe.aircraft.repositories.AircraftRepository;
import aisafe.aircraftmodel.domain.AircraftModel;
import aisafe.aircraftmodel.repositories.AircraftModelRepository;
import aisafe.enginemodel.domain.EngineModel;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * Application-layer controller for the "Remove Engine from Aircraft Model" use case (US058).
 * Requires a Back-Office Operator or Admin role.
 */
@UseCaseController
public class RemoveEngineFromAircraftModelController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AircraftModelRepository aircraftModelRepository =
            PersistenceContext.repositories().aircraftModels();
    private final AircraftRepository aircraftRepository =
            PersistenceContext.repositories().aircraft();

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
     * Removes the certification of the given engine from the given aircraft model and persists the change.
     *
     * @param aircraftModel the target aircraft model
     * @param engine        the engine to de-certify
     * @return the updated {@link AircraftModel}
     * @throws IllegalArgumentException if the engine is not certified, is the last remaining one,
     *                                  or there are aircraft of this model currently using the engine
     */
    public AircraftModel removeEngine(final AircraftModel aircraftModel, final EngineModel engine) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.BACKOFFICE_OPERATOR, AiSafeRoles.ADMIN);

        if (aircraftRepository.existsAircraftUsingModelEngine(aircraftModel, engine)) {
            throw new IllegalArgumentException(
                    "Cannot remove engine '" + engine.name() + "' from model '" + aircraftModel.modelName()
                            + "': there are aircraft in service using this model and engine.");
        }

        aircraftModel.removeEngine(engine);
        return aircraftModelRepository.save(aircraftModel);
    }
}
