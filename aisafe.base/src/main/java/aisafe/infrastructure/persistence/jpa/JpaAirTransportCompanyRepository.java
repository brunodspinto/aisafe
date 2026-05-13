package aisafe.infrastructure.persistence.jpa;

import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.airtransportcompany.domain.IATACode;
import aisafe.airtransportcompany.domain.ICAOCode;
import aisafe.airtransportcompany.repositories.AirTransportCompanyRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class JpaAirTransportCompanyRepository
        extends JpaAutoTxRepository<AirTransportCompany, IATACode, IATACode>
        implements AirTransportCompanyRepository {

    public JpaAirTransportCompanyRepository(final String puName) {
        super(puName, "iataCode");
    }

    public JpaAirTransportCompanyRepository(final TransactionalContext tx) {
        super(tx, "iataCode");
    }

    @Override
    public Optional<AirTransportCompany> findByName(final String name) {
        final Map<String, Object> params = new HashMap<>();
        params.put("name", name);
        return matchOne("LOWER(e.name) = LOWER(:name)", params);
    }

    @Override
    public Optional<AirTransportCompany> findByIcaoCode(final ICAOCode code) {
        final Map<String, Object> params = new HashMap<>();
        params.put("code", code.toString());
        return matchOne("e.icaoCode.icaoCode = :code", params);
    }
}
