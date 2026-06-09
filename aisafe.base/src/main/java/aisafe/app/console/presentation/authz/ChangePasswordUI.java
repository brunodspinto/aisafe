package aisafe.app.console.presentation.authz;

import aisafe.usermanagement.application.ChangePasswordController;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

/**
 * Console UI for the "Change My Password" use case.
 * Available to any authenticated user from the "My Account" menu.
 */
public class ChangePasswordUI extends AbstractUI {

    private final ChangePasswordController controller = new ChangePasswordController();

    @Override
    protected boolean doShow() {
        try {
            final String currentPassword = Console.readLine("Current password (or 0 to cancel): ");
            if (currentPassword.trim().equals("0")) {
                System.out.println("  Operation cancelled.");
                return false;
            }

            final String newPassword = readNewPassword();
            if (newPassword == null) {
                return false;
            }

            if (controller.changePassword(currentPassword, newPassword)) {
                System.out.println("\n Password changed successfully.");
            } else {
                System.out.println("\n Could not change password. Please check that your current password is correct.");
            }

        } catch (final IllegalArgumentException e) {
            System.out.println("\n Validation Error: " + e.getMessage());
        } catch (final Exception e) {
            System.out.println("\n An error occurred: " + e.getMessage());
        }
        return false;
    }

    /**
     * Reads and confirms the new password, re-prompting until it satisfies the policy and the
     * confirmation matches, or the operator cancels with 0.
     *
     * @return the validated new password, or {@code null} if cancelled
     */
    private String readNewPassword() {
        while (true) {
            final String newPassword = Console.readLine(
                    "New password (min 6 chars, 1 digit, 1 uppercase, or 0 to cancel): ");
            if (newPassword.trim().equals("0")) {
                System.out.println("  Operation cancelled.");
                return null;
            }
            if (!controller.isPasswordAcceptable(newPassword)) {
                System.out.println("  Password must have at least 6 characters, one digit and one uppercase letter.");
                continue;
            }
            final String confirmation = Console.readLine("Confirm new password: ");
            if (!newPassword.equals(confirmation)) {
                System.out.println("  Passwords do not match. Please try again.");
                continue;
            }
            return newPassword;
        }
    }

    @Override
    public String headline() {
        return "Change Password";
    }
}
