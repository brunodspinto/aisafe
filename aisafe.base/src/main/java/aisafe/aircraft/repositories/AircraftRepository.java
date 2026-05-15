package aisafe.aircraft.repositories;

import aisafe.aircraft.domain.Aircraft;
import aisafe.aircraft.domain.RegistrationNumber;
import aisafe.aircraftmodel.domain.AircraftModel;
import aisafe.enginemodel.domain.EngineModel;
import eapli.framework.domain.repositories.DomainRepository;

/**
 * Repository interface for {@link Aircraft} aggregate roots, keyed by registration number.
 */
public interface AircraftRepository extends DomainRepository<RegistrationNumber, Aircraft> {

    /**
     * Checks whether any aircraft of the given model still has the given engine in its
     * model's certified engines list. Used by US058 to prevent removal of an engine model
     * that is currently used by aircraft in service.
     *
     * @param model  the aircraft model whose engine list is being modified
     * @param engine the engine model proposed for removal
     * @return {@code true} if at least one aircraft of that model exists in the fleet
     */
    boolean existsAircraftUsingModelEngine(AircraftModel model, EngineModel engine);
}
