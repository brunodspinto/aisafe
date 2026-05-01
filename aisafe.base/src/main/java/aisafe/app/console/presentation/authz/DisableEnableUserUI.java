package aisafe.app.console.presentation.authz;

import aisafe.usermanagement.application.DisableEnableUserController;
import aisafe.usermanagement.domain.User;
import eapli.framework.infrastructure.authz.domain.model.Username;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class DisableEnableUserUI extends AbstractUI {

    private final DisableEnableUserController controller = new DisableEnableUserController();

    @Override
    protected boolean doShow() {
        final Iterable<User> users = controller.allUsers();
        System.out.printf("%-20s %-15s %-15s %-10s%n", "Username", "First Name", "Last Name", "Status");
        System.out.println("-".repeat(65));
        boolean any = false;
        for (final User u : users) {
            any = true;
            System.out.printf("%-20s %-15s %-15s %-10s%n",
                    u.systemUser().identity(),
                    u.systemUser().name().firstName(),
                    u.systemUser().name().lastName(),
                    u.systemUser().isActive() ? "ACTIVE" : "DISABLED");
        }
        if (!any) {
            System.out.println("No users found.");
            return false;
        }

        final String input = Console.readLine("\nUsername to toggle (Enter to cancel)").trim();
        if (input.isEmpty()) return false;

        try {
            final boolean nowActive = controller.toggleUser(Username.valueOf(input));
            System.out.println("User '" + input + "' is now " + (nowActive ? "ACTIVE" : "DISABLED") + ".");
        } catch (final IllegalArgumentException | IllegalStateException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return false;
    }

    @Override
    public String headline() {
        return "Disable/Enable User";
    }
}
