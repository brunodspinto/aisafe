package aisafe.aircraftmodel.application;

import aisafe.aircraftmodel.domain.AircraftModel;
import aisafe.aircraftmodel.domain.AircraftType;
import aisafe.aircraftmodel.repositories.AircraftModelRepository;
import aisafe.enginemodel.domain.EngineModel;
import aisafe.enginemodel.repositories.EngineModelRepository;
import aisafe.maker.domain.Maker;
import aisafe.maker.repositories.MakerRepository;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * Application-layer controller for the "Register Aircraft Model" use case (US056).
 * Requires a Back-Office Operator or Admin role.
 */
@UseCaseController
public class RegisterAircraftModelController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AircraftModelRepository aircraftModelRepository =
            PersistenceContext.repositories().aircraftModels();
    private final MakerRepository makerRepository =
            PersistenceContext.repositories().makers();
    private final EngineModelRepository engineModelRepository =
            PersistenceContext.repositories().engineModels();

    /**
     * Returns all registered manufacturers for selection in the UI.
     *
     * @return all {@link Maker} instances
     */
    public Iterable<Maker> allMakers() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.BACKOFFICE_OPERATOR, AiSafeRoles.ADMIN);
        return makerRepository.findAll();
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
     * Returns all available aircraft type values for selection in the UI.
     *
     * @return array of {@link AircraftType} enum values
     */
    public AircraftType[] aircraftTypes() {
        return AircraftType.values();
    }

    /**
     * Validates and persists a new aircraft model.
     * Enforces uniqueness of the modelName + maker combination.
     *
     * @param modelName        commercial name of the model
     * @param makerName        name of an existing {@link Maker}
     * @param aircraftTypeName name of an {@link AircraftType} constant
     * @param emptyWeight      operating empty weight in kg
     * @param mtow             maximum take-off weight in kg
     * @param mzfw             maximum zero-fuel weight in kg
     * @param maxFuelCapacity  maximum fuel capacity in kg
     * @param serviceCeiling   maximum operating altitude in metres
     * @param cruiseSpeed      typical cruise speed in m/s
     * @param wingSpan         wing span in metres
     * @param wingArea         wing area in m²
     * @param dragCoefficient  aerodynamic drag coefficient
     * @param liftCoefficient  aerodynamic lift coefficient
     * @param engine           first certified engine model
     * @return the saved {@link AircraftModel}
     * @throws IllegalArgumentException if the maker is not found, the model already exists, or any value is invalid
     */
    public AircraftModel registerAircraftModel(final String modelName,
                                               final String makerName,
                                               final String aircraftTypeName,
                                               final double emptyWeight,
                                               final double mtow,
                                               final double mzfw,
                                               final double maxFuelCapacity,
                                               final double serviceCeiling,
                                               final double cruiseSpeed,
                                               final double wingSpan,
                                               final double wingArea,
                                               final double dragCoefficient,
                                               final double liftCoefficient,
                                               final EngineModel engine) {

        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.BACKOFFICE_OPERATOR, AiSafeRoles.ADMIN);

        final Maker maker = makerRepository.ofIdentity(makerName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Maker '" + makerName + "' not found."));

        final AircraftType aircraftType = AircraftType.valueOf(aircraftTypeName.toUpperCase());

        if (aircraftModelRepository.findByModelNameAndMaker(modelName, maker).isPresent()) {
            throw new IllegalArgumentException(
                    "An aircraft model with name '" + modelName + "' and maker '" + makerName + "' already exists.");
        }

        final AircraftModel model = new AircraftModel(
                modelName, maker, aircraftType,
                emptyWeight, mtow, mzfw, maxFuelCapacity,
                serviceCeiling, cruiseSpeed, wingSpan, wingArea,
                dragCoefficient, liftCoefficient, engine
        );

        return aircraftModelRepository.save(model);
    }
}
