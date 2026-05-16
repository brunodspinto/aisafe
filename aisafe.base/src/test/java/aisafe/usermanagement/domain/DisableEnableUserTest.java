package aisafe.usermanagement.domain;

import eapli.framework.infrastructure.authz.domain.model.NilPasswordPolicy;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.Role;
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
            new SecurityClearance(SecurityLevel.HIGH, LocalDate.now().plusYears(1));

    private static SystemUser dummySystemUser(final String username, final Role... roles) {
        return new SystemUserBuilder(new NilPasswordPolicy(), new PlainTextEncoder())
                .with(username, "duMMy1", "Dummy", "User", username + "@aisafe.com")
                .withRoles(roles)
                .build();
    }

    private static User buildUser(final SystemUser sys, final String mecNumber) {
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

    @Test
    void ensureNewSystemUserStartsActive() {
        final SystemUser sys = dummySystemUser("user0", AiSafeRoles.ADMIN);
        assertTrue(sys.isActive());
    }

    @Test
    void ensureActiveUserCanBeDeactivated() {
        final SystemUser sys = dummySystemUser("user2", AiSafeRoles.ADMIN);
        sys.deactivate(Calendar.getInstance());
        assertFalse(sys.isActive());
    }

    @Test
    void ensureInactiveUserCanBeReactivated() {
        final SystemUser sys = dummySystemUser("user3", AiSafeRoles.ADMIN);
        sys.deactivate(Calendar.getInstance());
        sys.activate();
        assertTrue(sys.isActive());
    }

    @Test
    void ensureReactivatedUserIsFullyActive() {
        final SystemUser sys = dummySystemUser("user4", AiSafeRoles.ADMIN);
        sys.deactivate(Calendar.getInstance());
        sys.activate();
        sys.deactivate(Calendar.getInstance());
        assertFalse(sys.isActive());
    }

    @Test
    void ensureDeactivatingAlreadyInactiveUserThrows() {
        final SystemUser sys = dummySystemUser("user5", AiSafeRoles.ADMIN);
        sys.deactivate(Calendar.getInstance());
        assertThrows(IllegalStateException.class,
                () -> sys.deactivate(Calendar.getInstance()));
    }

    @Test
    void ensureActivatingAlreadyActiveUserIsIdempotent() {
        final SystemUser sys = dummySystemUser("user6", AiSafeRoles.ADMIN);
        assertTrue(sys.isActive());
        sys.activate();
        assertTrue(sys.isActive());
    }

    @Test
    void ensureUserAggregateReflectsDeactivatedState() {
        final SystemUser sys = dummySystemUser("user7", AiSafeRoles.ADMIN);
        final User user = buildUser(sys, "MECNUM1");
        assertTrue(user.systemUser().isActive());
        sys.deactivate(Calendar.getInstance());
        assertFalse(user.systemUser().isActive());
    }

    @Test
    void ensureMultipleToggleCyclesPreserveState() {
        final SystemUser sys = dummySystemUser("user8", AiSafeRoles.ADMIN);
        for (int i = 0; i < 3; i++) {
            sys.deactivate(Calendar.getInstance());
            assertFalse(sys.isActive());
            sys.activate();
            assertTrue(sys.isActive());
        }
    }
}
