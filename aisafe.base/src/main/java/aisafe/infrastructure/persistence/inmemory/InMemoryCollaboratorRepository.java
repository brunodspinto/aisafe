package aisafe.infrastructure.persistence.inmemory;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.collaborator.domain.Collaborator;
import aisafe.collaborator.repositories.CollaboratorRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
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
    public Iterable<Collaborator> findActiveByAirTransportCompany(final AirTransportCompany company) {
        final List<Collaborator> result = new ArrayList<>();
        for (final Collaborator c : findAll()) {
            if (c.isCompanyCollaborator()
                    && c.airTransportCompany().equals(company)
                    && c.user().systemUser().isActive()) {
                result.add(c);
            }
        }
        return result;
    }

    @Override
    public Iterable<Collaborator> findActiveByAirControlArea(final AirControlArea area) {
        final List<Collaborator> result = new ArrayList<>();
        for (final Collaborator c : findAll()) {
            if (c.isAreaCollaborator()
                    && c.airControlArea().equals(area)
                    && c.user().systemUser().isActive()) {
                result.add(c);
            }
        }
        return result;
    }
}
