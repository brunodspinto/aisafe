package aisafe.infrastructure.persistence.inmemory;

import aisafe.aircraftmodel.domain.AircraftModel;
import aisafe.aircraftmodel.repositories.AircraftModelRepository;
import aisafe.maker.domain.Maker;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryAircraftModelRepository
        extends InMemoryDomainRepository<AircraftModel, Long>
        implements AircraftModelRepository {

    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public <S extends AircraftModel> S save(final S entity) {
        if (entity.identity() == null) {
            try {
                final Field idField = AircraftModel.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(entity, idGenerator.getAndIncrement());
            } catch (final Exception e) {
                throw new RuntimeException("Failed to set aircraft model id", e);
            }
        }
        return super.save(entity);
    }

    @Override
    public Optional<AircraftModel> findByModelNameAndMaker(final String modelName, final Maker maker) {
        return matchOne(m -> m.modelName().equalsIgnoreCase(modelName)
                && m.maker().equals(maker));
    }
}
