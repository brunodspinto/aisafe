package aisafe.pilot.domain;

import aisafe.airtransportcompany.domain.IATACode;
import aisafe.usermanagement.domain.AiSafePasswordPolicy;
import aisafe.usermanagement.domain.AiSafeRoles;
import aisafe.usermanagement.domain.Email;
import aisafe.usermanagement.domain.MecanographicNumber;
import aisafe.usermanagement.domain.SecurityClearance;
import aisafe.usermanagement.domain.SecurityLevel;
import aisafe.usermanagement.domain.User;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link Pilot} aggregate root (US075).
 * Verifies construction invariants, certifications handling and identity semantics.
 */
class PilotTest {

    private static User validUser(final String username) {
        final var builder = new SystemUserBuilder(
                new AiSafePasswordPolicy(), new PlainTextEncoder());
        builder.withUsername(username)
                .withPassword("Password1")
                .withName("First", "Last")
                .withEmail(username + "@aisafe.com")
                .withRoles(AiSafeRoles.PILOT);
        return new User(builder.build(),
                MecanographicNumber.valueOf("PIL00001"),
                "910000000",
                new Email(username + "@aisafe.com"),
                "Pilot",
                new SecurityClearance(SecurityLevel.LOW, LocalDate.now().plusYears(1)),
                LocalDate.now());
    }

    private static IATACode validCompany() {
        return IATACode.valueOf("TP");
    }

    @Test
    void ensureValidPilotCanBeCreated() {
        final Pilot pilot = new Pilot(validUser("pilot1"), validCompany(), Set.of(1L, 2L));
        assertEquals(validCompany(), pilot.companyIataCode());
        assertEquals(2, pilot.certifiedAircraftModelIds().size());
        assertTrue(pilot.isActive());
    }

    @Test
    void ensurePilotIsActiveByDefault() {
        final Pilot pilot = new Pilot(validUser("pilot-active"), validCompany(), Set.of(1L));
        assertTrue(pilot.isActive());
    }

    @Test
    void ensureUserCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new Pilot(null, validCompany(), Set.of(1L)));
    }

    @Test
    void ensureCompanyCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new Pilot(validUser("pilot2"), null, Set.of(1L)));
    }

    @Test
    void ensurePilotCertificationSetCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new Pilot(validUser("pilot3"), validCompany(), null));
    }

    @Test
    void ensurePilotMustHaveAtLeastOneCertification() {
        assertThrows(IllegalArgumentException.class,
                () -> new Pilot(validUser("pilot4"), validCompany(), Set.of()));
    }

    @Test
    void ensureCertificationsCannotContainNull() {
        final Set<Long> withNull = new HashSet<>();
        withNull.add(null);
        assertThrows(IllegalArgumentException.class,
                () -> new Pilot(validUser("pilot5"), validCompany(), withNull));
    }

    @Test
    void ensureIsCertifiedForReturnsTrueForCertifiedModel() {
        final Pilot pilot = new Pilot(validUser("pilot6"), validCompany(), Set.of(1L));
        assertTrue(pilot.isCertifiedFor(1L));
        assertFalse(pilot.isCertifiedFor(99L));
    }

    @Test
    void ensureCertificationsAreDefensivelyCopied() {
        final Set<Long> source = new HashSet<>(Set.of(1L));
        final Pilot pilot = new Pilot(validUser("pilot7"), validCompany(), source);
        source.add(99L);
        assertEquals(1, pilot.certifiedAircraftModelIds().size());
        assertFalse(pilot.isCertifiedFor(99L));
    }

    @Test
    void ensureCertificationsAreUnmodifiable() {
        final Pilot pilot = new Pilot(validUser("pilot8"), validCompany(), Set.of(1L));
        assertThrows(UnsupportedOperationException.class,
                () -> pilot.certifiedAircraftModelIds().add(2L));
    }

    @Test
    void ensurePilotHasCorrectUser() {
        final User user = validUser("pilot9");
        final Pilot pilot = new Pilot(user, validCompany(), Set.of(1L));
        assertEquals(user, pilot.user());
    }

    @Test
    void ensureToStringContainsCompany() {
        final Pilot pilot = new Pilot(validUser("pilot10"), validCompany(), Set.of(1L));
        assertTrue(pilot.toString().contains("TP"));
    }

    @Test
    void ensureEqualsReturnsTrueForSameInstance() {
        final Pilot pilot = new Pilot(validUser("pilot11"), validCompany(), Set.of(1L));
        assertEquals(pilot, pilot);
    }

    @Test
    void ensureEqualsReturnsFalseForNull() {
        final Pilot pilot = new Pilot(validUser("pilot12"), validCompany(), Set.of(1L));
        assertNotEquals(null, pilot);
    }

    @Test
    void ensureHashCodeIsConsistent() {
        final Pilot pilot = new Pilot(validUser("pilot13"), validCompany(), Set.of(1L));
        assertEquals(pilot.hashCode(), pilot.hashCode());
    }

    @Test
    void ensureSameAsReturnsTrueForSameInstance() {
        final Pilot pilot = new Pilot(validUser("pilot14"), validCompany(), Set.of(1L));
        assertTrue(pilot.sameAs(pilot));
    }
}
