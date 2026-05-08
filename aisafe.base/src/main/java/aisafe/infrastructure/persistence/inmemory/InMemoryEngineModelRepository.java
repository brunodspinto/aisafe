package aisafe.infrastructure.persistence.inmemory;

import aisafe.enginemodel.domain.EngineModel;
import aisafe.enginemodel.repositories.EngineModelRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import java.util.Optional;

public class InMemoryEngineModelRepository
        extends InMemoryDomainRepository<EngineModel, Long>
        implements EngineModelRepository {

    @Override
    public Optional<EngineModel> findByNameAndMaker(final String name, final String makerName) {
        for (final EngineModel model : findAll()) {
            if (model.name().equals(name) && model.makerName().equals(makerName)) {
                return Optional.of(model);
            }
        }
        return Optional.empty();
    }
}
