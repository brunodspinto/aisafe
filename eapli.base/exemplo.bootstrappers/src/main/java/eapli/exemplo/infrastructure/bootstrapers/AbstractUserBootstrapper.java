package eapli.exemplo.infrastructure.bootstrapers;

import java.time.LocalDate;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eapli.exemplo.usermanagement.application.AddUserController;
import eapli.exemplo.usermanagement.application.ListUsersController;
import eapli.exemplo.userbackoffice.domain.Email;
import eapli.exemplo.userbackoffice.domain.SecurityClearance;
import eapli.exemplo.userbackoffice.domain.User;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.authz.domain.model.Username;

public class AbstractUserBootstrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractUserBootstrapper.class);

    final AddUserController userController = new AddUserController();
    final ListUsersController listUserController = new ListUsersController();

    public AbstractUserBootstrapper() {
        super();
    }

    protected SystemUser registerUser(final String username, final String password,
            final String firstName, final String lastName,
            final String email, final Set<Role> roles) {

        final SecurityClearance defaultClearance =
                new SecurityClearance("PUBLIC", LocalDate.now().plusYears(5));
        final Email emailVO = new Email(email);

        SystemUser u = null;
        try {
            final User utente = userController.addUser(username, password, firstName, lastName,
                    email, roles, "000000000", "Staff", emailVO, defaultClearance, LocalDate.now());
            u = utente.user();
            LOGGER.debug("»»» %s", username);
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            u = listUserController.find(Username.valueOf(username)).orElseThrow(() -> e);
        }
        return u;
    }
}
