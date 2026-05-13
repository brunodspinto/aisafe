package aisafe.infrastructure.persistence.jpa;

import aisafe.maker.domain.Maker;
import aisafe.maker.domain.MakerName;
import aisafe.maker.repositories.MakerRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

public class JpaMakerRepository
        extends JpaAutoTxRepository<Maker, MakerName, MakerName>
        implements MakerRepository {

    public JpaMakerRepository(final String puName) {
        super(puName, "name");
    }

    public JpaMakerRepository(final TransactionalContext tx) {
        super(tx, "name");
    }
}
