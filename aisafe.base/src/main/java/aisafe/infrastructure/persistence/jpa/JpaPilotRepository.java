package aisafe.infrastructure.persistence.jpa;

import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.pilot.domain.Pilot;
import aisafe.pilot.repositories.PilotRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class JpaPilotRepository
        extends JpaAutoTxRepository<Pilot, Long, Long>
        implements PilotRepository {

    public JpaPilotRepository(final String puName) {
        super(puName, "id");
    }

    public JpaPilotRepository(final TransactionalContext tx) {
        super(tx, "id");
    }

    @Override
    public Iterable<Pilot> findByAirTransportCompany(final AirTransportCompany company) {
        final Map<String, Object> params = new HashMap<>();
        params.put("iata", company.identity());
        return match("e.companyIataCode = :iata", params);
    }

    @Override
    public Optional<Pilot> findBySystemUser(final SystemUser systemUser) {
        final Map<String, Object> params = new HashMap<>();
        params.put("su", systemUser);
        return matchOne("e.user.systemUser = :su", params);
    }
}
