package aisafe.aircontrolarea.repositories;

import aisafe.aircontrolarea.domain.AirControlArea;
import eapli.framework.domain.repositories.DomainRepository;

/**
 * Repository for the AirControlArea entity.
 * The entity ID is a String (areaCode).
 */
public interface AirControlAreaRepository extends DomainRepository<String, AirControlArea> {
}