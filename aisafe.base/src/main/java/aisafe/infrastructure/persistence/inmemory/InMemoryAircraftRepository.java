package aisafe.infrastructure.persistence.inmemory;

import aisafe.aircraft.domain.Aircraft;
import aisafe.aircraft.repositories.AircraftRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

public class InMemoryAircraftRepository
        extends InMemoryDomainRepository<Aircraft, String>
        implements AircraftRepository {
}
