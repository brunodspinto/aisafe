package aisafe.infrastructure.persistence.jpa;

import aisafe.aircraftmodel.domain.AircraftModel;
import aisafe.aircraftmodel.repositories.AircraftModelRepository;
import aisafe.maker.domain.Maker;
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

    @Override
    public Optional<AircraftModel> findByModelNameAndMaker(final String modelName, final Maker maker) {
        final Map<String, Object> params = new HashMap<>();
        params.put("modelName", modelName);
        params.put("makerName", maker.name());
        return matchOne("LOWER(e.modelName) = LOWER(:modelName) AND e.maker.name = :makerName", params);
    }
}
