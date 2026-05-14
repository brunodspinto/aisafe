package aisafe.infrastructure.persistence.jpa;

import aisafe.aircraft.domain.Aircraft;
import aisafe.aircraft.domain.RegistrationNumber;
import aisafe.aircraft.repositories.AircraftRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

public class JpaAircraftRepository
        extends JpaAutoTxRepository<Aircraft, RegistrationNumber, RegistrationNumber>
        implements AircraftRepository {

    public JpaAircraftRepository(final String puName) {
        super(puName, "registrationNumber");
    }

    public JpaAircraftRepository(final TransactionalContext tx) {
        super(tx, "registrationNumber");
    }
}
