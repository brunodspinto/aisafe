package aisafe.usermanagement.repositories;

import aisafe.usermanagement.domain.MecanographicNumber;
import aisafe.usermanagement.domain.User;
import eapli.framework.domain.repositories.DomainRepository;
import eapli.framework.infrastructure.authz.domain.model.Username;
import java.util.Optional;

/**
 * Repository interface for {@link User} aggregates.
 */
public interface UserRepository extends DomainRepository<MecanographicNumber, User> {

    /** @return all users whose underlying system account is currently active */
    Iterable<User> findAllActive();

    /** @return all users regardless of account status */
    Iterable<User> findAll();

    /**
     * Finds a user by their system-user username.
     *
     * @param username the EAPLI {@link Username} to search by
     * @return an {@link Optional} containing the user, or empty if not found
     */
    Optional<User> findByUsername(Username username);
}
