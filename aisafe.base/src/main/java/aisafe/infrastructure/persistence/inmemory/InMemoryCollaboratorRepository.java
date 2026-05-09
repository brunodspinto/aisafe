package aisafe.infrastructure.persistence.inmemory;

import aisafe.collaborator.domain.Collaborator;
import aisafe.collaborator.repositories.CollaboratorRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

import java.lang.reflect.Field;
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
}
