package aisafe.collaborator.domain;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.aircontrolarea.domain.AirControlAreaCode;
import aisafe.aircontrolarea.domain.GeoBoundary;
import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.airtransportcompany.domain.IATACode;
import aisafe.airtransportcompany.domain.ICAOCode;
import aisafe.usermanagement.domain.AiSafeRoles;
import aisafe.usermanagement.domain.Email;
import aisafe.usermanagement.domain.MecanographicNumber;
import aisafe.usermanagement.domain.SecurityClearance;
import aisafe.usermanagement.domain.SecurityLevel;
import aisafe.usermanagement.domain.User;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;
import aisafe.usermanagement.domain.AiSafePasswordPolicy;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Collaborator} aggregate root.
 * Verifies construction, company/area associations, and business rules.
 */
class CollaboratorTest {

    private static User validUser(final String username) {
        final var builder = new SystemUserBuilder(
                new AiSafePasswordPolicy(), new PlainTextEncoder());
        builder.withUsername(username)
                .withPassword("Password1")
                .withName("First", "Last")
                .withEmail(username + "@aisafe.com")
                .withRoles(AiSafeRoles.ATCC);
        final var systemUser = builder.build();
        return new User(systemUser,
                MecanographicNumber.valueOf("123"),
                "910000000",
                new Email("test@aisafe.com"),
                "Pilot",
                new SecurityClearance(SecurityLevel.LOW, LocalDate.now().plusYears(1)),
                LocalDate.now());
    }

    private static AirTransportCompany validCompany() {
        return new AirTransportCompany("TAP Air Portugal",
                IATACode.valueOf("TP"),
                ICAOCode.valueOf("TAP"));
    }

    private static AirControlArea validArea() {
        return new AirControlArea(AirControlAreaCode.valueOf("PT-N"), "Northern Portugal", 1200.0,
                new GeoBoundary(42.15, 36.95, -6.18, -9.50));
    }

    @Test
    void ensureCompanyCollaboratorCanBeCreated() {
        final Collaborator collaborator = new Collaborator(validUser("user1"), validCompany());
        assertTrue(collaborator.isCompanyCollaborator());
        assertFalse(collaborator.isAreaCollaborator());
        assertEquals("TAP Air Portugal", collaborator.customerName());
    }

    @Test
    void ensureAreaCollaboratorCanBeCreated() {
        final Collaborator collaborator = new Collaborator(validUser("user2"), validArea());
        assertTrue(collaborator.isAreaCollaborator());
        assertFalse(collaborator.isCompanyCollaborator());
        assertEquals("Northern Portugal", collaborator.customerName());
    }

    @Test
    void ensureUserCannotBeNullForCompanyCollaborator() {
        assertThrows(IllegalArgumentException.class,
                () -> new Collaborator(null, validCompany()));
    }

    @Test
    void ensureUserCannotBeNullForAreaCollaborator() {
        assertThrows(IllegalArgumentException.class,
                () -> new Collaborator(null, validArea()));
    }

    @Test
    void ensureCompanyCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new Collaborator(validUser("user3"), (AirTransportCompany) null));
    }

    @Test
    void ensureAreaCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new Collaborator(validUser("user4"), (AirControlArea) null));
    }

    @Test
    void ensureCompanyCollaboratorHasCorrectUser() {
        final User user = validUser("user5");
        final Collaborator collaborator = new Collaborator(user, validCompany());
        assertEquals(user, collaborator.user());
    }

    @Test
    void ensureAreaCollaboratorHasCorrectUser() {
        final User user = validUser("user6");
        final Collaborator collaborator = new Collaborator(user, validArea());
        assertEquals(user, collaborator.user());
    }

    @Test
    void ensureToStringContainsCustomerName() {
        final Collaborator collaborator = new Collaborator(validUser("user7"), validCompany());
        assertTrue(collaborator.toString().contains("TAP Air Portugal"));
    }

    @Test
    void ensureEqualsReturnsTrueForSameInstance() {
        final Collaborator collaborator = new Collaborator(validUser("user8"), validCompany());
        assertEquals(collaborator, collaborator);
    }

    @Test
    void ensureEqualsReturnsFalseForNull() {
        final Collaborator collaborator = new Collaborator(validUser("user9"), validCompany());
        assertNotEquals(null, collaborator);
    }

    @Test
    void ensureHashCodeIsConsistent() {
        final Collaborator collaborator = new Collaborator(validUser("user10"), validCompany());
        assertEquals(collaborator.hashCode(), collaborator.hashCode());
    }

    @Test
    void ensureSameAsReturnsTrueForSameInstance() {
        final Collaborator collaborator = new Collaborator(validUser("user11"), validCompany());
        assertTrue(collaborator.sameAs(collaborator));
    }

    @Test
    void ensureAirTransportCompanyGetterWorks() {
        final AirTransportCompany company = validCompany();
        final Collaborator collaborator = new Collaborator(validUser("user12"), company);
        assertEquals(company, collaborator.airTransportCompany());
        assertNull(collaborator.airControlArea());
    }

    @Test
    void ensureAirControlAreaGetterWorks() {
        final AirControlArea area = validArea();
        final Collaborator collaborator = new Collaborator(validUser("user13"), area);
        assertEquals(area, collaborator.airControlArea());
        assertNull(collaborator.airTransportCompany());
    }

    @Test
    void ensureIdentityIsNullBeforePersistence() {
        final Collaborator collaborator = new Collaborator(validUser("user14"), validCompany());
        assertNull(collaborator.identity());
    }

    @Test
    void ensureEqualsReturnsFalseForDifferentType() {
        final Collaborator collaborator = new Collaborator(validUser("user15"), validCompany());
        assertNotEquals("string", collaborator);
    }

    @Test
    void ensureCustomerNameForCompanyCollaborator() {
        final Collaborator collaborator = new Collaborator(validUser("user16"), validCompany());
        assertEquals("TAP Air Portugal", collaborator.customerName());
    }

    @Test
    void ensureCustomerNameForAreaCollaborator() {
        final Collaborator collaborator = new Collaborator(validUser("user17"), validArea());
        assertEquals("Northern Portugal", collaborator.customerName());
    }


    @Test
    void ensureTwoCollaboratorsWithSameIdAreEqual() {
        final Collaborator collaborator = new Collaborator(validUser("user20"), validCompany());
        assertTrue(collaborator.sameAs(collaborator));
    }

    @Test
    void ensureToStringContainsAreaName() {
        final Collaborator collaborator = new Collaborator(validUser("user21"), validArea());
        assertTrue(collaborator.toString().contains("Northern Portugal"));
    }

    @Test
    void ensureCompanyCollaboratorHasNullArea() {
        final Collaborator collaborator = new Collaborator(validUser("user22"), validCompany());
        assertNull(collaborator.airControlArea());
        assertNotNull(collaborator.airTransportCompany());
    }

    @Test
    void ensureAreaCollaboratorHasNullCompany() {
        final Collaborator collaborator = new Collaborator(validUser("user23"), validArea());
        assertNull(collaborator.airTransportCompany());
        assertNotNull(collaborator.airControlArea());
    }
}
