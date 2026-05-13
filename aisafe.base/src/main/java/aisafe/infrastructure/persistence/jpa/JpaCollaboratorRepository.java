package aisafe.infrastructure.persistence.jpa;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.collaborator.domain.Collaborator;
import aisafe.collaborator.repositories.CollaboratorRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class JpaCollaboratorRepository
        extends JpaAutoTxRepository<Collaborator, Long, Long>
        implements CollaboratorRepository {

    public JpaCollaboratorRepository(final String puName) {
        super(puName, "id");
    }

    public JpaCollaboratorRepository(final TransactionalContext tx) {
        super(tx, "id");
    }

    @Override
    public Iterable<Collaborator> findActiveByAirTransportCompany(final AirTransportCompany company) {
        final Map<String, Object> params = new HashMap<>();
        params.put("company", company);
        return match("e.airTransportCompany = :company AND e.user.systemUser.active = true", params);
    }

    @Override
    public Iterable<Collaborator> findActiveByAirControlArea(final AirControlArea area) {
        final Map<String, Object> params = new HashMap<>();
        params.put("area", area);
        return match("e.airControlArea = :area AND e.user.systemUser.active = true", params);
    }

    @Override
    public Optional<Collaborator> findBySystemUser(final SystemUser systemUser) {
        final Map<String, Object> params = new HashMap<>();
        params.put("su", systemUser);
        return matchOne("e.user.systemUser = :su", params);
    }
}
