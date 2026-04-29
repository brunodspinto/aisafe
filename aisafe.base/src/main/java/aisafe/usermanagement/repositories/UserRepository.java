package aisafe.usermanagement.repositories;

import aisafe.usermanagement.domain.MecanographicNumber;
import aisafe.usermanagement.domain.User;
import eapli.framework.domain.repositories.DomainRepository;
import eapli.framework.infrastructure.authz.domain.model.Username;
import java.util.Optional;

public interface UserRepository extends DomainRepository<MecanographicNumber, User> {

    Iterable<User> findAllActive();

    Optional<User> findByUsername(Username username);
}
