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
}
