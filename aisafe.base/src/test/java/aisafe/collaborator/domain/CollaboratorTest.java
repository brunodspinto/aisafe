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
        final Collaborator collaborator = new Collaborator(validUser("user1"), validCompany().identity());
        assertTrue(collaborator.isCompanyCollaborator());
        assertFalse(collaborator.isAreaCollaborator());
        assertEquals("TP", collaborator.customerName());
    }

    @Test
    void ensureAreaCollaboratorCanBeCreated() {
        final Collaborator collaborator = new Collaborator(validUser("user2"), validArea().identity());
        assertTrue(collaborator.isAreaCollaborator());
        assertFalse(collaborator.isCompanyCollaborator());
        assertEquals("PT-N", collaborator.customerName());
    }

    @Test
    void ensureUserCannotBeNullForCompanyCollaborator() {
        assertThrows(IllegalArgumentException.class,
                () -> new Collaborator(null, validCompany().identity()));
    }

    @Test
    void ensureUserCannotBeNullForAreaCollaborator() {
        assertThrows(IllegalArgumentException.class,
                () -> new Collaborator(null, validArea().identity()));
    }

    @Test
    void ensureCompanyCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new Collaborator(validUser("user3"), (IATACode) null));
    }

    @Test
    void ensureAreaCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new Collaborator(validUser("user4"), (AirControlAreaCode) null));
    }

    @Test
    void ensureCompanyCollaboratorHasCorrectUser() {
        final User user = validUser("user5");
        final Collaborator collaborator = new Collaborator(user, validCompany().identity());
        assertEquals(user, collaborator.user());
    }

    @Test
    void ensureAreaCollaboratorHasCorrectUser() {
        final User user = validUser("user6");
        final Collaborator collaborator = new Collaborator(user, validArea().identity());
        assertEquals(user, collaborator.user());
    }

    @Test
    void ensureToStringContainsCustomerName() {
        final Collaborator collaborator = new Collaborator(validUser("user7"), validCompany().identity());
        assertTrue(collaborator.toString().contains("TP"));
    }

    @Test
    void ensureEqualsReturnsTrueForSameInstance() {
        final Collaborator collaborator = new Collaborator(validUser("user8"), validCompany().identity());
        assertEquals(collaborator, collaborator);
    }

    @Test
    void ensureEqualsReturnsFalseForNull() {
        final Collaborator collaborator = new Collaborator(validUser("user9"), validCompany().identity());
        assertNotEquals(null, collaborator);
    }

    @Test
    void ensureHashCodeIsConsistent() {
        final Collaborator collaborator = new Collaborator(validUser("user10"), validCompany().identity());
        assertEquals(collaborator.hashCode(), collaborator.hashCode());
    }

    @Test
    void ensureSameAsReturnsTrueForSameInstance() {
        final Collaborator collaborator = new Collaborator(validUser("user11"), validCompany().identity());
        assertTrue(collaborator.sameAs(collaborator));
    }

    @Test
    void ensureCompanyIataCodeGetterWorks() {
        final IATACode iataCode = validCompany().identity();
        final Collaborator collaborator = new Collaborator(validUser("user12"), iataCode);
        assertEquals(iataCode, collaborator.companyIataCode());
        assertNull(collaborator.areaCode());
    }

    @Test
    void ensureAreaCodeGetterWorks() {
        final AirControlAreaCode code = validArea().identity();
        final Collaborator collaborator = new Collaborator(validUser("user13"), code);
        assertEquals(code, collaborator.areaCode());
        assertNull(collaborator.companyIataCode());
    }


    @Test
    void ensureEqualsReturnsFalseForDifferentType() {
        final Collaborator collaborator = new Collaborator(validUser("user15"), validCompany().identity());
        assertNotEquals("string", collaborator);
    }

    @Test
    void ensureCustomerNameForCompanyCollaborator() {
        final Collaborator collaborator = new Collaborator(validUser("user16"), validCompany().identity());
        assertEquals("TP", collaborator.customerName());
    }

    @Test
    void ensureCustomerNameForAreaCollaborator() {
        final Collaborator collaborator = new Collaborator(validUser("user17"), validArea().identity());
        assertEquals("PT-N", collaborator.customerName());
    }


    @Test
    void ensureTwoCollaboratorsWithSameIdAreEqual() {
        final Collaborator collaborator = new Collaborator(validUser("user20"), validCompany().identity());
        assertTrue(collaborator.sameAs(collaborator));
    }

    @Test
    void ensureToStringContainsAreaCode() {
        final Collaborator collaborator = new Collaborator(validUser("user21"), validArea().identity());
        assertTrue(collaborator.toString().contains("PT-N"));
    }

    @Test
    void ensureCompanyCollaboratorHasNullArea() {
        final Collaborator collaborator = new Collaborator(validUser("user22"), validCompany().identity());
        assertNull(collaborator.areaCode());
        assertNotNull(collaborator.companyIataCode());
    }

    @Test
    void ensureAreaCollaboratorHasNullCompany() {
        final Collaborator collaborator = new Collaborator(validUser("user23"), validArea().identity());
        assertNull(collaborator.companyIataCode());
        assertNotNull(collaborator.areaCode());
    }
}
