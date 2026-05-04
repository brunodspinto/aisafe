package aisafe.airtransportcompany.repositories;

import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.airtransportcompany.domain.IATACode;
import aisafe.airtransportcompany.domain.ICAOCode;
import eapli.framework.domain.repositories.DomainRepository;
import java.util.Optional;

public interface AirTransportCompanyRepository extends DomainRepository<IATACode, AirTransportCompany> {

    Optional<AirTransportCompany> findByName(String name);

    Optional<AirTransportCompany> findByIcaoCode(ICAOCode code);
}
