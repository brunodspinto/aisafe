package aisafe.usermanagement.application;

import aisafe.auth.AuthenticationContext;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafePasswordPolicy;
import aisafe.usermanagement.domain.AiSafeRoles;
import aisafe.usermanagement.domain.Email;
import aisafe.usermanagement.domain.MecanographicNumber;
import aisafe.usermanagement.domain.SecurityClearance;
import aisafe.usermanagement.domain.SecurityLevel;
import aisafe.usermanagement.domain.User;
import aisafe.usermanagement.domain.UserBuilder;
import aisafe.usermanagement.repositories.UserRepository;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;
import eapli.framework.infrastructure.authz.domain.model.Username;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListUsersControllerTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private final ListUsersController controller = new ListUsersController();

    @BeforeAll
    static void configureAuthz() {
        // ensure a fresh in-memory repository for this test class
        PersistenceContext.reset();
        AuthzRegistry.configure(
            PersistenceContext.repositories().systemUsers(),
            new AiSafePasswordPolicy(),
            new PlainTextEncoder());
    }

    @AfterEach
    void tearDown() {
        AuthenticationContext.clear();
    }

    @Test
    void ensureAdminCanListAllUsersIncludingInactiveUsers() {
        AuthenticationContext.authenticate("admin", "Password1");

        final User activeUser = saveUser("listuser-active", true);
        final User inactiveUser = saveUser("listuser-inactive", false);

        final List<User> users = toList(controller.allUsers());

        assertEquals(2, users.size());
        assertTrue(users.stream().anyMatch(user -> user.identity().equals(activeUser.identity())
                && user.systemUser().isActive()));
        assertTrue(users.stream().anyMatch(user -> user.identity().equals(inactiveUser.identity())
                && !user.systemUser().isActive()));
    }

    @Test
    void ensureNonAdminCannotListUsers() {
        seedSystemUser("listuser-op", AiSafeRoles.BACKOFFICE_OPERATOR);
        AuthenticationContext.authenticate("listuser-op", "Password1");

        assertThrows(RuntimeException.class, controller::allUsers);
    }

    private User saveUser(final String username, final boolean active) {
        final SystemUser systemUser = seedSystemUser(username, AiSafeRoles.BACKOFFICE_OPERATOR);
        if (!active) {
            systemUser.deactivate(Calendar.getInstance());
        }

        final User user = new UserBuilder()
                .withSystemUser(systemUser)
                .withMecanographicNumber(MecanographicNumber.valueOf(nextMecanographicNumber()))
                .withPhoneNumber("912345678")
                .withEmail(new Email(username + "@aisafe.com"))
                .withPosition("Operator")
                .withSecurityClearance(new SecurityClearance(SecurityLevel.HIGH, LocalDate.now().plusYears(1)))
                .withSkillsAssessmentDate(LocalDate.now())
                .build();

        userRepo().save(user);
        return user;
    }

    private static SystemUser seedSystemUser(final String username, final eapli.framework.infrastructure.authz.domain.model.Role... roles) {
        final SystemUserBuilder builder = new SystemUserBuilder(new AiSafePasswordPolicy(), new PlainTextEncoder());
        builder.with(username, "Password1", "First", "Last", username + "@aisafe.com")
                .withRoles(roles);
        final SystemUser user = builder.build();
        PersistenceContext.repositories().systemUsers().save(user);
        return user;
    }

    private static List<User> toList(final Iterable<User> users) {
        final List<User> result = new ArrayList<>();
        for (final User user : users) {
            result.add(user);
        }
        return result;
    }

    private static String nextMecanographicNumber() {
        return String.format("LUS%06d", SEQUENCE.incrementAndGet());
    }

    private static UserRepository userRepo() {
        return PersistenceContext.repositories().users();
    }
}