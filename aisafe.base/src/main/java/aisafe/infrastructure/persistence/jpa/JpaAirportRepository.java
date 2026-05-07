package aisafe.infrastructure.persistence.jpa;

import aisafe.airport.domain.Airport;
import aisafe.airport.domain.AirportIATACode;
import aisafe.airport.domain.AirportICAOCode;
import aisafe.airport.repositories.AirportRepository;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class JpaAirportRepository
        extends JpaAutoTxRepository<Airport, AirportIATACode, AirportIATACode>
        implements AirportRepository {

    public JpaAirportRepository(final String puName) {
        super(puName, "iataCode");
    }

    @Override
    public Optional<Airport> findByIcaoCode(final AirportICAOCode icaoCode) {
        final Map<String, Object> params = new HashMap<>();
        params.put("code", icaoCode.toString());
        return matchOne("e.icaoCode.icaoCode = :code", params);
    }
}
