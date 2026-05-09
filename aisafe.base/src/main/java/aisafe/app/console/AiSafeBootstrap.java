package aisafe.app.console;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.aircontrolarea.domain.GeoBoundary;
import aisafe.enginemodel.domain.EngineModel;
import aisafe.enginemodel.domain.EngineType;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafePasswordPolicy;
import aisafe.usermanagement.domain.AiSafeRoles;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;

public final class AiSafeBootstrap {

    private AiSafeBootstrap() {}

    public static void main(final String[] args) {
        AuthzRegistry.configure(
                PersistenceContext.repositories().systemUsers(),
                new AiSafePasswordPolicy(),
                new PlainTextEncoder());

        System.out.println("=====================================");
        System.out.println("      AISafe Bootstrap               ");
        System.out.println("=====================================");

        bootstrapAdmin();
        bootstrapWeatherPerson();
        bootstrapAirControlAreas();
        bootstrapEngineModels();
        bootstrapAirports();
        bootstrapMakers();
        bootstrapCollaborators();

        System.out.println("Bootstrap completed successfully!");
    }

    private static void bootstrapAdmin() {
        final var userRepo = PersistenceContext.repositories().systemUsers();
        final var adminUsername = "admin";

        if (userRepo.ofIdentity(
                eapli.framework.infrastructure.authz.domain.model.Username.valueOf(adminUsername))
                .isEmpty()) {

            final var builder = new SystemUserBuilder(
                    new AiSafePasswordPolicy(), new PlainTextEncoder());
            builder.withUsername(adminUsername)
                    .withPassword("Password1")
                    .withName("System", "Admin")
                    .withEmail("admin@aisafe.com")
                    .withRoles(AiSafeRoles.ADMIN);

            userRepo.save(builder.build());
            System.out.println("Admin user created.");
        } else {
            System.out.println("Admin user already exists.");
        }
    }

    private static void bootstrapWeatherPerson() {
        final var userRepo = PersistenceContext.repositories().systemUsers();
        final var username = "weather_person";

        if (userRepo.ofIdentity(
                eapli.framework.infrastructure.authz.domain.model.Username.valueOf(username))
                .isEmpty()) {

            final var builder = new SystemUserBuilder(
                    new AiSafePasswordPolicy(), new PlainTextEncoder());
            builder.withUsername(username)
                    .withPassword("Password1")
                    .withName("Weather", "Person")
                    .withEmail("weather@aisafe.com")
                    .withRoles(AiSafeRoles.WEATHER_PERSON);

            userRepo.save(builder.build());
            System.out.println("Weather person user created.");
        } else {
            System.out.println("Weather person user already exists.");
        }
    }

    private static void bootstrapAirControlAreas() {
        final var areaRepo = PersistenceContext.repositories().airControlAreas();
        final String defaultAreaCode = "PT-N";

        if (areaRepo.ofIdentity(defaultAreaCode).isEmpty()) {
            final GeoBoundary boundaries = new GeoBoundary(42.15, 36.95, -6.18, -9.50);
            final AirControlArea area = new AirControlArea(
                    defaultAreaCode,
                    "Northern Portugal Control Area",
                    1200.0,
                    boundaries
            );
            areaRepo.save(area);
            System.out.println("Default air control area created: " + defaultAreaCode);
        } else {
            System.out.println("Default air control area already exists: " + defaultAreaCode);
        }
    }

    private static void bootstrapEngineModels() {
        final var engineRepo = PersistenceContext.repositories().engineModels();

        bootstrapEngineModel(engineRepo, "CFM56", "CFM International", EngineType.TURBOFAN, 120.0, 0.372);
        bootstrapEngineModel(engineRepo, "PW4000", "Pratt & Whitney", EngineType.TURBOFAN, 252.0, 0.330);
        bootstrapEngineModel(engineRepo, "PT6A-65B", "Pratt & Whitney Canada", EngineType.TURBOPROP, 17.0, 0.290);
    }

    private static void bootstrapEngineModel(
            final aisafe.enginemodel.repositories.EngineModelRepository repo,
            final String name, final String makerName, final EngineType engineType,
            final double thrust, final double tsfc) {
        if (repo.findByNameAndMaker(name, makerName).isEmpty()) {
            repo.save(new EngineModel(name, makerName, engineType, thrust, tsfc));
            System.out.println("Engine model created: " + name + " by " + makerName);
        } else {
            System.out.println("Engine model already exists: " + name + " by " + makerName);
        }
    }

