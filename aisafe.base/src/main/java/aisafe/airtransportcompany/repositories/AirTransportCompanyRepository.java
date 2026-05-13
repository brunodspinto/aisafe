package aisafe.airtransportcompany.repositories;

import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.airtransportcompany.domain.IATACode;
import aisafe.airtransportcompany.domain.ICAOCode;
import eapli.framework.domain.repositories.DomainRepository;
import java.util.Optional;

/**
 * Repository interface for {@link AirTransportCompany} aggregate roots, keyed by {@link IATACode}.
 */
public interface AirTransportCompanyRepository extends DomainRepository<IATACode, AirTransportCompany> {

    /**
     * Looks up a company by its unique name.
     *
     * @param name the company name to search for
     * @return an {@code Optional} with the matching company, or empty if not found
     */
    Optional<AirTransportCompany> findByName(String name);

    /**
     * Looks up a company by its ICAO code.
     *
     * @param code the ICAO code to search for
     * @return an {@code Optional} with the matching company, or empty if not found
     */
    Optional<AirTransportCompany> findByIcaoCode(ICAOCode code);
}
