package aisafe.infrastructure.persistence.jpa;

import aisafe.infrastructure.persistence.RepositoryFactory;
import aisafe.usermanagement.repositories.UserRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.authz.repositories.impl.jpa.JpaAutoTxUserRepository;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;
import java.util.HashMap;

public class JpaRepositoryFactory implements RepositoryFactory {

    private static final String PERSISTENCE_UNIT = "aisafe";

    @Override
    public eapli.framework.infrastructure.authz.domain.repositories.UserRepository systemUsers(
            final TransactionalContext tx) {
        return new JpaAutoTxUserRepository(tx);
    }

    @Override
    public eapli.framework.infrastructure.authz.domain.repositories.UserRepository systemUsers() {
        return new JpaAutoTxUserRepository(PERSISTENCE_UNIT, new HashMap<>());
    }

    @Override
    public UserRepository users(final TransactionalContext tx) {
        return new JpaUserRepository(PERSISTENCE_UNIT);
    }

    @Override
    public UserRepository users() {
        return new JpaUserRepository(PERSISTENCE_UNIT);
    }

    @Override
    public TransactionalContext newTransactionalContext() {
        return JpaAutoTxRepository.buildTransactionalContext(PERSISTENCE_UNIT, new HashMap<>());
    }
}
