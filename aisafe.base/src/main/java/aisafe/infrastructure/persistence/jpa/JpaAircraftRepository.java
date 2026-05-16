package aisafe.infrastructure.persistence.jpa;

import aisafe.aircraft.domain.Aircraft;
import aisafe.aircraft.domain.RegistrationNumber;
import aisafe.aircraft.repositories.AircraftRepository;
import aisafe.aircraftmodel.domain.AircraftModel;
import aisafe.enginemodel.domain.EngineModel;
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

    @Override
    public boolean existsAircraftUsingModelEngine(final AircraftModel model, final EngineModel engine) {
        if (model == null || engine == null) return false;
        final Long count = entityManager().createQuery(
                        "SELECT COUNT(a) FROM Aircraft a "
                                + "JOIN a.aircraftModel m "
                                + "JOIN m.certifiedEngines e "
                                + "WHERE m = :model AND e = :engine", Long.class)
                .setParameter("model", model)
                .setParameter("engine", engine)
                .getSingleResult();
        return count != null && count > 0L;
    }
}
