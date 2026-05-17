package aisafe.aircontrolarea.repositories;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.aircontrolarea.domain.AirControlAreaCode;
import eapli.framework.domain.repositories.DomainRepository;

/**
 * Repository for the AirControlArea entity.
 * The entity ID is an {@link AirControlAreaCode}.
 */
public interface AirControlAreaRepository extends DomainRepository<AirControlAreaCode, AirControlArea> {
}