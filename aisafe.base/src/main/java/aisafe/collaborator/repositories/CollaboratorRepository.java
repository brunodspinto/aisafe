package aisafe.collaborator.repositories;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.collaborator.domain.Collaborator;
import eapli.framework.domain.repositories.DomainRepository;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;

import java.util.Optional;

/**
 * Repository interface for {@link Collaborator} aggregate roots, keyed by auto-generated {@code Long} id.
 */
public interface CollaboratorRepository extends DomainRepository<Long, Collaborator> {

    /**
     * Returns all active collaborators belonging to the given air transport company.
     *
     * @param company the air transport company to filter by
     * @return active collaborators for that company
     */
    Iterable<Collaborator> findActiveByAirTransportCompany(AirTransportCompany company);

    /**
     * Returns all active collaborators belonging to the given air control area.
     *
     * @param area the air control area to filter by
     * @return active collaborators for that area
     */
    Iterable<Collaborator> findActiveByAirControlArea(AirControlArea area);

    /**
     * Finds the collaborator associated with the given system user.
     *
     * @param systemUser the authenticated system user
     * @return an {@code Optional} with the matching collaborator, or empty if not found
     */
    Optional<Collaborator> findBySystemUser(SystemUser systemUser);
}
