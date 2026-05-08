package aisafe.infrastructure.persistence.inmemory;

import aisafe.aircraftmodel.domain.AircraftModel;
import aisafe.aircraftmodel.repositories.AircraftModelRepository;
import aisafe.maker.domain.Maker;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

import java.util.Optional;

public class InMemoryAircraftModelRepository
        extends InMemoryDomainRepository<AircraftModel, Long>
        implements AircraftModelRepository {

    @Override
    public Optional<AircraftModel> findByModelNameAndMaker(final String modelName, final Maker maker) {
        return matchOne(m -> m.modelName().equalsIgnoreCase(modelName)
                && m.maker().equals(maker));
    }
}
