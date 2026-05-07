package aisafe.airport.repositories;

import aisafe.airport.domain.Airport;
import aisafe.airport.domain.AirportIATACode;
import aisafe.airport.domain.AirportICAOCode;
import eapli.framework.domain.repositories.DomainRepository;

import java.util.Optional;

public interface AirportRepository extends DomainRepository<AirportIATACode, Airport> {

    Optional<Airport> findByIcaoCode(AirportICAOCode icaoCode);
}