package aisafe.infrastructure.persistence.jpa;

import aisafe.enginemodel.domain.EngineModel;
import aisafe.enginemodel.repositories.EngineModelRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class JpaEngineModelRepository
        extends JpaAutoTxRepository<EngineModel, Long, Long>
        implements EngineModelRepository {

    public JpaEngineModelRepository(final String puName) {
        super(puName, "id");
    }

    public JpaEngineModelRepository(final TransactionalContext tx) {
        super(tx, "id");
    }

    @Override
    public Optional<EngineModel> findByNameAndMaker(final String name, final String makerName) {
        final Map<String, Object> params = new HashMap<>();
        params.put("name", name);
        params.put("makerName", makerName);
        return matchOne("e.name = :name AND e.makerName = :makerName", params);
    }
}
