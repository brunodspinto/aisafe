package aisafe.flightroute.application;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.aircontrolarea.domain.AirControlAreaCode;
import aisafe.aircontrolarea.domain.GeoBoundary;
import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.airtransportcompany.domain.IATACode;
import aisafe.airtransportcompany.domain.ICAOCode;
import aisafe.airport.domain.Airport;
import aisafe.airport.domain.AirportIATACode;
import aisafe.airport.domain.AirportICAOCode;
import aisafe.airport.domain.GeoCoordinate;
import aisafe.auth.AuthenticationContext;
import aisafe.collaborator.domain.Collaborator;
import aisafe.flightroute.domain.FlightRoute;
import aisafe.flightroute.domain.RouteName;
import aisafe.infrastructure.persistence.PersistenceContext;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link CreateFlightRouteController} (US073).
 * Uses in-memory repositories and real authentication infrastructure.
 */
class CreateFlightRouteControllerTest {

    private static final String ATCC_USERNAME = "atcc-us073";
    private static final String ATCC_PASSWORD = "Password1";
    private static final String OPERATOR_USERNAME = "bo-op-us073";
    private static final String OPERATOR_PASSWORD = "Password1";

    private final CreateFlightRouteController controller = new CreateFlightRouteController();

    @BeforeAll
    static void bootstrapAll() {
        AuthzRegistry.configure(
                PersistenceContext.repositories().systemUsers(),
                new AiSafePasswordPolicy(),
                new PlainTextEncoder());

        ensureSystemUserExists(ATCC_USERNAME, ATCC_PASSWORD, AiSafeRoles.ATCC);
        ensureSystemUserExists(OPERATOR_USERNAME, OPERATOR_PASSWORD, AiSafeRoles.BACKOFFICE_OPERATOR);

        ensureCompanyExists();
        ensureAtccUserAndCollaboratorExist();
        ensureAirportsExist();
    }

    @AfterEach
    void tearDown() {
        AuthenticationContext.clear();
    }

    // -----------------------------------------------------------------------
    // Authorization — no session
    // -----------------------------------------------------------------------

    @Test
    void ensureAllAirportsThrowsWhenNotAuthenticated() {
        assertThrows(UnauthenticatedException.class, () -> controller.allAirports());
    }

    @Test
    void ensureCreateFlightRouteThrowsWhenNotAuthenticated() {
        assertThrows(UnauthenticatedException.class,
                () -> controller.createFlightRoute("TP1", "OPO", "LIS"));
    }

    // -----------------------------------------------------------------------
    // Authorization — wrong role
    // -----------------------------------------------------------------------

    @Test
    void ensureAllAirportsThrowsWhenWrongRole() {
        AuthenticationContext.authenticate(OPERATOR_USERNAME, OPERATOR_PASSWORD);
        assertThrows(UnauthorizedException.class, () -> controller.allAirports());
    }

    @Test
    void ensureCreateFlightRouteThrowsWhenWrongRole() {
        AuthenticationContext.authenticate(OPERATOR_USERNAME, OPERATOR_PASSWORD);
        assertThrows(UnauthorizedException.class,
                () -> controller.createFlightRoute("TP1", "OPO", "LIS"));
    }

    // -----------------------------------------------------------------------
    // allAirports()
    // -----------------------------------------------------------------------

    @Test
    void ensureAllAirportsReturnsAtLeastTheSeededAirports() {
        AuthenticationContext.authenticate(ATCC_USERNAME, ATCC_PASSWORD);
        final List<Airport> airports = toList(controller.allAirports());
        final List<String> codes = airports.stream()
                .map(a -> a.identity().code())
                .toList();
        assertTrue(airports.size() >= 2);
        assertTrue(codes.contains("OPO"));
        assertTrue(codes.contains("LIS"));
    }

    // -----------------------------------------------------------------------
    // createFlightRoute() — happy path
    // -----------------------------------------------------------------------

