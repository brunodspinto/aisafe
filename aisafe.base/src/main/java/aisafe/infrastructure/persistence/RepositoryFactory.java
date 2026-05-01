package aisafe.infrastructure.persistence;

import aisafe.usermanagement.repositories.UserRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import aisafe.aircontrolarea.repositories.AirControlAreaRepository;

public interface RepositoryFactory {

    TransactionalContext newTransactionalContext();

    eapli.framework.infrastructure.authz.domain.repositories.UserRepository systemUsers(TransactionalContext tx);

    eapli.framework.infrastructure.authz.domain.repositories.UserRepository systemUsers();

    UserRepository users(TransactionalContext tx);

    UserRepository users();

    // US 50
    AirControlAreaRepository airControlAreas(TransactionalContext tx);

    AirControlAreaRepository airControlAreas();
}
