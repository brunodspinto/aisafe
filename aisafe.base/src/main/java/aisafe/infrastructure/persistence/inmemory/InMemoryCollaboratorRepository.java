package aisafe.infrastructure.persistence.inmemory;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.collaborator.domain.Collaborator;
import aisafe.collaborator.repositories.CollaboratorRepository;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryCollaboratorRepository
        extends InMemoryDomainRepository<Collaborator, Long>
        implements CollaboratorRepository {

    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public <S extends Collaborator> S save(final S entity) {
        if (entity.identity() == null) {
            try {
                final Field idField = Collaborator.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(entity, idGenerator.getAndIncrement());
            } catch (final Exception e) {
                throw new RuntimeException("Failed to set collaborator id", e);
            }
        }
        return super.save(entity);
    }

    @Override
    public Iterable<Collaborator> findByAirTransportCompany(final AirTransportCompany company) {
        final List<Collaborator> result = new ArrayList<>();
        for (final Collaborator c : findAll()) {
            if (c.isCompanyCollaborator() && c.airTransportCompany().equals(company))
                result.add(c);
        }
        return result;
    }

    @Override
    public Iterable<Collaborator> findByAirControlArea(final AirControlArea area) {
        final List<Collaborator> result = new ArrayList<>();
        for (final Collaborator c : findAll()) {
            if (c.isAreaCollaborator() && c.airControlArea().equals(area))
                result.add(c);
        }
        return result;
    }

    @Override
    public Optional<Collaborator> findBySystemUser(final SystemUser systemUser) {
        for (final Collaborator c : findAll()) {
            if (c.user().systemUser().equals(systemUser)) return Optional.of(c);
        }
        return Optional.empty();
    }
}
