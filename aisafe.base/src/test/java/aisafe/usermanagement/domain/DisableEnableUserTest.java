package aisafe.usermanagement.domain;

import eapli.framework.infrastructure.authz.domain.model.NilPasswordPolicy;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisableEnableUserTest {

    private static final SecurityClearance DUMMY_CLEARANCE =
            new SecurityClearance(SecurityLevel.LOW, LocalDate.now().plusYears(1));

    private SystemUser dummySystemUser(final String username) {
        return new SystemUserBuilder(new NilPasswordPolicy(), new PlainTextEncoder())
                .with(username, "duMMy1", "Dummy", "User", "dummy@aisafe.com")
                .withRoles(AiSafeRoles.ADMIN)
                .build();
    }

    private User buildUser(final SystemUser sys, final String mecNumber) {
        return new UserBuilder()
                .withSystemUser(sys)
                .withMecanographicNumber(mecNumber)
                .withPhoneNumber("912345678")
                .withEmail(new Email("test@aisafe.com"))
                .withPosition("Operator")
                .withSecurityClearance(DUMMY_CLEARANCE)
                .withSkillsAssessmentDate(LocalDate.now())
                .build();
    }

    // AC032.1 — disable an active user

    @Test
    void ensureNewUserIsActiveByDefault() {
        final SystemUser sys = dummySystemUser("user1");
        assertTrue(sys.isActive());
    }

    @Test
    void ensureActiveUserCanBeDeactivated() {
        final SystemUser sys = dummySystemUser("user2");
        sys.deactivate(Calendar.getInstance());
        assertFalse(sys.isActive());
    }

    // AC032.2 — re-enable a disabled user

    @Test
    void ensureInactiveUserCanBeReactivated() {
        final SystemUser sys = dummySystemUser("user3");
        sys.deactivate(Calendar.getInstance());
        sys.activate();
        assertTrue(sys.isActive());
    }

    @Test
    void ensureReactivatedUserIsFullyActive() {
        final SystemUser sys = dummySystemUser("user4");
        sys.deactivate(Calendar.getInstance());
        sys.activate();
        // second deactivation must be possible (user is truly active again)
        sys.deactivate(Calendar.getInstance());
        assertFalse(sys.isActive());
    }

    // AC032.5 — graceful handling of edge cases

    @Test
    void ensureDeactivatingAlreadyInactiveUserThrows() {
        final SystemUser sys = dummySystemUser("user5");
        sys.deactivate(Calendar.getInstance());
        assertThrows(IllegalStateException.class,
                () -> sys.deactivate(Calendar.getInstance()));
    }

    @Test
    void ensureActivatingAlreadyActiveUserIsIdempotent() {
        final SystemUser sys = dummySystemUser("user6");
        assertTrue(sys.isActive());
        sys.activate(); // should not throw
        assertTrue(sys.isActive());
    }

    // AC032.3 — User aggregate reflects SystemUser active state

    @Test
    void ensureUserAggregateReflectsDeactivatedState() {
        final SystemUser sys = dummySystemUser("user7");
        final User user = buildUser(sys, "MECNUM1");
        assertTrue(user.systemUser().isActive());
        sys.deactivate(Calendar.getInstance());
        assertFalse(user.systemUser().isActive());
    }

    @Test
    void ensureUserAggregateReflectsReactivatedState() {
        final SystemUser sys = dummySystemUser("user8");
        final User user = buildUser(sys, "MECNUM2");
        sys.deactivate(Calendar.getInstance());
        sys.activate();
        assertTrue(user.systemUser().isActive());
    }
}
