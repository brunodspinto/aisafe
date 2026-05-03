package aisafe.app.console;

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
}
