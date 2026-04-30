package aisafe.app.console.presentation.authz;

import aisafe.usermanagement.application.ListUsersController;
import aisafe.usermanagement.domain.User;
import eapli.framework.presentation.console.AbstractUI;

public class ListUsersUI extends AbstractUI {

    private final ListUsersController controller = new ListUsersController();

    @Override
    protected boolean doShow() {
        final Iterable<User> users = controller.allUsers();
        System.out.printf("%-20s %-15s %-15s %-30s %-20s%n",
                "Username", "First Name", "Last Name", "Email", "Position");
        System.out.println("-".repeat(100));
        boolean any = false;
        for (final User u : users) {
            any = true;
            System.out.printf("%-20s %-15s %-15s %-30s %-20s%n",
                    u.systemUser().identity(),
                    u.systemUser().name().firstName(),
                    u.systemUser().name().lastName(),
                    u.email().address(),
                    u.position());
        }
        if (!any) {
            System.out.println("No users found.");
        }
        return false;
    }

    @Override
    public String headline() {
        return "List Users";
    }
}
