package aisafe.collaborator.repositories;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.collaborator.domain.Collaborator;
import eapli.framework.domain.repositories.DomainRepository;

public interface CollaboratorRepository extends DomainRepository<Long, Collaborator> {

    Iterable<Collaborator> findActiveByAirTransportCompany(AirTransportCompany company);

    Iterable<Collaborator> findActiveByAirControlArea(AirControlArea area);
}
