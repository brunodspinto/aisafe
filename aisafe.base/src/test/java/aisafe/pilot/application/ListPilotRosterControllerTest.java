package aisafe.pilot.application;

import aisafe.aircraftmodel.domain.AircraftModel;
import aisafe.aircraftmodel.domain.AircraftType;
import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.airtransportcompany.domain.IATACode;
import aisafe.airtransportcompany.domain.ICAOCode;
import aisafe.enginemodel.domain.EngineModel;
import aisafe.enginemodel.domain.EngineType;
import aisafe.infrastructure.persistence.inmemory.InMemoryAircraftModelRepository;
import aisafe.infrastructure.persistence.inmemory.InMemoryPilotRepository;
import aisafe.maker.domain.MakerName;
import aisafe.pilot.domain.Pilot;
import aisafe.usermanagement.domain.AiSafePasswordPolicy;
import aisafe.usermanagement.domain.AiSafeRoles;
import aisafe.usermanagement.domain.Email;
import aisafe.usermanagement.domain.MecanographicNumber;
import aisafe.usermanagement.domain.SecurityClearance;
import aisafe.usermanagement.domain.SecurityLevel;
import aisafe.usermanagement.domain.User;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ListPilotRosterController} (US076).
 *
 * <p>Tests use the package-private constructor to inject in-memory repositories, avoiding any
 * JPA or authentication context. All twelve tests focus exclusively on the in-memory filtering
 * logic; role-based authorization (AC076.4) is exercised through the public constructor path.</p>
 */
class ListPilotRosterControllerTest {

    /**
     * Lightweight authorization double. The package-private filter overloads exercised by these
     * tests never invoke it, but injecting a real (unconfigured) instance keeps the controller's
     * {@code authz} field non-null, avoiding the previous fragile {@code null} seam.
     */
    private static final AuthorizationService NO_AUTH = new AuthorizationService() { };

    private InMemoryPilotRepository pilotRepo;
    private InMemoryAircraftModelRepository modelRepo;
    private ListPilotRosterController controller;

    private AirTransportCompany companyA;
    private AirTransportCompany companyB;
    private IATACode iataA;
    private IATACode iataB;

    @BeforeEach
    void setUp() throws Exception {
        // The eapli InMemoryRepository uses a shared static DATA map across all instances.
        // Reset it before each test to ensure full isolation.
        final var reset = Class.forName(
                "eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryRepository")
                .getDeclaredMethod("reset");
        reset.setAccessible(true);
        reset.invoke(null);

        pilotRepo = new InMemoryPilotRepository();
        modelRepo = new InMemoryAircraftModelRepository();
        controller = new ListPilotRosterController(NO_AUTH, pilotRepo, modelRepo, null, null);

        iataA = IATACode.valueOf("TP");
        iataB = IATACode.valueOf("AF");
        companyA = new AirTransportCompany("TAP Air Portugal", iataA, ICAOCode.valueOf("TAP"));
        companyB = new AirTransportCompany("Air France", iataB, ICAOCode.valueOf("AFR"));
    }

    // -------------------------------------------------------------------------
    // Helpers (replicated from PilotTest)
    // -------------------------------------------------------------------------

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

    private static AircraftModel validAircraftModel(final String modelName) {
        return new AircraftModel(
                modelName, MakerName.valueOf("Airbus"), AircraftType.PASSENGER,
                41140, 79016, 62732, 20894,
                12500, 230, 34.3, 125.0,
                0.026, 1.5, 5765.0,
                new EngineModel("CFM56", MakerName.valueOf("CFM"), EngineType.TURBOFAN,
                        120.0, 115.0, 0.35));
    }

    /** Uses reflection to flip {@code active} to {@code false} on a saved pilot. */
    private static void deactivate(final Pilot pilot) throws Exception {
        final Field f = Pilot.class.getDeclaredField("active");
        f.setAccessible(true);
        f.set(pilot, false);
    }

    // -------------------------------------------------------------------------
    // allPilots(AirTransportCompany)
    // -------------------------------------------------------------------------

    @Test
    void allPilots_returnsBothPilotsOfSameCompany() {
        pilotRepo.save(new Pilot(validUser("pilot1"), iataA, Set.of(1L)));
        pilotRepo.save(new Pilot(validUser("pilot2"), iataA, Set.of(1L)));

        final List<Pilot> result = controller.allPilots(companyA);

        assertEquals(2, result.size());
    }

    @Test
    void allPilots_excludesPilotFromOtherCompany() {
        pilotRepo.save(new Pilot(validUser("pilot1"), iataA, Set.of(1L)));
        pilotRepo.save(new Pilot(validUser("pilot2"), iataB, Set.of(1L)));

        final List<Pilot> result = controller.allPilots(companyA);

        assertEquals(1, result.size());
    }

