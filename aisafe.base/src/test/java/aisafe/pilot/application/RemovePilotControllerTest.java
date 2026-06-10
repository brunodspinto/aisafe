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
import aisafe.flightplan.domain.FlightPlan;
import aisafe.flightplan.domain.FlightPlanDesignator;
import aisafe.flightplan.domain.FuelQuantity;
import aisafe.aircraft.domain.RegistrationNumber;
import aisafe.dsl.ast.FlightType;
import aisafe.flightroute.domain.RouteName;
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
import eapli.framework.infrastructure.authz.application.exceptions.UnauthenticatedException;
import eapli.framework.infrastructure.authz.application.exceptions.UnauthorizedException;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;
import eapli.framework.infrastructure.authz.domain.model.Username;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link RemovePilotController} (US077).
 * Uses in-memory repositories and real authentication infrastructure.
 */
class RemovePilotControllerTest {

    private static final String ATCC_USERNAME = "atcc-us077";
    private static final String ATCC_PASSWORD = "Password1";
    private static final String OPERATOR_USERNAME = "bo-op-us077";
    private static final String OPERATOR_PASSWORD = "Password1";

    private final RemovePilotController controller = new RemovePilotController();

    private static Pilot pilotActive;
    private static Pilot pilotToDeactivate1;
    private static Pilot pilotToDeactivate2;
    private static Pilot pilotForAlreadyInactiveTest;
    private static Pilot pilotWithFlightPlan;
    private static Pilot otherCompanyPilot;

    @BeforeAll
    static void bootstrapAll() {
        AuthzRegistry.configure(
                PersistenceContext.repositories().systemUsers(),
                new AiSafePasswordPolicy(),
                new PlainTextEncoder());

        ensureSystemUserExists(ATCC_USERNAME, ATCC_PASSWORD, AiSafeRoles.ATCC);
        ensureSystemUserExists(OPERATOR_USERNAME, OPERATOR_PASSWORD, AiSafeRoles.BACKOFFICE_OPERATOR);

        ensureCompanyExists("TP", "TAP Air Portugal", "TAP");
        ensureCompanyExists("RY", "Ryanair", "RYR");

        ensureAtccUserAndCollaboratorExist();

        final Long modelId = ensureAircraftModelExists();

        pilotActive             = ensurePilotExists("pilot-077-a", "US077-P01", IATACode.valueOf("TP"), modelId);
        pilotToDeactivate1      = ensurePilotExists("pilot-077-b", "US077-P02", IATACode.valueOf("TP"), modelId);
        pilotToDeactivate2      = ensurePilotExists("pilot-077-c", "US077-P03", IATACode.valueOf("TP"), modelId);
        pilotForAlreadyInactiveTest = ensurePilotExists("pilot-077-d", "US077-P04", IATACode.valueOf("TP"), modelId);
        pilotWithFlightPlan     = ensurePilotExists("pilot-077-e", "US077-P05", IATACode.valueOf("TP"), modelId);
        otherCompanyPilot       = ensurePilotExists("pilot-077-f", "US077-P06", IATACode.valueOf("RY"), modelId);

        ensureFlightPlanExists(pilotWithFlightPlan.identity());
    }

    @AfterEach
    void tearDown() {
        AuthenticationContext.clear();
    }

    // -----------------------------------------------------------------------
    // Authorization — no session
    // -----------------------------------------------------------------------

    @Test
    void ensureAllActivePilotsThrowsWhenNotAuthenticated() {
        assertThrows(UnauthenticatedException.class,
                () -> controller.allActivePilotsOfCompany());
    }

    @Test
    void ensureDeactivatePilotThrowsWhenNotAuthenticated() {
        assertThrows(UnauthenticatedException.class,
                () -> controller.deactivatePilot(pilotActive.identity()));
    }

    // -----------------------------------------------------------------------
    // Authorization — wrong role
    // -----------------------------------------------------------------------

    @Test
    void ensureAllActivePilotsThrowsWhenWrongRole() {
        AuthenticationContext.authenticate(OPERATOR_USERNAME, OPERATOR_PASSWORD);
        assertThrows(UnauthorizedException.class,
                () -> controller.allActivePilotsOfCompany());
    }

