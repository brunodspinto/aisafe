package aisafe.aircraftmodel.repositories;

import aisafe.aircraftmodel.domain.AircraftModel;
import aisafe.maker.domain.Maker;
import eapli.framework.domain.repositories.DomainRepository;

import java.util.Optional;

/**
 * Repository interface for {@link AircraftModel} aggregate roots, keyed by auto-generated {@code Long} id.
 */
public interface AircraftModelRepository extends DomainRepository<Long, AircraftModel> {

    /**
     * Finds an aircraft model by its name and maker combination (uniqueness constraint).
     *
     * @param modelName the model name
     * @param maker     the manufacturer
     * @return an {@code Optional} with the matching model, or empty if not found
     */
    Optional<AircraftModel> findByModelNameAndMaker(String modelName, Maker maker);
}
