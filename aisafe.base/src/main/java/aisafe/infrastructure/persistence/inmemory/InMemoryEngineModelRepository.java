package aisafe.infrastructure.persistence.inmemory;

import aisafe.enginemodel.domain.EngineModel;
import aisafe.enginemodel.repositories.EngineModelRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainAutoNumberRepository;
import java.util.Optional;

public class InMemoryEngineModelRepository
        extends InMemoryDomainAutoNumberRepository<EngineModel>
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
