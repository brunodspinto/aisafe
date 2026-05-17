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

/**
 * Unit tests for the {@link User} aggregate root.
 * Verifies construction, security clearance assignment, and identity.
 */
class UserTest {

    private static final SecurityClearance DUMMY_CLEARANCE =
            new SecurityClearance(SecurityLevel.HIGH, LocalDate.now().plusYears(1));

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


    @Test
    void ensureSecurityClearanceIsActiveWhenDateIsInFuture() {
        final SecurityClearance clearance =
                new SecurityClearance(SecurityLevel.CRITICAL, LocalDate.now().plusDays(30));
        assertTrue(clearance.isActive());
    }

    @Test
    void ensureSecurityClearanceRejectsPastExpirationDate() {
        assertThrows(IllegalArgumentException.class, () ->
                new SecurityClearance(SecurityLevel.HIGH, LocalDate.now().minusDays(1)));
    }

    @Test
    void ensureSecurityClearanceRejectsNullLevel() {
        assertThrows(IllegalArgumentException.class, () ->
                new SecurityClearance(null, LocalDate.now().plusYears(1)));
    }

    @Test
    void ensureElevatedAndAboveRequireBodyScan() {
        assertTrue(SecurityLevel.ELEVATED.requiresBodyScan());
        assertTrue(SecurityLevel.HIGH.requiresBodyScan());
        assertTrue(SecurityLevel.CRITICAL.requiresBodyScan());
    }

    @Test
    void ensureLowAndGuardedDoNotRequireBodyScan() {
        assertFalse(SecurityLevel.LOW.requiresBodyScan());
        assertFalse(SecurityLevel.GUARDED.requiresBodyScan());
    }

    @Test
    void ensureIsAtLeastRespectsOrder() {
        assertTrue(SecurityLevel.HIGH.isAtLeast(SecurityLevel.LOW));
        assertTrue(SecurityLevel.HIGH.isAtLeast(SecurityLevel.HIGH));
        assertFalse(SecurityLevel.LOW.isAtLeast(SecurityLevel.HIGH));
    }

    @Test
    void ensureSecurityLevelFromCodeThrowsOnInvalidCode() {
        assertThrows(IllegalArgumentException.class, () -> SecurityLevel.fromCode(99));
    }

