package aisafe.infrastructure.persistence.jpa;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.aircontrolarea.domain.AirControlAreaCode;
import aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

public class JpaAirControlAreaRepository
        extends JpaAutoTxRepository<AirControlArea, AirControlAreaCode, AirControlAreaCode>
        implements AirControlAreaRepository {

    public JpaAirControlAreaRepository(final String puName) {
        super(puName, "areaCode");
    }

    public JpaAirControlAreaRepository(final TransactionalContext tx) {
        super(tx, "areaCode");
    }
}
