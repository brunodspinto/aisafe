package aisafe.infrastructure.persistence.inmemory;

import aisafe.aircontrolarea.domain.AirControlAreaCode;
import aisafe.weatherdata.domain.WeatherData;
import aisafe.weatherdata.repositories.WeatherDataRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainAutoNumberRepository;
import java.time.LocalDate;

public class InMemoryWeatherDataRepository
        extends InMemoryDomainAutoNumberRepository<WeatherData>
        implements WeatherDataRepository {

    @Override
    public Iterable<WeatherData> findByDateAndAirControlArea(final LocalDate date,
                                                             final AirControlAreaCode areaCode) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null.");
        }
        if (areaCode == null) {
            throw new IllegalArgumentException("Air Control Area code cannot be null.");
        }
        return match(weatherData ->
                weatherData.date().toLocalDate().equals(date)
                        && weatherData.areaCode().equals(areaCode.toString()));
    }
}
