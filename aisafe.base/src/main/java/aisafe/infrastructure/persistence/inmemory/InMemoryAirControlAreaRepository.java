package aisafe.infrastructure.persistence.inmemory;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

public class InMemoryAirControlAreaRepository
        extends InMemoryDomainRepository<AirControlArea, String>
        implements AirControlAreaRepository {
}
