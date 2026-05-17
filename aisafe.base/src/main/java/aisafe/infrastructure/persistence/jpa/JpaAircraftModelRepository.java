package aisafe.infrastructure.persistence.jpa;

import aisafe.aircraftmodel.domain.AircraftModel;
import aisafe.aircraftmodel.repositories.AircraftModelRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class JpaAircraftModelRepository
        extends JpaAutoTxRepository<AircraftModel, Long, Long>
        implements AircraftModelRepository {

    public JpaAircraftModelRepository(final String puName) {
        super(puName, "id");
    }

    public JpaAircraftModelRepository(final TransactionalContext tx) {
        super(tx, "id");
    }

    @Override
    public Optional<AircraftModel> findByModelNameAndMaker(final String modelName, final String makerName) {
        final Map<String, Object> params = new HashMap<>();
        params.put("modelName", modelName);
        params.put("makerName", makerName);
        return matchOne("LOWER(e.modelName) = LOWER(:modelName) AND e.makerName.value = :makerName", params);
    }
}
