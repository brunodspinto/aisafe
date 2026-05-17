package aisafe.infrastructure.persistence.jpa;

import aisafe.weatherdata.domain.WeatherData;
import aisafe.weatherdata.repositories.WeatherDataRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

public class JpaWeatherDataRepository
        extends JpaAutoTxRepository<WeatherData, Long, Long>
        implements WeatherDataRepository {

    public JpaWeatherDataRepository(final String puName) {
        super(puName, "id");
    }

    public JpaWeatherDataRepository(final TransactionalContext tx) {
        super(tx, "id");
    }
}
