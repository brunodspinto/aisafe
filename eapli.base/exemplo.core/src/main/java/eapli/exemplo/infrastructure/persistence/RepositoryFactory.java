package eapli.exemplo.infrastructure.persistence;

import eapli.exemplo.userbackoffice.repositories.AiSafeUserRepository;
import eapli.exemplo.userbackoffice.repositories.SignupRequestRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.authz.domain.repositories.UserRepository;

public interface RepositoryFactory {

    TransactionalContext newTransactionalContext();

    UserRepository users(TransactionalContext autoTx);

    UserRepository users();

    AiSafeUserRepository aiSafeUsers(TransactionalContext autoTx);

    AiSafeUserRepository aiSafeUsers();

    AiSafeUserRepository utentes(TransactionalContext autoTx);

    AiSafeUserRepository utentes();

    SignupRequestRepository signupRequests(TransactionalContext autoTx);

    SignupRequestRepository signupRequests();
}
