package aisafe.infrastructure.persistence.jpa;

import aisafe.usermanagement.domain.MecanographicNumber;
import aisafe.usermanagement.domain.User;
import aisafe.usermanagement.repositories.UserRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.authz.domain.model.Username;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class JpaUserRepository
        extends JpaAutoTxRepository<User, MecanographicNumber, MecanographicNumber>
        implements UserRepository {

    public JpaUserRepository(final String puName) {
        super(puName, "mecanographicNumber");
    }

    public JpaUserRepository(final TransactionalContext tx) {
        super(tx, "mecanographicNumber");
    }

    @Override
    public Optional<User> findByUsername(final Username username) {
        final Map<String, Object> params = new HashMap<>();
        params.put("username", username);
        return matchOne("e.systemUser.username=:username", params);
    }

    @Override
    public Iterable<User> findAllActive() {
        return match("e.systemUser.active = true");
    }

    @Override
    public Iterable<User> findAll() {
        return match("1=1");
    }
}
