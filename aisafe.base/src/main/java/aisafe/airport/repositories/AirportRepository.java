package aisafe.airport.repositories;

import aisafe.airport.domain.Airport;
import aisafe.airport.domain.AirportIATACode;
import aisafe.airport.domain.AirportICAOCode;
import eapli.framework.domain.repositories.DomainRepository;

import java.util.Optional;

/**
 * Repository interface for {@link Airport} aggregate roots, keyed by {@link AirportIATACode}.
 */
public interface AirportRepository extends DomainRepository<AirportIATACode, Airport> {

    /**
     * Looks up an airport by its ICAO code.
     *
     * @param icaoCode the 4-letter ICAO code to search for
     * @return an {@code Optional} containing the airport, or empty if not found
     */
    Optional<Airport> findByIcaoCode(AirportICAOCode icaoCode);
}