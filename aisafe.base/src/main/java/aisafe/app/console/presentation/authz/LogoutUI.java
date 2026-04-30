package aisafe.app.console.presentation.authz;

import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class LogoutUI extends AbstractUI {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();

    @Override
    protected boolean doShow() {
        authz.clearSession();
        System.out.println("\nLogged out successfully.");
        System.out.println("\n1 - Login again");
        System.out.println("0 - Exit");
        final String choice = Console.readLine("Option").trim();

        if ("1".equals(choice)) {
            if (new LoginUI().show()) {
                return true;
            }
        }

        System.out.println("\nGoodbye!");
        System.exit(0);
        return false;
    }

    @Override
    public String headline() {
        return "Logout";
    }
}
