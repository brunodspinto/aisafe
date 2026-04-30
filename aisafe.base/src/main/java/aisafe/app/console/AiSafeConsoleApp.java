package aisafe.app.console;

import aisafe.app.console.presentation.MainMenu;
import aisafe.app.console.presentation.authz.LoginUI;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafePasswordPolicy;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;

public final class AiSafeConsoleApp {

    private AiSafeConsoleApp() {}

    public static void main(final String[] args) {
        AuthzRegistry.configure(
                PersistenceContext.repositories().systemUsers(),
                new AiSafePasswordPolicy(),
                new PlainTextEncoder());

        System.out.println("=====================================");
        System.out.println("      AISafe Backoffice Console      ");
        System.out.println("=====================================");

        if (new LoginUI().show()) {
            new MainMenu().mainLoop();
        }

        System.out.println("\nGoodbye!");
        System.exit(0);
    }
}
