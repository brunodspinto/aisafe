package aisafe.infrastructure.persistence.inmemory;

import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.airtransportcompany.domain.IATACode;
import aisafe.airtransportcompany.domain.ICAOCode;
import aisafe.airtransportcompany.repositories.AirTransportCompanyRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import java.util.Optional;

public class InMemoryAirTransportCompanyRepository
        extends InMemoryDomainRepository<AirTransportCompany, IATACode>
        implements AirTransportCompanyRepository {

    @Override
    public Optional<AirTransportCompany> findByName(final String name) {
        for (final AirTransportCompany c : findAll()) {
            if (c.name().equalsIgnoreCase(name)) return Optional.of(c);
        }
        return Optional.empty();
    }

    @Override
    public Optional<AirTransportCompany> findByIcaoCode(final ICAOCode code) {
        for (final AirTransportCompany c : findAll()) {
            if (c.icaoCode().equals(code)) return Optional.of(c);
        }
        return Optional.empty();
    }
}
