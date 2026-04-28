package eapli.exemplo.persistence.impl.inmemory;

import eapli.exemplo.infrastructure.persistence.RepositoryFactory;
import eapli.exemplo.usermanagement.domain.AiSafeRoles;
import eapli.exemplo.usermanagement.domain.UserBuilderHelper;
import eapli.exemplo.userbackoffice.repositories.AiSafeUserRepository;
import eapli.exemplo.userbackoffice.repositories.SignupRequestRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.authz.domain.repositories.UserRepository;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;

public class InMemoryRepositoryFactory implements RepositoryFactory {

    @Override
    public UserRepository users(final TransactionalContext tx) {
        final var repo = new InMemoryUserRepository();
        final var userBuilder = UserBuilderHelper.builder();
        userBuilder.withUsername("poweruser").withPassword("Password1").withName("joe", "power")
                .withEmail("joe@email.org").withRoles(AiSafeRoles.ADMIN);
        final var newUser = userBuilder.build();
        repo.save(newUser);
        return repo;
    }

    @Override
    public UserRepository users() {
        return users(null);
    }

    @Override
    public AiSafeUserRepository aiSafeUsers(final TransactionalContext tx) {
        return new InMemoryAiSafeUserRepository();
    }

    @Override
    public AiSafeUserRepository aiSafeUsers() {
        return aiSafeUsers(null);
    }

    @Override
    public AiSafeUserRepository utentes(final TransactionalContext tx) {
        return new InMemoryAiSafeUserRepository();
    }

    @Override
    public AiSafeUserRepository utentes() {
        return utentes(null);
    }

    @Override
    public SignupRequestRepository signupRequests() {
        return signupRequests(null);
    }

    @Override
    public SignupRequestRepository signupRequests(final TransactionalContext tx) {
        return new InMemorySignupRequestRepository();
    }

    @Override
    public TransactionalContext newTransactionalContext() {
        return null;
    }
}