    @Test
    void allPilots_emptyWhenNoPilots() {
        final List<Pilot> result = controller.allPilots(companyA);

        assertTrue(result.isEmpty());
    }

    // -------------------------------------------------------------------------
    // activePilots(AirTransportCompany)
    // -------------------------------------------------------------------------

    @Test
    void activePilots_returnsOnlyActivePilots() throws Exception {
        final Pilot active = pilotRepo.save(new Pilot(validUser("pilot-active"), iataA, Set.of(1L)));
        final Pilot inactive = pilotRepo.save(new Pilot(validUser("pilot-inactive"), iataA, Set.of(1L)));
        deactivate(inactive);

        final List<Pilot> result = controller.activePilots(companyA);

        assertEquals(1, result.size());
        assertTrue(result.contains(active));
    }

    @Test
    void activePilots_emptyWhenNoActivePilots() throws Exception {
        final Pilot p = pilotRepo.save(new Pilot(validUser("pilot-only"), iataA, Set.of(1L)));
        deactivate(p);

        final List<Pilot> result = controller.activePilots(companyA);

        assertTrue(result.isEmpty());
    }

    // -------------------------------------------------------------------------
    // pilotsByCertifiedModel(AirTransportCompany, String)
    // -------------------------------------------------------------------------

    @Test
    void pilotsByCertifiedModel_returnsMatchingPilot() {
        final AircraftModel a320 = modelRepo.save(validAircraftModel("A320"));
        final Pilot certified = pilotRepo.save(
                new Pilot(validUser("pilot-cert"), iataA, Set.of(a320.identity())));
        pilotRepo.save(new Pilot(validUser("pilot-other"), iataA, Set.of(99L)));

        final List<Pilot> result = controller.pilotsByCertifiedModel(companyA, "A320");

        assertEquals(1, result.size());
        assertTrue(result.contains(certified));
    }

    @Test
    void pilotsByCertifiedModel_isCaseInsensitive() {
        final AircraftModel a320 = modelRepo.save(validAircraftModel("A320"));
        final Pilot certified = pilotRepo.save(
                new Pilot(validUser("pilot-cert"), iataA, Set.of(a320.identity())));

        final List<Pilot> result = controller.pilotsByCertifiedModel(companyA, "a320");

        assertEquals(1, result.size());
        assertTrue(result.contains(certified));
    }

    @Test
    void pilotsByCertifiedModel_excludesUncertifiedPilot() {
        modelRepo.save(validAircraftModel("A320"));
        pilotRepo.save(new Pilot(validUser("pilot-uncert"), iataA, Set.of(99L)));

        final List<Pilot> result = controller.pilotsByCertifiedModel(companyA, "A320");

        assertTrue(result.isEmpty());
    }

    @Test
    void pilotsByCertifiedModel_emptyWhenModelNameUnknown() {
        pilotRepo.save(new Pilot(validUser("pilot1"), iataA, Set.of(1L)));

        final List<Pilot> result = controller.pilotsByCertifiedModel(companyA, "UNKNOWN_MODEL");

        assertTrue(result.isEmpty());
    }

    @Test
    void pilotsByCertifiedModel_withLeadingAndTrailingSpacesInModelName() {
        final AircraftModel a320 = modelRepo.save(validAircraftModel("A320"));
        final Pilot certified = pilotRepo.save(
                new Pilot(validUser("pilot-cert"), iataA, Set.of(a320.identity())));

        final List<Pilot> result = controller.pilotsByCertifiedModel(companyA, "  A320  ");

        assertEquals(1, result.size());
        assertTrue(result.contains(certified));
    }

    @Test
    void allPilots_includesInactivePilots() throws Exception {
        final Pilot active   = pilotRepo.save(new Pilot(validUser("pilot-active2"),   iataA, Set.of(1L)));
        final Pilot inactive = pilotRepo.save(new Pilot(validUser("pilot-inactive2"), iataA, Set.of(1L)));
        deactivate(inactive);

        final List<Pilot> result = controller.allPilots(companyA);

        assertEquals(2, result.size());
        assertTrue(result.contains(active));
        assertTrue(result.contains(inactive));
    }

    @Test
    void pilotsByCertifiedModel_pilotWithNoCertificationsIsExcluded() throws Exception {
        final AircraftModel a320 = modelRepo.save(validAircraftModel("B737"));
        final Pilot pilot = pilotRepo.save(
                new Pilot(validUser("pilot-nocert"), iataA, Set.of(a320.identity())));
        // Clear certifications via reflection to simulate a pilot with no certifications
        final Field f = Pilot.class.getDeclaredField("certifiedAircraftModelIds");
        f.setAccessible(true);
        ((java.util.Set<?>) f.get(pilot)).clear();

        final List<Pilot> result = controller.pilotsByCertifiedModel(companyA, "B737");

        assertTrue(result.isEmpty());
    }
}
