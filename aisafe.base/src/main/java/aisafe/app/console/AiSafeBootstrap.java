package aisafe.app.console;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.aircontrolarea.domain.AirControlAreaCode;
import aisafe.aircontrolarea.domain.GeoBoundary;
import aisafe.aircraft.domain.RegistrationNumber;
import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.airtransportcompany.domain.IATACode;
import aisafe.airtransportcompany.domain.ICAOCode;
import aisafe.maker.domain.MakerName;
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
        bootstrapAircraftModels();
        bootstrapAirTransportCompanies();
        bootstrapCollaborators();
        bootstrapAtccUser();
        bootstrapAircrafts();


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

        if (areaRepo.ofIdentity(AirControlAreaCode.valueOf(defaultAreaCode)).isEmpty()) {
            final GeoBoundary boundaries = new GeoBoundary(42.15, 36.95, -6.18, -9.50);
            final AirControlArea area = new AirControlArea(
                    AirControlAreaCode.valueOf(defaultAreaCode),
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
        final var checkRepo = PersistenceContext.repositories().systemUsers();
        final String username = "fco1";

        if (checkRepo.ofIdentity(
                        eapli.framework.infrastructure.authz.domain.model.Username.valueOf(username))
                .isEmpty()) {

            final var tx = PersistenceContext.repositories().newTransactionalContext();
            final var systemUserRepo = PersistenceContext.repositories().systemUsers(tx);
            final var userRepo = PersistenceContext.repositories().users(tx);
            final var collaboratorRepo = PersistenceContext.repositories().collaborators(tx);
            final var areaRepo = PersistenceContext.repositories().airControlAreas(tx);

            tx.beginTransaction();

            final var builder = new SystemUserBuilder(
                    new AiSafePasswordPolicy(), new PlainTextEncoder());
            builder.withUsername(username)
                    .withPassword("Password1")
                    .withName("Flight", "Controller")
                    .withEmail("fco1@aisafe.com")
                    .withRoles(AiSafeRoles.FLIGHT_CONTROL_OPERATOR);
            final var systemUser = builder.build();
            final var savedSystemUser = systemUserRepo.save(systemUser);

            final var user = new aisafe.usermanagement.domain.User(
                    savedSystemUser,
                    aisafe.usermanagement.domain.MecanographicNumber.valueOf("FCO001"),
                    "910000001",
                    new aisafe.usermanagement.domain.Email("fco1@aisafe.com"),
                    "Flight Controller",
                    new aisafe.usermanagement.domain.SecurityClearance(
                            aisafe.usermanagement.domain.SecurityLevel.HIGH,
                            java.time.LocalDate.of(2030, 1, 1)),
                    java.time.LocalDate.of(2025, 1, 1));
            final var savedUser = userRepo.save(user);

            areaRepo.ofIdentity(AirControlAreaCode.valueOf("PT-N")).ifPresent(area -> {
                collaboratorRepo.save(
                        new aisafe.collaborator.domain.Collaborator(savedUser, area));
                System.out.println("Collaborator created: " + username + " for area PT-N");
            });

            tx.commit();
        } else {
            System.out.println("Collaborator already exists: " + username);
        }
    }

    private static void bootstrapAirTransportCompanies() {
        final var repo = PersistenceContext.repositories().airTransportCompanies();

        bootstrapAirTransportCompany(repo, "TAP Air Portugal", "TP", "TAP");
        bootstrapAirTransportCompany(repo, "Ryanair", "FR", "RYR");
        bootstrapAirTransportCompany(repo, "Lufthansa", "LH", "DLH");
    }

    private static void bootstrapAirTransportCompany(
            final aisafe.airtransportcompany.repositories.AirTransportCompanyRepository repo,
            final String name, final String iata, final String icao) {
        final IATACode iataCode = IATACode.valueOf(iata);
        if (repo.ofIdentity(iataCode).isEmpty()) {
            repo.save(new AirTransportCompany(name, iataCode, ICAOCode.valueOf(icao)));
            System.out.println("Air Transport Company created: " + name + " (" + iata + " / " + icao + ")");
        } else {
            System.out.println("Air Transport Company already exists: " + iata);
        }
    }

    private static void bootstrapAirports() {
        final var airportRepo = PersistenceContext.repositories().airports();
        final var areaRepo = PersistenceContext.repositories().airControlAreas();

        final aisafe.airport.domain.AirportIATACode lisCode =
                aisafe.airport.domain.AirportIATACode.valueOf("LIS");

        if (airportRepo.ofIdentity(lisCode).isEmpty()) {
            areaRepo.ofIdentity(AirControlAreaCode.valueOf("PT-N")).ifPresent(area -> {
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
            areaRepo.ofIdentity(AirControlAreaCode.valueOf("PT-N")).ifPresent(area -> {
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

    private static void bootstrapAtccUser() {
        final var systemUserRepo = PersistenceContext.repositories().systemUsers();
        final var userRepo = PersistenceContext.repositories().users();
        final var collaboratorRepo = PersistenceContext.repositories().collaborators();
        final var companyRepo = PersistenceContext.repositories().airTransportCompanies();

        final String username = "atcc1";

        if (systemUserRepo.ofIdentity(
                eapli.framework.infrastructure.authz.domain.model.Username.valueOf(username)).isEmpty()) {

            final var builder = new SystemUserBuilder(new AiSafePasswordPolicy(), new PlainTextEncoder());
            builder.withUsername(username)
                    .withPassword("Password1")
                    .withName("Air", "Transport")
                    .withEmail("atcc1@aisafe.com")
                    .withRoles(AiSafeRoles.ATCC);
            final var systemUser = builder.build();
            systemUserRepo.save(systemUser);

            final var user = new aisafe.usermanagement.domain.User(
                    systemUser,
                    aisafe.usermanagement.domain.MecanographicNumber.valueOf("ATC001"),
                    "920000001",
                    new aisafe.usermanagement.domain.Email("atcc1@aisafe.com"),
                    "Air Transport Collaborator",
                    new aisafe.usermanagement.domain.SecurityClearance(
                            aisafe.usermanagement.domain.SecurityLevel.GUARDED,
                            java.time.LocalDate.of(2030, 1, 1)),
                    java.time.LocalDate.of(2025, 1, 1));
            userRepo.save(user);

            companyRepo.ofIdentity(IATACode.valueOf("TP")).ifPresent(company -> {
                collaboratorRepo.save(new aisafe.collaborator.domain.Collaborator(user, company));
                System.out.println("ATCC collaborator created: " + username + " for company TAP");
            });
        } else {
            System.out.println("ATCC collaborator already exists: " + username);
        }
    }

    private static void bootstrapAircrafts() {
        final var aircraftRepo = PersistenceContext.repositories().aircraft();
        final var companyRepo = PersistenceContext.repositories().airTransportCompanies();
        final var modelRepo = PersistenceContext.repositories().aircraftModels();

        final String registration = "CS-TUA";

        if (aircraftRepo.ofIdentity(RegistrationNumber.valueOf(registration)).isEmpty()) {
            final var models = modelRepo.findAll();
            if (!models.iterator().hasNext()) {
                System.out.println("No aircraft models found — skipping aircraft bootstrap.");
                return;
            }
            final aisafe.aircraftmodel.domain.AircraftModel model = models.iterator().next();

            companyRepo.ofIdentity(IATACode.valueOf("TP")).ifPresent(company -> {
                final aisafe.aircraft.domain.CabinConfiguration cabin =
                        new aisafe.aircraft.domain.CabinConfiguration(8, 20, 150);
                final aisafe.aircraft.domain.Aircraft aircraft =
                        new aisafe.aircraft.domain.Aircraft(RegistrationNumber.valueOf(registration), "Portugal", 6, 2018, cabin, model);
                aircraftRepo.save(aircraft);
                company.addAircraftToFleet(aircraft);
                companyRepo.save(company);
                System.out.println("Aircraft bootstrapped: " + registration + " added to TAP fleet.");
            });
        } else {
            System.out.println("Aircraft already exists: " + registration);
        }
    }

        private static void bootstrapMakers() {
            final var makerRepo = PersistenceContext.repositories().makers();

            if (makerRepo.ofIdentity(MakerName.valueOf("Boeing")).isEmpty()) {
                makerRepo.save(new aisafe.maker.domain.Maker(MakerName.valueOf("Boeing"), "USA"));
                System.out.println("Maker created: Boeing");
            } else {
                System.out.println("Maker already exists: Boeing");
            }

            if (makerRepo.ofIdentity(MakerName.valueOf("Airbus")).isEmpty()) {
                makerRepo.save(new aisafe.maker.domain.Maker(MakerName.valueOf("Airbus"), "France"));
                System.out.println("Maker created: Airbus");
            } else {
                System.out.println("Maker already exists: Airbus");
            }
        }

    private static void bootstrapAircraftModels() {
        final var aircraftModelRepo = PersistenceContext.repositories().aircraftModels();
        final var makerRepo = PersistenceContext.repositories().makers();
        final var engineRepo = PersistenceContext.repositories().engineModels();

        makerRepo.ofIdentity(MakerName.valueOf("Boeing")).ifPresent(boeing -> {
            engineRepo.findByNameAndMaker("CFM56", "CFM International").ifPresent(engine -> {
                if (aircraftModelRepo.findByModelNameAndMaker("737-800", boeing).isEmpty()) {
                    final var model = new aisafe.aircraftmodel.domain.AircraftModel(
                            "737-800", boeing, aisafe.aircraftmodel.domain.AircraftType.PASSENGER,
                            41140, 79016, 62732, 20894,
                            12500, 230, 34.3, 125.0,
                            0.026, 1.5, 5765.0, engine);
                    aircraftModelRepo.save(model);
                    System.out.println("Aircraft model created: 737-800 by Boeing");
                } else {
                    System.out.println("Aircraft model already exists: 737-800 by Boeing");
                }
            });
        });
    }

    public static void runBootstrap() {
        bootstrapAdmin();
        bootstrapWeatherPerson();
        bootstrapAirControlAreas();
        bootstrapEngineModels();
        bootstrapAirports();
        bootstrapMakers();
        bootstrapAircraftModels();
        bootstrapAirTransportCompanies();
        bootstrapCollaborators();
        bootstrapAtccUser();
        bootstrapAircrafts();
    }

}
