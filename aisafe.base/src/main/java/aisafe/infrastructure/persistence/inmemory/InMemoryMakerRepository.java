package aisafe.infrastructure.persistence.inmemory;

import aisafe.maker.domain.Maker;
import aisafe.maker.repositories.MakerRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

public class InMemoryMakerRepository
        extends InMemoryDomainRepository<Maker, String>
        implements MakerRepository {
}
