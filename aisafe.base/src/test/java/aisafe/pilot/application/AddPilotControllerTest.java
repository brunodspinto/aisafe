package aisafe.pilot.application;

import aisafe.aircraftmodel.domain.AircraftModel;
import aisafe.aircraftmodel.domain.AircraftType;
import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.airtransportcompany.domain.IATACode;
import aisafe.airtransportcompany.domain.ICAOCode;
import aisafe.auth.AuthenticationContext;
import aisafe.collaborator.domain.Collaborator;
import aisafe.enginemodel.domain.EngineModel;
import aisafe.enginemodel.domain.EngineType;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.maker.domain.MakerName;
import aisafe.pilot.domain.Pilot;
import aisafe.usermanagement.domain.AiSafePasswordPolicy;
import aisafe.usermanagement.domain.AiSafeRoles;
import aisafe.usermanagement.domain.Email;
import aisafe.usermanagement.domain.MecanographicNumber;
import aisafe.usermanagement.domain.SecurityClearance;
import aisafe.usermanagement.domain.SecurityLevel;
import aisafe.usermanagement.domain.User;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddPilotControllerTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static final String PASSWORD = "Password1";

    private final AddPilotController controller = new AddPilotController();

    @BeforeAll
    static void configureAuthz() {
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
    void ensurePilotIsAddedToAuthenticatedCollaboratorCompany() {
        final String companyCode = nextCompanyCode();
        final AirTransportCompany company = persistCompany(companyCode);
        final String atccUsername = authenticateAsAtccOf(company);
        final AircraftModel model = persistAircraftModel();

        final String pilotUsername = "pilot-" + SEQUENCE.incrementAndGet();
        final Pilot pilot = controller.addPilot(
                pilotUsername, PASSWORD, "Maria", "Voo",
                pilotUsername + "@aisafe.com", "912345678", "Captain",
                new Email(pilotUsername + "@aisafe.com"),
                new SecurityClearance(SecurityLevel.HIGH, LocalDate.now().plusYears(1)),
                LocalDate.now(),
                Set.of(model.identity()));

        assertNotNull(pilot);
        assertEquals(company.identity(), pilot.companyIataCode());
        assertTrue(pilot.isCertifiedFor(model.identity()));
        assertTrue(pilot.isActive());
        assertTrue(pilot.user().systemUser().hasAny(AiSafeRoles.PILOT));

        // not consumed but documents the ATCC under whom the pilot was created
        assertNotNull(atccUsername);
    }

    @Test
    void ensurePilotRequiresAtLeastOneCertification() {
        final AirTransportCompany company = persistCompany(nextCompanyCode());
        authenticateAsAtccOf(company);

        final String pilotUsername = "pilot-" + SEQUENCE.incrementAndGet();
        assertThrows(IllegalArgumentException.class, () -> controller.addPilot(
                pilotUsername, PASSWORD, "No", "Cert",
                pilotUsername + "@aisafe.com", "912345678", "Captain",
                new Email(pilotUsername + "@aisafe.com"),
                new SecurityClearance(SecurityLevel.HIGH, LocalDate.now().plusYears(1)),
                LocalDate.now(),
                Set.of()));
    }

    @Test
    void ensureUnknownAircraftModelIsRejected() {
        final AirTransportCompany company = persistCompany(nextCompanyCode());
        authenticateAsAtccOf(company);

        final String pilotUsername = "pilot-" + SEQUENCE.incrementAndGet();
        assertThrows(IllegalArgumentException.class, () -> controller.addPilot(
                pilotUsername, PASSWORD, "Bad", "Model",
                pilotUsername + "@aisafe.com", "912345678", "Captain",
                new Email(pilotUsername + "@aisafe.com"),
                new SecurityClearance(SecurityLevel.HIGH, LocalDate.now().plusYears(1)),
                LocalDate.now(),
                Set.of(999_999L)));
    }

    private static AirTransportCompany persistCompany(final String companyCode) {
        final AirTransportCompany company = new AirTransportCompany(
                "Company " + companyCode,
                IATACode.valueOf(companyCode),
                ICAOCode.valueOf(companyCode + "C"));
        return PersistenceContext.repositories().airTransportCompanies().save(company);
    }

    /**
     * Creates an ATCC system user that is a collaborator of the given company and authenticates as them.
     *
     * @param company the company the ATCC belongs to
     * @return the ATCC username
     */
    private static String authenticateAsAtccOf(final AirTransportCompany company) {
        final String username = "atcc-" + SEQUENCE.incrementAndGet();
        final var builder = new SystemUserBuilder(new AiSafePasswordPolicy(), new PlainTextEncoder());
        builder.withUsername(username)
                .withPassword(PASSWORD)
                .withName("Air", "Manager")
                .withEmail(username + "@aisafe.com")
                .withRoles(AiSafeRoles.ATCC);
        final SystemUser su = builder.build();
        PersistenceContext.repositories().systemUsers().save(su);

        final User atccUser = new User(su,
                MecanographicNumber.valueOf("ATC" + SEQUENCE.incrementAndGet()),
                "919999999",
                new Email(username + "@aisafe.com"),
                "Manager",
                new SecurityClearance(SecurityLevel.HIGH, LocalDate.now().plusYears(1)),
                LocalDate.now());
        PersistenceContext.repositories().users().save(atccUser);

        PersistenceContext.repositories().collaborators()
                .save(new Collaborator(atccUser, company.identity()));

        AuthenticationContext.authenticate(username, PASSWORD);
        return username;
    }

    private static AircraftModel persistAircraftModel() {
        final EngineModel engine = new EngineModel(
                "CFM56-" + SEQUENCE.incrementAndGet(), MakerName.valueOf("CFM International"),
                EngineType.TURBOFAN, 120.0, 115.0, 0.35);
        final AircraftModel model = new AircraftModel(
                "737-" + SEQUENCE.incrementAndGet(), MakerName.valueOf("Boeing"), AircraftType.PASSENGER,
                41140, 79016, 62732, 20894, 12500, 230, 34.3, 125.0,
                0.026, 1.5, 5765.0, engine);
        return PersistenceContext.repositories().aircraftModels().save(model);
    }

    private static String nextCompanyCode() {
        final int index = SEQUENCE.incrementAndGet();
        final char first = (char) ('A' + (index % 26));
        final char second = (char) ('A' + ((index / 26) % 26));
        return "" + first + second;
    }
}
