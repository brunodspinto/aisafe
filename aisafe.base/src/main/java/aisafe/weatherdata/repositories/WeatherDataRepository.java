package aisafe.weatherdata.repositories;

import aisafe.aircontrolarea.domain.AirControlAreaCode;
import aisafe.weatherdata.domain.WeatherData;
import eapli.framework.domain.repositories.DomainRepository;
import java.time.LocalDate;

/**
 * Repository for the WeatherData aggregate.
 * The aggregate identity is a Long (auto-generated surrogate key).
 */
public interface WeatherDataRepository extends DomainRepository<Long, WeatherData> {

    Iterable<WeatherData> findByDateAndAirControlArea(LocalDate date, AirControlAreaCode areaCode);
}
