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

@UseCaseController
public class RegisterAircraftModelController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AircraftModelRepository aircraftModelRepository =
            PersistenceContext.repositories().aircraftModels();
    private final MakerRepository makerRepository =
            PersistenceContext.repositories().makers();
    private final EngineModelRepository engineModelRepository =
            PersistenceContext.repositories().engineModels();

    public Iterable<Maker> allMakers() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.BACKOFFICE_OPERATOR, AiSafeRoles.ADMIN);
        return makerRepository.findAll();
    }

    public Iterable<EngineModel> allEngineModels() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.BACKOFFICE_OPERATOR, AiSafeRoles.ADMIN);
        return engineModelRepository.findAll();
    }

    public AircraftType[] aircraftTypes() {
        return AircraftType.values();
    }

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