    @Test
    void ensureCreateFlightRouteReturnsPersistedRoute() {
        AuthenticationContext.authenticate(ATCC_USERNAME, ATCC_PASSWORD);
        final FlightRoute route = controller.createFlightRoute("TP1", "OPO", "LIS");
        assertNotNull(route);
        assertEquals("TP1", route.identity().toString());
        assertEquals(IATACode.valueOf("TP"), route.companyIataCode());
        assertEquals(AirportIATACode.valueOf("OPO"), route.originAirport());
        assertEquals(AirportIATACode.valueOf("LIS"), route.destinationAirport());
        assertTrue(route.isActive());
    }

    @Test
    void ensureCreatedRouteIsPersisted() {
        AuthenticationContext.authenticate(ATCC_USERNAME, ATCC_PASSWORD);
        controller.createFlightRoute("TP2", "LIS", "OPO");
        assertTrue(PersistenceContext.repositories().flightRoutes()
                .ofIdentity(new RouteName("TP2")).isPresent());
    }

    @Test
    void ensureCreateFlightRouteWithWhitespacePaddedInputsSucceeds() {
        AuthenticationContext.authenticate(ATCC_USERNAME, ATCC_PASSWORD);
        final FlightRoute route = controller.createFlightRoute("  tp3  ", "  opo  ", "  lis  ");
        assertEquals("TP3", route.identity().toString());
    }

    @Test
    void ensureRouteIsAssociatedWithAuthenticatedCollaboratorsCompany() {
        AuthenticationContext.authenticate(ATCC_USERNAME, ATCC_PASSWORD);
        final FlightRoute route = controller.createFlightRoute("TP5", "OPO", "LIS");
        assertEquals(IATACode.valueOf("TP"), route.companyIataCode());
    }

    @Test
    void ensureMultipleRoutesCanBeCreatedForSameCompany() {
        AuthenticationContext.authenticate(ATCC_USERNAME, ATCC_PASSWORD);
        controller.createFlightRoute("TP6", "OPO", "LIS");
        controller.createFlightRoute("TP7", "LIS", "OPO");
        assertTrue(PersistenceContext.repositories().flightRoutes()
                .ofIdentity(new RouteName("TP6")).isPresent());
        assertTrue(PersistenceContext.repositories().flightRoutes()
                .ofIdentity(new RouteName("TP7")).isPresent());
    }

    // -----------------------------------------------------------------------
    // createFlightRoute() — validation
    // -----------------------------------------------------------------------

    @Test
    void ensureCreateFlightRouteThrowsForDuplicateRouteName() {
        AuthenticationContext.authenticate(ATCC_USERNAME, ATCC_PASSWORD);
        controller.createFlightRoute("TP4", "OPO", "LIS");
        assertThrows(IllegalArgumentException.class,
                () -> controller.createFlightRoute("TP4", "OPO", "LIS"));
    }

    @Test
    void ensureCreateFlightRouteThrowsForUnknownOriginAirport() {
        AuthenticationContext.authenticate(ATCC_USERNAME, ATCC_PASSWORD);
        assertThrows(IllegalArgumentException.class,
                () -> controller.createFlightRoute("TP8", "XXX", "LIS"));
    }

    @Test
    void ensureCreateFlightRouteThrowsForUnknownDestinationAirport() {
        AuthenticationContext.authenticate(ATCC_USERNAME, ATCC_PASSWORD);
        assertThrows(IllegalArgumentException.class,
                () -> controller.createFlightRoute("TP9", "OPO", "YYY"));
    }

    @Test
    void ensureCreateFlightRouteThrowsForSameOriginAndDestination() {
        AuthenticationContext.authenticate(ATCC_USERNAME, ATCC_PASSWORD);
        assertThrows(IllegalArgumentException.class,
                () -> controller.createFlightRoute("TP10", "OPO", "OPO"));
    }

    @Test
    void ensureCreateFlightRouteThrowsForInvalidRouteName() {
        AuthenticationContext.authenticate(ATCC_USERNAME, ATCC_PASSWORD);
        assertThrows(IllegalArgumentException.class,
                () -> controller.createFlightRoute("INVALID", "OPO", "LIS"));
    }

    @Test
    void ensureCreateFlightRouteThrowsWhenPrefixDoesNotMatchCompanyIATA() {
        AuthenticationContext.authenticate(ATCC_USERNAME, ATCC_PASSWORD);
        assertThrows(IllegalArgumentException.class,
                () -> controller.createFlightRoute("XY1", "OPO", "LIS"));
    }

