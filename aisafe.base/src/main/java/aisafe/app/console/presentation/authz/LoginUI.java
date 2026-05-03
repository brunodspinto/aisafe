package aisafe.app.console.presentation.authz;

import aisafe.auth.AuthenticationContext;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class LoginUI extends AbstractUI {

    private static final int MAX_ATTEMPTS = 3;

    @Override
    protected boolean doShow() {
        var attempt = 1;
        while (attempt <= MAX_ATTEMPTS) {
            final String username = Console.readNonEmptyLine("Username:", "Please provide a username");
            final String password = Console.readLine("Password:");

            if (AuthenticationContext.authenticate(username, password)) {
                return true;
            }
            System.out.printf("Wrong username or password. You have %d attempt(s) left.%n%n", MAX_ATTEMPTS - attempt);
            attempt++;
        }
        System.out.println("Unable to authenticate. Please contact your administrator.");
        return false;
    }

    @Override
    public String headline() {
        return "Login";
    }
}
