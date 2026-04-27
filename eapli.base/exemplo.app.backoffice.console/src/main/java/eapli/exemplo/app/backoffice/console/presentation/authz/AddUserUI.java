package eapli.exemplo.app.backoffice.console.presentation.authz;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import eapli.exemplo.usermanagement.application.AddUserController;
import eapli.exemplo.userbackoffice.domain.Email;
import eapli.exemplo.userbackoffice.domain.SecurityClearance;
import eapli.framework.actions.Actions;
import eapli.framework.actions.menu.Menu;
import eapli.framework.actions.menu.MenuItem;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.presentation.console.menu.MenuItemRenderer;
import eapli.framework.presentation.console.menu.MenuRenderer;
import eapli.framework.presentation.console.menu.VerticalMenuRenderer;

public class AddUserUI extends AbstractUI {

    private final AddUserController theController = new AddUserController();

    @Override
    protected boolean doShow() {
        final String username = Console.readLine("Username");
        final String password = Console.readLine("Password(Must have at least 6 characters, one digit and one capital letter)");
        final String firstName = Console.readLine("First Name");
        final String lastName = Console.readLine("Last Name");
        final String phoneNumber = Console.readLine("Phone Number");

        Email email = null;
        while (email == null) {
            try {
                final String emailStr = Console.readLine("E-Mail");
                email = new Email(emailStr);
            } catch (final IllegalArgumentException e) {
                System.out.println("Invalid email format. Please try again.");
            }
        }

        final String position = Console.readLine("Position");

        final String clearanceLevel = Console.readLine("Security Clearance Level").trim();
        final String expirationStr = Console.readLine("Security Clearance Expiration (YYYY-MM-DD)").trim();
        final LocalDate expirationDate = LocalDate.parse(expirationStr);
        final SecurityClearance securityClearance =
                new SecurityClearance(clearanceLevel, expirationDate);

        final String assessmentStr = Console.readLine("Skills Assessment Date (YYYY-MM-DD)").trim();
        final LocalDate skillsAssessmentDate = LocalDate.parse(assessmentStr);

        final Set<Role> roleTypes = new HashSet<>();
        boolean show;
        do {
            show = showRoles(roleTypes);
        } while (!show);

        try {
            this.theController.addUser(username, password, firstName, lastName,
                    email.address(), roleTypes, phoneNumber, position,
                    email, securityClearance, skillsAssessmentDate);
            System.out.println("User successfully registered.");
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            System.out.println("That username/email is already in use.");
        } catch (final IllegalArgumentException e) {
            System.out.println("Invalid data: " + e.getMessage());
        }

        return false;
    }

    private boolean showRoles(final Set<Role> roleTypes) {
        final Menu rolesMenu = buildRolesMenu(roleTypes);
        final MenuRenderer renderer =
                new VerticalMenuRenderer(rolesMenu, MenuItemRenderer.DEFAULT);
        return renderer.render();
    }

    private Menu buildRolesMenu(final Set<Role> roleTypes) {
        final Menu rolesMenu = new Menu();
        int counter = 0;
        rolesMenu.addItem(MenuItem.of(counter++, "No Role", Actions.SUCCESS));
        for (final Role roleType : theController.getRoleTypes()) {
            rolesMenu.addItem(
                    MenuItem.of(counter++, roleType.toString(),
                            () -> roleTypes.add(roleType)));
        }
        return rolesMenu;
    }

    @Override
    public String headline() {
        return "Add User";
    }
}
