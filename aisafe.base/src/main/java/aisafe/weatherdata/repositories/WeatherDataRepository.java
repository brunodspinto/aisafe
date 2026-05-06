package aisafe.weatherdata.repositories;

import aisafe.weatherdata.domain.WeatherData;
import eapli.framework.domain.repositories.DomainRepository;

/**
 * Repository for the WeatherData aggregate.
 * The aggregate identity is a Long (auto-generated surrogate key).
 */
public interface WeatherDataRepository extends DomainRepository<Long, WeatherData> {
}