    @Test
    void ensureDeactivatePilotThrowsWhenWrongRole() {
        AuthenticationContext.authenticate(OPERATOR_USERNAME, OPERATOR_PASSWORD);
        assertThrows(UnauthorizedException.class,
                () -> controller.deactivatePilot(pilotActive.identity()));
    }

    // -----------------------------------------------------------------------
    // allActivePilotsOfCompany()
    // -----------------------------------------------------------------------

    @Test
    void ensureAllActivePilotsReturnsOnlyActivePilots() {
        AuthenticationContext.authenticate(ATCC_USERNAME, ATCC_PASSWORD);
        final List<Pilot> result = toList(controller.allActivePilotsOfCompany());
        assertTrue(result.stream().allMatch(Pilot::isActive));
        assertTrue(result.stream().anyMatch(p -> p.identity().equals(pilotActive.identity())));
    }

    @Test
    void ensureAllActivePilotsDoesNotReturnOtherCompanyPilots() {
        AuthenticationContext.authenticate(ATCC_USERNAME, ATCC_PASSWORD);
        final List<Pilot> result = toList(controller.allActivePilotsOfCompany());
        assertTrue(result.stream().noneMatch(
                p -> p.identity().equals(otherCompanyPilot.identity())));
    }

    // -----------------------------------------------------------------------
    // deactivatePilot() — happy path
    // -----------------------------------------------------------------------

    @Test
    void ensureDeactivatePilotSetsActiveToFalse() {
        AuthenticationContext.authenticate(ATCC_USERNAME, ATCC_PASSWORD);
        final Pilot result = controller.deactivatePilot(pilotToDeactivate1.identity());
        assertFalse(result.isActive());
    }

    @Test
    void ensureDeactivatePilotIsPersisted() {
        AuthenticationContext.authenticate(ATCC_USERNAME, ATCC_PASSWORD);
        controller.deactivatePilot(pilotToDeactivate2.identity());
        final Optional<Pilot> found = PersistenceContext.repositories().pilots()
                .ofIdentity(pilotToDeactivate2.identity());
        assertTrue(found.isPresent());
        assertFalse(found.get().isActive());
    }

    // -----------------------------------------------------------------------
    // deactivatePilot() — validation
    // -----------------------------------------------------------------------

    @Test
    void ensureDeactivatePilotThrowsWhenAlreadyInactive() {
        AuthenticationContext.authenticate(ATCC_USERNAME, ATCC_PASSWORD);
        controller.deactivatePilot(pilotForAlreadyInactiveTest.identity());
        assertThrows(IllegalStateException.class,
                () -> controller.deactivatePilot(pilotForAlreadyInactiveTest.identity()));
    }

    @Test
    void ensureDeactivatePilotThrowsWhenPilotNotFound() {
        AuthenticationContext.authenticate(ATCC_USERNAME, ATCC_PASSWORD);
        assertThrows(IllegalArgumentException.class,
                () -> controller.deactivatePilot(999999L));
    }

    @Test
    void ensureDeactivatePilotThrowsWhenPilotBelongsToOtherCompany() {
        AuthenticationContext.authenticate(ATCC_USERNAME, ATCC_PASSWORD);
        assertThrows(IllegalArgumentException.class,
                () -> controller.deactivatePilot(otherCompanyPilot.identity()));
    }

    @Test
    void ensureDeactivatePilotThrowsWhenFlightPlansAssigned() {
        AuthenticationContext.authenticate(ATCC_USERNAME, ATCC_PASSWORD);
        assertThrows(IllegalArgumentException.class,
                () -> controller.deactivatePilot(pilotWithFlightPlan.identity()));
    }

    // -----------------------------------------------------------------------
    // Bootstrap helpers
    // -----------------------------------------------------------------------

    private static void ensureSystemUserExists(final String username, final String password,
                                               final eapli.framework.infrastructure.authz.domain.model.Role role) {
        final var repo = PersistenceContext.repositories().systemUsers();
        if (repo.ofIdentity(Username.valueOf(username)).isPresent()) return;
        final var builder = new SystemUserBuilder(new AiSafePasswordPolicy(), new PlainTextEncoder());
        builder.withUsername(username).withPassword(password)
                .withName("Test", "User")
                .withEmail(username + "@aisafe.com")
                .withRoles(role);
        repo.save(builder.build());
    }