    private static void bootstrapCollaborators() {
        final var systemUserRepo = PersistenceContext.repositories().systemUsers();
        final var userRepo = PersistenceContext.repositories().users();
        final var collaboratorRepo = PersistenceContext.repositories().collaborators();
        final var areaRepo = PersistenceContext.repositories().airControlAreas();

        final String username = "fco1";

        if (systemUserRepo.ofIdentity(
                        eapli.framework.infrastructure.authz.domain.model.Username.valueOf(username))
                .isEmpty()) {

            final var builder = new SystemUserBuilder(
                    new AiSafePasswordPolicy(), new PlainTextEncoder());
            builder.withUsername(username)
                    .withPassword("Password1")
                    .withName("Flight", "Controller")
                    .withEmail("fco1@aisafe.com")
                    .withRoles(AiSafeRoles.FLIGHT_CONTROL_OPERATOR);
            final var systemUser = builder.build();
            systemUserRepo.save(systemUser);

            final var user = new aisafe.usermanagement.domain.User(
                    systemUser,
                    aisafe.usermanagement.domain.MecanographicNumber.valueOf("FCO001"),
                    "910000001",
                    new aisafe.usermanagement.domain.Email("fco1@aisafe.com"),
                    "Flight Controller",
                    new aisafe.usermanagement.domain.SecurityClearance(
                            aisafe.usermanagement.domain.SecurityLevel.HIGH,
                            java.time.LocalDate.of(2030, 1, 1)),
                    java.time.LocalDate.of(2025, 1, 1));
            userRepo.save(user);

            areaRepo.ofIdentity("PT-N").ifPresent(area -> {
                collaboratorRepo.save(
                        new aisafe.collaborator.domain.Collaborator(user, area));
                System.out.println("Collaborator created: " + username + " for area PT-N");
            });
        } else {
            System.out.println("Collaborator already exists: " + username);
        }
    }

    private static void bootstrapAirports() {
        final var airportRepo = PersistenceContext.repositories().airports();
        final var areaRepo = PersistenceContext.repositories().airControlAreas();

        final aisafe.airport.domain.AirportIATACode lisCode =
                aisafe.airport.domain.AirportIATACode.valueOf("LIS");

        if (airportRepo.ofIdentity(lisCode).isEmpty()) {
            areaRepo.ofIdentity("PT-N").ifPresent(area -> {
                final aisafe.airport.domain.Airport airport = new aisafe.airport.domain.Airport(
                        lisCode,
                        aisafe.airport.domain.AirportICAOCode.valueOf("LPPT"),
                        "Humberto Delgado Airport",
                        "Lisbon",
                        "Portugal",
                        new aisafe.airport.domain.GeoCoordinate(38.7756, -9.1354),
                        113.0,
                        area
                );
                airportRepo.save(airport);
                System.out.println("Airport created: LIS");
            });
        } else {
            System.out.println("Airport already exists: LIS");
        }

        final aisafe.airport.domain.AirportIATACode opoCode =
                aisafe.airport.domain.AirportIATACode.valueOf("OPO");

        if (airportRepo.ofIdentity(opoCode).isEmpty()) {
            areaRepo.ofIdentity("PT-N").ifPresent(area -> {
                final aisafe.airport.domain.Airport airport = new aisafe.airport.domain.Airport(
                        opoCode,
                        aisafe.airport.domain.AirportICAOCode.valueOf("LPPR"),
                        "Francisco Sá Carneiro Airport",
                        "Porto",
                        "Portugal",
                        new aisafe.airport.domain.GeoCoordinate(41.2481, -8.6814),
                        69.0,
                        area
                );
                airportRepo.save(airport);
                System.out.println("Airport created: OPO");
            });
        } else {
            System.out.println("Airport already exists: OPO");
        }
    }

        private static void bootstrapMakers() {
            final var makerRepo = PersistenceContext.repositories().makers();

            if (makerRepo.ofIdentity("Boeing").isEmpty()) {
                makerRepo.save(new aisafe.maker.domain.Maker("Boeing", "USA"));
                System.out.println("Maker created: Boeing");
            } else {
                System.out.println("Maker already exists: Boeing");
            }

            if (makerRepo.ofIdentity("Airbus").isEmpty()) {
                makerRepo.save(new aisafe.maker.domain.Maker("Airbus", "France"));
                System.out.println("Maker created: Airbus");
            } else {
                System.out.println("Maker already exists: Airbus");
            }
        }

    }
