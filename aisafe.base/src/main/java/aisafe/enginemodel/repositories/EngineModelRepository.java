package aisafe.enginemodel.repositories;

import aisafe.enginemodel.domain.EngineModel;
import eapli.framework.domain.repositories.DomainRepository;
import java.util.Optional;

/**
 * Repository for the EngineModel aggregate.
 */
public interface EngineModelRepository extends DomainRepository<Long, EngineModel> {

    /**
     * Finds an engine model by its name and maker name combination.
     *
     * @param name      the model name
     * @param makerName the manufacturer name
     * @return an Optional containing the matching model, or empty if not found
     */
    Optional<EngineModel> findByNameAndMaker(String name, String makerName);
}
