package aisafe.pilot.repositories;

import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.pilot.domain.Pilot;
import eapli.framework.domain.repositories.DomainRepository;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;

import java.util.Optional;

/**
 * Repository interface for {@link Pilot} aggregate roots, keyed by auto-generated {@code Long} id.
 */
public interface PilotRepository extends DomainRepository<Long, Pilot> {

    /**
     * Returns all pilots (active and inactive) belonging to the given air transport company.
     *
     * @param company the air transport company to filter by
     * @return pilots of that company
     */
    Iterable<Pilot> findByAirTransportCompany(AirTransportCompany company);

    /**
     * Finds the pilot backed by the given system user.
     *
     * @param systemUser the system user
     * @return an {@code Optional} with the matching pilot, or empty if none exists
     */
    Optional<Pilot> findBySystemUser(SystemUser systemUser);
}
