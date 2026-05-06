package aisafe.infrastructure.persistence.inmemory;

import aisafe.weatherdata.domain.WeatherData;
import aisafe.weatherdata.repositories.WeatherDataRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

public class InMemoryWeatherDataRepository
        extends InMemoryDomainRepository<WeatherData, Long>
        implements WeatherDataRepository {
}
