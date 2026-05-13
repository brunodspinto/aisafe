package aisafe.aircraft.repositories;

import aisafe.aircraft.domain.Aircraft;
import eapli.framework.domain.repositories.DomainRepository;

/**
 * Repository interface for {@link Aircraft} aggregate roots, keyed by registration number.
 */
public interface AircraftRepository extends DomainRepository<String, Aircraft> {
}
