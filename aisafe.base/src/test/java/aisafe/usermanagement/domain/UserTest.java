package aisafe.usermanagement.domain;

import eapli.framework.infrastructure.authz.domain.model.NilPasswordPolicy;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    private static final SecurityClearance DUMMY_CLEARANCE =
            new SecurityClearance("SECRET", LocalDate.now().plusYears(1));

    static SystemUser dummySystemUser(final String username, final Role... roles) {
        return new SystemUserBuilder(new NilPasswordPolicy(), new PlainTextEncoder())
                .with(username, "duMMy1", "Dummy", "User", "dummy@aisafe.com")
                .withRoles(roles)
                .build();
    }

    private UserBuilder baseBuilder() {
        return new UserBuilder()
                .withPhoneNumber("912345678")
                .withEmail(new Email("test@aisafe.com"))
                .withPosition("Operator")
                .withSecurityClearance(DUMMY_CLEARANCE)
                .withSkillsAssessmentDate(LocalDate.now());
    }

    // --- User identity ---

    @Test
    void ensureUsersWithSameMecanographicNumberAreEqual() {
        final User a = baseBuilder().withMecanographicNumber("DUMMY")
                .withSystemUser(dummySystemUser("user1", AiSafeRoles.ADMIN)).build();
        final User b = baseBuilder().withMecanographicNumber("DUMMY")
                .withSystemUser(dummySystemUser("user2", AiSafeRoles.ADMIN)).build();

        assertEquals(a, b);
    }

    @Test
    void ensureUsersWithDifferentMecanographicNumbersAreNotEqual() {
        final User a = baseBuilder().withMecanographicNumber("AAA")
                .withSystemUser(dummySystemUser("user1", AiSafeRoles.ADMIN)).build();
        final User b = baseBuilder().withMecanographicNumber("BBB")
                .withSystemUser(dummySystemUser("user2", AiSafeRoles.ADMIN)).build();

        assertNotEquals(a, b);
    }

    @Test
    void ensureUserEqualsItself() {
        final User a = baseBuilder().withMecanographicNumber("DUMMY")
                .withSystemUser(dummySystemUser("user1", AiSafeRoles.ADMIN)).build();

        assertEquals(a, a);
        assertTrue(a.sameAs(a));
    }

    @Test
    void ensureUserDoesNotEqualDifferentType() {
        final SystemUser systemUser = dummySystemUser("user1", AiSafeRoles.ADMIN);
        final User a = baseBuilder().withMecanographicNumber("DUMMY")
                .withSystemUser(systemUser).build();

        assertFalse(a.equals(systemUser));
    }

    @Test
    void ensureUsersWithDifferentMecanographicNumbersAreNotTheSame() {
        final User a = baseBuilder().withMecanographicNumber("AAA")
                .withSystemUser(dummySystemUser("user1", AiSafeRoles.ADMIN)).build();
        final User b = baseBuilder().withMecanographicNumber("BBB")
                .withSystemUser(dummySystemUser("user2", AiSafeRoles.ADMIN)).build();

        assertFalse(a.sameAs(b));
    }

    @Test
    void ensureUserConstructorRejectsNullSystemUser() {
        assertThrows(IllegalArgumentException.class, () ->
                new User(null, MecanographicNumber.valueOf("123"),
                        "910000000", new Email("a@b.com"), "Pilot",
                        DUMMY_CLEARANCE, LocalDate.now()));
    }

    @Test
    void ensureUserConstructorRejectsNullMecanographicNumber() {
        assertThrows(IllegalArgumentException.class, () ->
                new User(dummySystemUser("u", AiSafeRoles.ADMIN), null,
                        "910000000", new Email("a@b.com"), "Pilot",
                        DUMMY_CLEARANCE, LocalDate.now()));
    }

    // --- Email (AC031.2) ---

    @Test
    void ensureEmailAcceptsValidFormat() {
        final Email email = new Email("valid@aisafe.com");
        assertEquals("valid@aisafe.com", email.address());
    }

    @Test
    void ensureEmailRejectsInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> new Email("not-an-email"));
    }

    @Test
    void ensureEmailRejectsMissingAtSign() {
        assertThrows(IllegalArgumentException.class, () -> new Email("useraisafe.com"));
    }

    @Test
    void ensureEmailRejectsMissingDomain() {
        assertThrows(IllegalArgumentException.class, () -> new Email("user@"));
    }

    @Test
    void ensureEmailRejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> new Email("  "));
    }

    @Test
    void ensureEmailIsSavedLowercase() {
        final Email email = new Email("User@AiSafe.COM");
        assertEquals("user@aisafe.com", email.address());
    }

    // --- SecurityClearance ---

    @Test
    void ensureSecurityClearanceIsActiveWhenDateIsInFuture() {
        final SecurityClearance clearance =
                new SecurityClearance("TOP_SECRET", LocalDate.now().plusDays(30));
        assertTrue(clearance.isActive());
    }

    @Test
    void ensureSecurityClearanceRejectsPastExpirationDate() {
        assertThrows(IllegalArgumentException.class, () ->
                new SecurityClearance("SECRET", LocalDate.now().minusDays(1)));
    }

    @Test
    void ensureSecurityClearanceRejectsNullLevel() {
        assertThrows(IllegalArgumentException.class, () ->
                new SecurityClearance(null, LocalDate.now().plusYears(1)));
    }

    @Test
    void ensureSecurityClearanceRejectsBlankLevel() {
        assertThrows(IllegalArgumentException.class, () ->
                new SecurityClearance("   ", LocalDate.now().plusYears(1)));
    }

    // --- MecanographicNumber ---

    @Test
    void ensureMecanographicNumberRejectsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                new MecanographicNumber(null));
    }

    @Test
    void ensureMecanographicNumberRejectsBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                new MecanographicNumber("  "));
    }

    @Test
    void ensureMecanographicNumberValueOfEqualsDirectConstructor() {
        final MecanographicNumber a = MecanographicNumber.valueOf("12345");
        final MecanographicNumber b = new MecanographicNumber("12345");
        assertEquals(a, b);
    }
}
