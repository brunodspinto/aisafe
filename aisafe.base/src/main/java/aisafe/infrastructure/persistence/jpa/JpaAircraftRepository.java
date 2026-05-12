package aisafe.infrastructure.persistence.jpa;

import aisafe.aircraft.domain.Aircraft;
import aisafe.aircraft.repositories.AircraftRepository;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

public class JpaAircraftRepository
        extends JpaAutoTxRepository<Aircraft, String, String>
        implements AircraftRepository {

    public JpaAircraftRepository(final String puName) {
        super(puName, "registrationNumber");
    }
}