    @Test
    void ensureSecurityClearanceNullExpiryIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new SecurityClearance(SecurityLevel.LOW, null));
    }

    @Test
    void ensureSecurityClearanceEqualityBasedOnLevelAndDate() {
        final LocalDate date = LocalDate.now().plusYears(1);
        final SecurityClearance a = new SecurityClearance(SecurityLevel.HIGH, date);
        final SecurityClearance b = new SecurityClearance(SecurityLevel.HIGH, date);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void ensureSecurityClearanceWithDifferentLevelIsNotEqual() {
        final LocalDate date = LocalDate.now().plusYears(1);
        final SecurityClearance a = new SecurityClearance(SecurityLevel.HIGH, date);
        final SecurityClearance b = new SecurityClearance(SecurityLevel.LOW, date);
        assertNotEquals(a, b);
    }

    @Test
    void ensureSecurityClearanceIsActiveOnExpirationDay() {
        final SecurityClearance clearance =
                new SecurityClearance(SecurityLevel.LOW, LocalDate.now());
        assertTrue(clearance.isActive());
    }


    @Test
    void ensureEmailEqualityBasedOnAddress() {
        final Email a = new Email("user@aisafe.com");
        final Email b = new Email("user@aisafe.com");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void ensureEmailsWithDifferentAddressesAreNotEqual() {
        final Email a = new Email("user@aisafe.com");
        final Email b = new Email("other@aisafe.com");
        assertNotEquals(a, b);
    }


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

    @Test
    void ensureMecanographicNumberCompareToOrdering() {
        final MecanographicNumber a = MecanographicNumber.valueOf("10000");
        final MecanographicNumber b = MecanographicNumber.valueOf("20000");
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(MecanographicNumber.valueOf("10000")));
    }


    @Test
    void ensureUpdateContactChangesEmailAndPhone() {
        final User user = baseBuilder().withMecanographicNumber("UPDATE1")
                .withSystemUser(dummySystemUser("user_update1", AiSafeRoles.ATCC)).build();
        user.updateContact(new Email("new@aisafe.com"), "912000000");
        assertEquals("new@aisafe.com", user.email().address());
        assertEquals("912000000", user.phoneNumber());
    }

    @Test
    void ensureUpdateContactRejectsNullEmail() {
        final User user = baseBuilder().withMecanographicNumber("UPDATE2")
                .withSystemUser(dummySystemUser("user_update2", AiSafeRoles.ATCC)).build();
        assertThrows(IllegalArgumentException.class,
                () -> user.updateContact(null, "912000000"));
    }

    @Test
    void ensureUpdateContactRejectsNullPhone() {
        final User user = baseBuilder().withMecanographicNumber("UPDATE3")
                .withSystemUser(dummySystemUser("user_update3", AiSafeRoles.ATCC)).build();
        assertThrows(IllegalArgumentException.class,
                () -> user.updateContact(new Email("new@aisafe.com"), null));
    }

    @Test
    void ensureUpdateContactRejectsBlankPhone() {
        final User user = baseBuilder().withMecanographicNumber("UPDATE4")
                .withSystemUser(dummySystemUser("user_update4", AiSafeRoles.ATCC)).build();
        assertThrows(IllegalArgumentException.class,
                () -> user.updateContact(new Email("new@aisafe.com"), "   "));
    }


    @Test
    void ensureGettersReturnCorrectValues() {
        final User user = baseBuilder().withMecanographicNumber("GETTERS")
                .withSystemUser(dummySystemUser("user_getters", AiSafeRoles.ATCC)).build();
        assertEquals("912345678", user.phoneNumber());
        assertEquals("test@aisafe.com", user.email().address());
        assertEquals("Operator", user.position());
        assertEquals(DUMMY_CLEARANCE, user.securityClearance());
    }

    @Test
    void ensureIdentityReturnsMecanographicNumber() {
        final User user = baseBuilder().withMecanographicNumber("IDENTITY")
                .withSystemUser(dummySystemUser("user_id", AiSafeRoles.ATCC)).build();
        assertEquals(MecanographicNumber.valueOf("IDENTITY"), user.identity());
    }

    @Test
    void ensureSecurityClearanceLevelGetterWorks() {
        final SecurityClearance clearance =
                new SecurityClearance(SecurityLevel.HIGH, LocalDate.now().plusYears(1));
        assertEquals(SecurityLevel.HIGH, clearance.level());
    }

    @Test
    void ensureSecurityClearanceExpirationDateGetterWorks() {
        final LocalDate date = LocalDate.now().plusYears(1);
        final SecurityClearance clearance = new SecurityClearance(SecurityLevel.LOW, date);
        assertEquals(date, clearance.expirationDate());
    }

    @Test
    void ensureSecurityClearanceToStringContainsLevel() {
        final SecurityClearance clearance =
                new SecurityClearance(SecurityLevel.HIGH, LocalDate.now().plusYears(1));
        assertTrue(clearance.toString().contains("HIGH"));
    }

    @Test
    void ensureSecurityClearanceHashCodeIsConsistent() {
        final LocalDate date = LocalDate.now().plusYears(1);
        final SecurityClearance a = new SecurityClearance(SecurityLevel.HIGH, date);
        final SecurityClearance b = new SecurityClearance(SecurityLevel.HIGH, date);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void ensureMecanographicNumberToStringReturnsValue() {
        final MecanographicNumber number = MecanographicNumber.valueOf("12345");
        assertEquals("12345", number.toString());
    }

    @Test
    void ensureMecanographicNumberHashCodeIsConsistent() {
        final MecanographicNumber a = MecanographicNumber.valueOf("12345");
        final MecanographicNumber b = MecanographicNumber.valueOf("12345");
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void ensureMecanographicNumberEqualsReturnsTrueForSameInstance() {
        final MecanographicNumber a = MecanographicNumber.valueOf("12345");
        assertEquals(a, a);
    }

    @Test
    void ensureMecanographicNumberEqualsReturnsFalseForNull() {
        final MecanographicNumber a = MecanographicNumber.valueOf("12345");
        assertNotEquals(null, a);
    }

    @Test
    void ensureMecanographicNumberEqualsReturnsFalseForDifferentType() {
        final MecanographicNumber a = MecanographicNumber.valueOf("12345");
        assertNotEquals("12345", a);
    }

    @Test
    void ensureEmailToStringReturnsAddress() {
        final Email email = new Email("user@aisafe.com");
        assertEquals("user@aisafe.com", email.toString());
    }

    @Test
    void ensureEmailHashCodeIsConsistent() {
        final Email a = new Email("user@aisafe.com");
        final Email b = new Email("user@aisafe.com");
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void ensureEmailEqualsReturnsTrueForSameInstance() {
        final Email email = new Email("user@aisafe.com");
        assertEquals(email, email);
    }

    @Test
    void ensureEmailEqualsReturnsFalseForNull() {
        final Email email = new Email("user@aisafe.com");
        assertNotEquals(null, email);
    }

    @Test
    void ensureEmailEqualsReturnsFalseForDifferentType() {
        final Email email = new Email("user@aisafe.com");
        assertNotEquals("user@aisafe.com", email);
    }

    @Test
    void ensureEmailAddressGetterWorks() {
        final Email email = new Email("user@aisafe.com");
        assertEquals("user@aisafe.com", email.address());
    }

    @Test
    void ensureEmailRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> new Email(null));
    }


    @Test
    void ensureSecurityLevelGetCodeReturnsCorrectValues() {
        assertEquals(1, SecurityLevel.LOW.getCode());
        assertEquals(2, SecurityLevel.GUARDED.getCode());
        assertEquals(3, SecurityLevel.ELEVATED.getCode());
        assertEquals(4, SecurityLevel.HIGH.getCode());
        assertEquals(5, SecurityLevel.CRITICAL.getCode());
    }

    @Test
    void ensureSecurityLevelFromCodeReturnsCorrectLevel() {
        assertEquals(SecurityLevel.LOW, SecurityLevel.fromCode(1));
        assertEquals(SecurityLevel.GUARDED, SecurityLevel.fromCode(2));
        assertEquals(SecurityLevel.ELEVATED, SecurityLevel.fromCode(3));
        assertEquals(SecurityLevel.HIGH, SecurityLevel.fromCode(4));
        assertEquals(SecurityLevel.CRITICAL, SecurityLevel.fromCode(5));
    }


    @Test
    void ensureSecurityClearanceNotEqualToNull() {
        final SecurityClearance clearance =
                new SecurityClearance(SecurityLevel.HIGH, LocalDate.now().plusYears(1));
        assertNotEquals(null, clearance);
    }

    @Test
    void ensureSecurityClearanceWithSameLevelButDifferentDateIsNotEqual() {
        final SecurityClearance a = new SecurityClearance(SecurityLevel.HIGH, LocalDate.now().plusYears(1));
        final SecurityClearance b = new SecurityClearance(SecurityLevel.HIGH, LocalDate.now().plusYears(2));
        assertNotEquals(a, b);
    }

    @Test
    void ensureSecurityClearanceToStringContainsExpirationDate() {
        final LocalDate date = LocalDate.now().plusYears(1);
        final SecurityClearance clearance = new SecurityClearance(SecurityLevel.LOW, date);
        assertTrue(clearance.toString().contains(date.toString()));
    }


    @Test
    void ensureUserSkillsAssessmentDateGetterReturnsCorrectValue() {
        final LocalDate assessmentDate = LocalDate.now().plusDays(10);
        final User user = baseBuilder()
                .withMecanographicNumber("SKILLS1")
                .withSystemUser(dummySystemUser("user_skills", AiSafeRoles.PILOT))
                .withSkillsAssessmentDate(assessmentDate)
                .build();
        assertEquals(assessmentDate, user.skillsAssessmentDate());
    }
}
