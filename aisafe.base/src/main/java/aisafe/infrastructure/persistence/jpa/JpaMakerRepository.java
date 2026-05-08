package aisafe.infrastructure.persistence.jpa;

import aisafe.maker.domain.Maker;
import aisafe.maker.repositories.MakerRepository;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

public class JpaMakerRepository
        extends JpaAutoTxRepository<Maker, String, String>
        implements MakerRepository {

    public JpaMakerRepository(final String puName) {
        super(puName, "name");
    }
}
