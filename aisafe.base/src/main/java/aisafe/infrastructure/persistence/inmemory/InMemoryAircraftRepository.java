package aisafe.infrastructure.persistence.inmemory;

import aisafe.aircraft.domain.Aircraft;
import aisafe.aircraft.domain.RegistrationNumber;
import aisafe.aircraft.repositories.AircraftRepository;
import aisafe.aircraftmodel.domain.AircraftModel;
import aisafe.enginemodel.domain.EngineModel;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

public class InMemoryAircraftRepository
        extends InMemoryDomainRepository<Aircraft, RegistrationNumber>
        implements AircraftRepository {

    @Override
    public boolean existsAircraftUsingModelEngine(final AircraftModel model, final EngineModel engine) {
        if (model == null || engine == null) return false;
        for (final Aircraft a : findAll()) {
            if (!a.aircraftModel().equals(model)) continue;
            for (final EngineModel certified : a.aircraftModel().certifiedEngines()) {
                if (certified.name().equals(engine.name())
                        && certified.makerName().equals(engine.makerName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
