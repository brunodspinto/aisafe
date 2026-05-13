package aisafe.infrastructure.persistence.inmemory;

import aisafe.maker.domain.Maker;
import aisafe.maker.domain.MakerName;
import aisafe.maker.repositories.MakerRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

public class InMemoryMakerRepository
        extends InMemoryDomainRepository<Maker, MakerName>
        implements MakerRepository {
}
