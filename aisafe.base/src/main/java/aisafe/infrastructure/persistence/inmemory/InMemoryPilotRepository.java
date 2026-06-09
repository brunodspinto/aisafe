package aisafe.infrastructure.persistence.inmemory;

import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.pilot.domain.Pilot;
import aisafe.pilot.repositories.PilotRepository;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryPilotRepository
        extends InMemoryDomainRepository<Pilot, Long>
        implements PilotRepository {

    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public <S extends Pilot> S save(final S entity) {
        if (entity.identity() == null) {
            try {
                final Field idField = Pilot.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(entity, idGenerator.getAndIncrement());
            } catch (final Exception e) {
                throw new RuntimeException("Failed to set pilot id", e);
            }
        }
        return super.save(entity);
    }

    @Override
    public Iterable<Pilot> findByAirTransportCompany(final AirTransportCompany company) {
        final List<Pilot> result = new ArrayList<>();
        for (final Pilot p : findAll()) {
            if (p.companyIataCode() != null && p.companyIataCode().equals(company.identity()))
                result.add(p);
        }
        return result;
    }

    @Override
    public Optional<Pilot> findBySystemUser(final SystemUser systemUser) {
        for (final Pilot p : findAll()) {
            if (p.user().systemUser().equals(systemUser)) return Optional.of(p);
        }
        return Optional.empty();
    }
}
