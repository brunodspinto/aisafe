package aisafe.app.console.presentation.authz;

import aisafe.auth.AuthenticationContext;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

/**
 * Console UI for user logout.
 * Clears the current session and offers the user the option to log in again or exit.
 */
public class LogoutUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        AuthenticationContext.clear();
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