    // -----------------------------------------------------------------------
    // Bootstrap helpers
    // -----------------------------------------------------------------------

    private static void ensureSystemUserExists(final String username, final String password,
                                               final eapli.framework.infrastructure.authz.domain.model.Role role) {
        final var systemUserRepo = PersistenceContext.repositories().systemUsers();
        if (systemUserRepo.ofIdentity(Username.valueOf(username)).isPresent()) return;
        final var builder = new SystemUserBuilder(new AiSafePasswordPolicy(), new PlainTextEncoder());
        builder.withUsername(username)
                .withPassword(password)
                .withName("Test", "User")
                .withEmail(username + "@aisafe.com")
                .withRoles(role);
        systemUserRepo.save(builder.build());
    }

    private static void ensureCompanyExists() {
        final var companyRepo = PersistenceContext.repositories().airTransportCompanies();
        if (companyRepo.ofIdentity(IATACode.valueOf("TP")).isPresent()) return;
        companyRepo.save(new AirTransportCompany("TAP Air Portugal",
                IATACode.valueOf("TP"), ICAOCode.valueOf("TAP")));
    }

    private static void ensureAtccUserAndCollaboratorExist() {
        final var systemUserRepo = PersistenceContext.repositories().systemUsers();
        final SystemUser systemUser = systemUserRepo
                .ofIdentity(Username.valueOf(ATCC_USERNAME)).orElseThrow();

        final var collaboratorRepo = PersistenceContext.repositories().collaborators();
        if (collaboratorRepo.findBySystemUser(systemUser).isPresent()) return;

        final var userRepo = PersistenceContext.repositories().users();
        User atccUser = userRepo.findByUsername(Username.valueOf(ATCC_USERNAME)).orElse(null);
        if (atccUser == null) {
            atccUser = new User(
                    systemUser,
                    MecanographicNumber.valueOf("US073-001"),
                    "910000073",
                    new Email(ATCC_USERNAME + "@aisafe.com"),
                    "ATC Controller",
                    new SecurityClearance(SecurityLevel.HIGH, LocalDate.now().plusYears(2)),
                    LocalDate.now());
            userRepo.save(atccUser);
        }

        collaboratorRepo.save(new Collaborator(atccUser, IATACode.valueOf("TP")));
    }

    private static void ensureAirportsExist() {
        final var airportRepo = PersistenceContext.repositories().airports();
        if (airportRepo.ofIdentity(AirportIATACode.valueOf("OPO")).isPresent()
                && airportRepo.ofIdentity(AirportIATACode.valueOf("LIS")).isPresent()) return;

        final var areaRepo = PersistenceContext.repositories().airControlAreas();
        AirControlArea area = areaRepo.ofIdentity(AirControlAreaCode.valueOf("PT-N")).orElse(null);
        if (area == null) {
            area = new AirControlArea(AirControlAreaCode.valueOf("PT-N"),
                    "Northern Portugal", 1200.0,
                    new GeoBoundary(42.5, 36.5, -6.0, -10.0));
            areaRepo.save(area);
        }

        if (airportRepo.ofIdentity(AirportIATACode.valueOf("OPO")).isEmpty()) {
            airportRepo.save(new Airport(
                    new AirportIATACode("OPO"), new AirportICAOCode("LPPR"),
                    "Francisco Sa Carneiro", "Porto", "Portugal",
                    new GeoCoordinate(41.2481, -8.6814), 69.0, area));
        }

        if (airportRepo.ofIdentity(AirportIATACode.valueOf("LIS")).isEmpty()) {
            airportRepo.save(new Airport(
                    new AirportIATACode("LIS"), new AirportICAOCode("LPPT"),
                    "Humberto Delgado", "Lisbon", "Portugal",
                    new GeoCoordinate(38.7756, -9.1354), 113.0, area));
        }
    }

    private static <T> List<T> toList(final Iterable<T> iterable) {
        final List<T> list = new ArrayList<>();
        iterable.forEach(list::add);
        return list;
    }
}
