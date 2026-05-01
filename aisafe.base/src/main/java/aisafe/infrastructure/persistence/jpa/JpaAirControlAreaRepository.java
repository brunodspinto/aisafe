package aisafe.infrastructure.persistence.jpa;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

public class JpaAirControlAreaRepository
        extends JpaAutoTxRepository<AirControlArea, String, String>
        implements AirControlAreaRepository {

    public JpaAirControlAreaRepository(final String puName) {
        super(puName, "areaCode");
    }
}
