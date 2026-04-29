package aisafe.infrastructure.persistence.inmemory;

import aisafe.usermanagement.domain.MecanographicNumber;
import aisafe.usermanagement.domain.User;
import aisafe.usermanagement.repositories.UserRepository;
import eapli.framework.infrastructure.authz.domain.model.Username;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import java.util.ArrayList;
import java.util.Optional;

public class InMemoryAiSafeUserRepository
        extends InMemoryDomainRepository<User, MecanographicNumber>
        implements UserRepository {

    @Override
    public Optional<User> findByUsername(final Username username) {
        for (final User user : findAll()) {
            if (user.systemUser() != null && user.systemUser().username().equals(username)) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    @Override
    public Iterable<User> findAllActive() {
        final var active = new ArrayList<User>();
        for (final User user : findAll()) {
            if (user.systemUser() != null && user.systemUser().isActive()) {
                active.add(user);
            }
        }
        return active;
    }
}
