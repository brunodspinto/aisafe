package aisafe.maker.repositories;

import aisafe.maker.domain.Maker;
import aisafe.maker.domain.MakerName;
import eapli.framework.domain.repositories.DomainRepository;

/**
 * Repository interface for {@link Maker} aggregate roots, keyed by manufacturer name.
 */
public interface MakerRepository extends DomainRepository<MakerName, Maker> {
}
