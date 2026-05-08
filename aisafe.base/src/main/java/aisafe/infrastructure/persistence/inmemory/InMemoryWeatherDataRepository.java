package aisafe.infrastructure.persistence.inmemory;

import aisafe.weatherdata.domain.WeatherData;
import aisafe.weatherdata.repositories.WeatherDataRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainAutoNumberRepository;

public class InMemoryWeatherDataRepository
        extends InMemoryDomainAutoNumberRepository<WeatherData>
        implements WeatherDataRepository {
}
