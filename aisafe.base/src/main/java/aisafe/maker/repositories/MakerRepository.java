package aisafe.maker.repositories;

import aisafe.maker.domain.Maker;
import eapli.framework.domain.repositories.DomainRepository;

/**
 * Repository interface for {@link Maker} aggregate roots, keyed by manufacturer name.
 */
public interface MakerRepository extends DomainRepository<String, Maker> {
}
