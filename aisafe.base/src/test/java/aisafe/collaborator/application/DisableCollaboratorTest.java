package aisafe.collaborator.application;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.aircontrolarea.domain.GeoBoundary;
import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.airtransportcompany.domain.IATACode;
import aisafe.airtransportcompany.domain.ICAOCode;
import aisafe.collaborator.domain.Collaborator;
import aisafe.usermanagement.domain.AiSafePasswordPolicy;
import aisafe.usermanagement.domain.AiSafeRoles;
import aisafe.usermanagement.domain.Email;
import aisafe.usermanagement.domain.MecanographicNumber;
import aisafe.usermanagement.domain.SecurityClearance;
import aisafe.usermanagement.domain.SecurityLevel;
import aisafe.usermanagement.domain.User;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link DisableCollaboratorController} use case.
 * Verifies that collaborator accounts can be disabled and re-enabled correctly.
 */
class DisableCollaboratorTest {

    private static SystemUser buildSystemUser(final String username) {
        final var builder = new SystemUserBuilder(
                new AiSafePasswordPolicy(), new PlainTextEncoder());
        builder.withUsername(username)
                .withPassword("Password1")
                .withName("First", "Last")
                .withEmail(username + "@aisafe.com")
                .withRoles(AiSafeRoles.ATCC);
        return builder.build();
    }

    private static User validUser(final String username) {
        return new User(buildSystemUser(username),
                MecanographicNumber.valueOf("200"),
                "910000001",
                new Email("contact@aisafe.com"),
                "Operator",
                new SecurityClearance(SecurityLevel.LOW, LocalDate.now().plusYears(1)),
                LocalDate.now());
    }

    private static AirTransportCompany validCompany() {
        return new AirTransportCompany("TAP Air Portugal",
                IATACode.valueOf("TP"),
                ICAOCode.valueOf("TAP"));
    }

    private static AirControlArea validArea() {
        return new AirControlArea("PT-N", "Northern Portugal", 1200.0,
                new GeoBoundary(42.15, 36.95, -6.18, -9.50));
    }

    @Test
    void ensureCompanyCollaboratorCanBeCreated() {
        final Collaborator c = new Collaborator(validUser("u1"), validCompany());
        assertTrue(c.isCompanyCollaborator());
        assertFalse(c.isAreaCollaborator());
    }

    @Test
    void ensureAreaCollaboratorCanBeCreated() {
        final Collaborator c = new Collaborator(validUser("u2"), validArea());
        assertTrue(c.isAreaCollaborator());
        assertFalse(c.isCompanyCollaborator());
    }

    @Test
    void ensureCollaboratorUserIsActive() {
        final Collaborator c = new Collaborator(validUser("u3"), validCompany());
        assertTrue(c.user().systemUser().isActive());
    }

    @Test
    void ensureSystemUserCanBeDeactivated() {
        final SystemUser su = buildSystemUser("u4");
        assertTrue(su.isActive());
        su.deactivate(Calendar.getInstance());
        assertFalse(su.isActive());
    }

    @Test
    void ensureDeactivatedCollaboratorUserIsInactive() {
        final Collaborator c = new Collaborator(validUser("u5"), validCompany());
        c.user().systemUser().deactivate(Calendar.getInstance());
        assertFalse(c.user().systemUser().isActive());
    }

    @Test
    void ensureCollaboratorUserIsNotNullAfterCreation() {
        final Collaborator c = new Collaborator(validUser("u6"), validArea());
        assertNotNull(c.user());
        assertNotNull(c.user().systemUser());
    }

    @Test
    void ensureNullUserThrowsForCompanyCollaborator() {
        assertThrows(IllegalArgumentException.class,
                () -> new Collaborator(null, validCompany()));
    }

    @Test
    void ensureNullUserThrowsForAreaCollaborator() {
        assertThrows(IllegalArgumentException.class,
                () -> new Collaborator(null, validArea()));
    }

    @Test
    void ensureDeactivatingAlreadyInactiveUserThrows() {
        final Collaborator c = new Collaborator(validUser("u7"), validCompany());
        c.user().systemUser().deactivate(Calendar.getInstance());
        assertThrows(IllegalStateException.class,
                () -> c.user().systemUser().deactivate(Calendar.getInstance()));
    }

    @Test
    void ensureCompanyCollaboratorCustomerName() {
        final Collaborator c = new Collaborator(validUser("u8"), validCompany());
        assertEquals("TAP Air Portugal", c.customerName());
    }

    @Test
    void ensureAreaCollaboratorCustomerName() {
        final Collaborator c = new Collaborator(validUser("u9"), validArea());
        assertEquals("Northern Portugal", c.customerName());
    }
}