    private static void ensureCompanyExists(final String iata, final String name, final String icao) {
        final var repo = PersistenceContext.repositories().airTransportCompanies();
        if (repo.ofIdentity(IATACode.valueOf(iata)).isPresent()) return;
        repo.save(new AirTransportCompany(name, IATACode.valueOf(iata), ICAOCode.valueOf(icao)));
    }

    private static void ensureAtccUserAndCollaboratorExist() {
        final var systemUserRepo = PersistenceContext.repositories().systemUsers();
        final SystemUser sysUser = systemUserRepo
                .ofIdentity(Username.valueOf(ATCC_USERNAME)).orElseThrow();

        final var collaboratorRepo = PersistenceContext.repositories().collaborators();
        if (collaboratorRepo.findBySystemUser(sysUser).isPresent()) return;

        final var userRepo = PersistenceContext.repositories().users();
        User atccUser = userRepo.findByUsername(Username.valueOf(ATCC_USERNAME)).orElse(null);
        if (atccUser == null) {
            atccUser = userRepo.save(new User(sysUser,
                    MecanographicNumber.valueOf("US077-001"), "910000077",
                    new Email(ATCC_USERNAME + "@aisafe.com"), "ATC Controller",
                    new SecurityClearance(SecurityLevel.HIGH, LocalDate.now().plusYears(2)),
                    LocalDate.now()));
        }
        collaboratorRepo.save(new Collaborator(atccUser, IATACode.valueOf("TP")));
    }

    private static Long ensureAircraftModelExists() {
        final var repo = PersistenceContext.repositories().aircraftModels();
        final var all = toList(repo.findAll());
        if (!all.isEmpty()) return all.get(0).identity();
        final AircraftModel model = repo.save(new AircraftModel(
                "737-800", MakerName.valueOf("Boeing"), AircraftType.PASSENGER,
                41140, 79016, 62732, 20894, 12500, 230, 34.3, 125.0, 0.026, 1.5, 5765.0,
                new EngineModel("CFM56", MakerName.valueOf("CFM International"),
                        EngineType.TURBOFAN, 120.0, 115.0, 0.35)));
        return model.identity();
    }

    private static Pilot ensurePilotExists(final String username, final String mecNumber,
                                           final IATACode company, final Long modelId) {
        final var systemUserRepo = PersistenceContext.repositories().systemUsers();
        SystemUser sysUser = systemUserRepo.ofIdentity(Username.valueOf(username)).orElse(null);
        if (sysUser == null) {
            final var builder = new SystemUserBuilder(new AiSafePasswordPolicy(), new PlainTextEncoder());
            builder.withUsername(username).withPassword("Password1")
                    .withName("Pilot", "Test")
                    .withEmail(username + "@aisafe.com")
                    .withRoles(AiSafeRoles.PILOT);
            sysUser = systemUserRepo.save(builder.build());
        }

        final var userRepo = PersistenceContext.repositories().users();
        User user = userRepo.findByUsername(Username.valueOf(username)).orElse(null);
        if (user == null) {
            user = userRepo.save(new User(sysUser,
                    MecanographicNumber.valueOf(mecNumber), "910000000",
                    new Email(username + "@aisafe.com"), "Pilot",
                    new SecurityClearance(SecurityLevel.LOW, LocalDate.now().plusYears(1)),
                    LocalDate.now()));
        }

        final var pilotRepo = PersistenceContext.repositories().pilots();
        final Optional<Pilot> existing = pilotRepo.findBySystemUser(sysUser);
        if (existing.isPresent()) return existing.get();
        return pilotRepo.save(new Pilot(user, company, Set.of(modelId)));
    }

    private static void ensureFlightPlanExists(final Long pilotId) {
        final var repo = PersistenceContext.repositories().flightPlans();
        if (repo.ofIdentity(FlightPlanDesignator.valueOf("TP9001")).isPresent()) return;
        repo.save(new FlightPlan(
                FlightPlanDesignator.valueOf("TP9001"),
                FlightType.REGULAR,
                new RouteName("TP01"),
                RegistrationNumber.valueOf("CS-TUA"),
                pilotId,
                LocalDateTime.now().plusDays(1),
                FuelQuantity.valueOf(5000.0)));
    }

    private static <T> List<T> toList(final Iterable<T> iterable) {
        final List<T> list = new ArrayList<>();
        iterable.forEach(list::add);
        return list;
    }
}
