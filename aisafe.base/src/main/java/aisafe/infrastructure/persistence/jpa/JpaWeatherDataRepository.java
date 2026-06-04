package aisafe.infrastructure.persistence.jpa;

import aisafe.aircontrolarea.domain.AirControlAreaCode;
import aisafe.weatherdata.domain.WeatherData;
import aisafe.weatherdata.repositories.WeatherDataRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class JpaWeatherDataRepository
        extends JpaAutoTxRepository<WeatherData, Long, Long>
        implements WeatherDataRepository {

    public JpaWeatherDataRepository(final String puName) {
        super(puName, "id");
    }

    public JpaWeatherDataRepository(final TransactionalContext tx) {
        super(tx, "id");
    }

    @Override
    public Iterable<WeatherData> findByDateAndAirControlArea(final LocalDate date,
                                                             final AirControlAreaCode areaCode) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null.");
        }
        if (areaCode == null) {
            throw new IllegalArgumentException("Air Control Area code cannot be null.");
        }

        final Map<String, Object> params = new HashMap<>();
        params.put("areaCode", areaCode.toString());
        params.put("start", date.atStartOfDay());
        params.put("end", date.plusDays(1).atStartOfDay());

        return match("e.areaCode.value = :areaCode AND e.date >= :start AND e.date < :end", params);
    }
}
