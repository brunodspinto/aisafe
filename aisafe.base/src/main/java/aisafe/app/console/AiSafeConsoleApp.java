package aisafe.app.console;

import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafePasswordPolicy;
import aisafe.usermanagement.domain.AiSafeRoles;
import aisafe.app.console.presentation.PreLoginMenu;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;
import eapli.framework.infrastructure.authz.domain.model.Username;

public final class AiSafeConsoleApp {

    private AiSafeConsoleApp() {}

    public static void main(final String[] args) {
        AuthzRegistry.configure(
                PersistenceContext.repositories().systemUsers(),
                new AiSafePasswordPolicy(),
                new PlainTextEncoder());

        bootstrapAdminIfNeeded();

        System.out.println("=====================================");
        System.out.println("      AISafe Backoffice Console      ");
        System.out.println("=====================================");

        new PreLoginMenu().mainLoop();
    }

    private static void bootstrapAdminIfNeeded() {
        AiSafeBootstrap.runBootstrap();
    }
}
