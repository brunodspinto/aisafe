package eapli.exemplo.userbackoffice.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;

import org.junit.Test;

import eapli.exemplo.usermanagement.domain.AiSafeRoles;
import eapli.framework.infrastructure.authz.domain.model.NilPasswordPolicy;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;

public class UserTest {

    private final String aMecanographicNumber = "abc";
    private final String anotherMecanographicNumber = "xyz";

    private static final SecurityClearance DUMMY_CLEARANCE =
            new SecurityClearance("SECRET", LocalDate.now().plusYears(1));

    public static SystemUser dummyUser(final String username, final Role... roles) {
        final SystemUserBuilder userBuilder =
                new SystemUserBuilder(new NilPasswordPolicy(), new PlainTextEncoder());
        return userBuilder.with(username, "duMMy1", "dummy", "dummy", "a@b.ro")
                .withRoles(roles).build();
    }

    private SystemUser getNewDummyUser() {
        return dummyUser("dummy", AiSafeRoles.ADMIN);
    }

    private UserBuilder baseBuilder() {
        return new UserBuilder()
                .withPhoneNumber("910000000")
                .withEmail(new Email("test@aisafe.com"))
                .withPosition("Operator")
                .withSecurityClearance(DUMMY_CLEARANCE)
                .withSkillsAssessmentDate(LocalDate.now());
    }

    @Test
    public void ensureUserEqualsPassesForTheSameMecanographicNumber() {
        final User a = baseBuilder().withMecanographicNumber("DUMMY")
                .withSystemUser(getNewDummyUser()).build();
        final User b = baseBuilder().withMecanographicNumber("DUMMY")
                .withSystemUser(getNewDummyUser()).build();
        assertTrue(a.equals(b));
    }

    @Test
    public void ensureUserEqualsFailsForDifferentMecanographicNumber() {
        final User a = baseBuilder().withMecanographicNumber(aMecanographicNumber)
                .withSystemUser(getNewDummyUser()).build();
        final User b = baseBuilder().withMecanographicNumber(anotherMecanographicNumber)
                .withSystemUser(getNewDummyUser()).build();
        assertFalse(a.equals(b));
    }

    @Test
    public void ensureUserEqualsAreTheSameForTheSameInstance() {
        final User a = new User();
        assertTrue(a.equals(a));
    }

    @Test
    public void ensureUserEqualsFailsForDifferentObjectTypes() {
        final User a = baseBuilder().withMecanographicNumber("DUMMY")
                .withSystemUser(getNewDummyUser()).build();
        assertFalse(a.equals(getNewDummyUser()));
    }

    @Test
    public void ensureUserIsTheSameAsItsInstance() {
        final User a = baseBuilder().withMecanographicNumber("DUMMY")
                .withSystemUser(getNewDummyUser()).build();
        assertTrue(a.sameAs(a));
    }

    @Test
    public void ensureTwoUsersWithDifferentMecanographicNumbersAreNotTheSame() {
        final User a = baseBuilder().withMecanographicNumber(aMecanographicNumber)
                .withSystemUser(getNewDummyUser()).build();
        final User b = baseBuilder().withMecanographicNumber(anotherMecanographicNumber)
                .withSystemUser(getNewDummyUser()).build();
        assertFalse(a.sameAs(b));
    }
}
