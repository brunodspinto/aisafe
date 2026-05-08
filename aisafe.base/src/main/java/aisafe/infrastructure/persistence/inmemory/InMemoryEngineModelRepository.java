package aisafe.infrastructure.persistence.inmemory;

import aisafe.enginemodel.domain.EngineModel;
import aisafe.enginemodel.repositories.EngineModelRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryEngineModelRepository
        extends InMemoryDomainRepository<EngineModel, Long>
        implements EngineModelRepository {

    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public <S extends EngineModel> S save(final S entity) {
        if (entity.identity() == null) {
            try {
                final Field idField = EngineModel.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(entity, idGenerator.getAndIncrement());
            } catch (final Exception e) {
                throw new RuntimeException("Failed to set engine model id", e);
            }
        }
        return super.save(entity);
    }

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
