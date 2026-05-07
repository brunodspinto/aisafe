package aisafe.infrastructure.persistence.inmemory;

import aisafe.airport.domain.Airport;
import aisafe.airport.domain.AirportIATACode;
import aisafe.airport.domain.AirportICAOCode;
import aisafe.airport.repositories.AirportRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

import java.util.Optional;

public class InMemoryAirportRepository
        extends InMemoryDomainRepository<Airport, AirportIATACode>
        implements AirportRepository {

    @Override
    public Optional<Airport> findByIcaoCode(final AirportICAOCode icaoCode) {
        return matchOne(a -> a.icaoCode().equals(icaoCode));
    }
}