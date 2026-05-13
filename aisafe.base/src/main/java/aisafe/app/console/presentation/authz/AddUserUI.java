package aisafe.app.console.presentation.authz;

import aisafe.usermanagement.application.AddUserController;
import aisafe.usermanagement.domain.Email;
import aisafe.usermanagement.domain.SecurityClearance;
import aisafe.usermanagement.domain.SecurityLevel;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Set;

/**
 * Console UI for the "Add User" use case (US031).
 * Collects user registration data interactively and delegates to {@link AddUserController}.
 */
public class AddUserUI extends AbstractUI {

    private final AddUserController controller = new AddUserController();

    @Override
    protected boolean doShow() {
        final String username = readUsername();
        final String password = readPassword();
        final String firstName = readName("First Name");
        final String lastName = readName("Last Name");
        final String phoneNumber = readPhoneNumber();
        final Email email = readEmail();
        final String position = readPosition();
        final SecurityClearance clearance = readSecurityClearance();
        final LocalDate skillsDate = readDate("Skills Assessment Date (YYYY-MM-DD)");

        final Set<Role> roles = readRoles();

        try {
            controller.addUser(username, password, firstName, lastName,
                    email.address(), roles, phoneNumber, position,
                    email, clearance, skillsDate);
            System.out.println("User successfully registered.");
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            System.out.println("That username or email is already in use.");
        } catch (final IllegalArgumentException e) {
            System.out.println("Invalid data: " + e.getMessage());
        }

        return false;
    }

    private String readUsername() {
        while (true) {
            final String value = Console.readLine("Username").trim();
            if (value.length() < 3) {
                System.out.println("Username must be at least 3 characters.");
            } else if (!value.matches("[a-zA-Z0-9._-]+")) {
                System.out.println("Username may only contain letters, digits, '.', '_' or '-'.");
            } else {
                return value;
            }
        }
    }

    private String readPassword() {
        while (true) {
            final String value = Console.readLine("Password (min 6 chars, 1 digit, 1 uppercase)");
            if (value.length() < 6) {
                System.out.println("Password must be at least 6 characters.");
            } else if (!value.chars().anyMatch(Character::isDigit)) {
                System.out.println("Password must contain at least one digit.");
            } else if (!value.chars().anyMatch(Character::isUpperCase)) {
                System.out.println("Password must contain at least one uppercase letter.");
            } else {
                return value;
            }
        }
    }

    private String readName(final String prompt) {
        while (true) {
            final String value = Console.readLine(prompt).trim();
            if (value.length() < 2) {
                System.out.println(prompt + " must be at least 2 characters.");
            } else if (!value.matches("[a-zA-ZÀ-ÿ '\\-]+")) {
                System.out.println(prompt + " may only contain letters, spaces, hyphens or apostrophes.");
            } else {
                return value;
            }
        }
    }

    private String readPhoneNumber() {
        while (true) {
            final String value = Console.readLine("Phone Number").trim();
            final String digits = value.replaceAll("[+\\s]", "");
            if (!digits.matches("\\d{9,15}")) {
                System.out.println("Phone number must have 9 to 15 digits (optionally starting with '+').");
            } else {
                return value;
            }
        }
    }

    private String readPosition() {
        while (true) {
            final String value = Console.readLine("Position").trim();
            if (value.length() < 2) {
                System.out.println("Position must be at least 2 characters.");
            } else {
                return value;
            }
        }
    }

    private Email readEmail() {
        while (true) {
            try {
                return new Email(Console.readLine("E-Mail"));
            } catch (final IllegalArgumentException e) {
                System.out.println("Invalid email format. Please try again.");
            }
        }
    }

    private SecurityClearance readSecurityClearance() {
        while (true) {
            try {
                System.out.println("Security Clearance Level:");
                for (final SecurityLevel lvl : SecurityLevel.values()) {
                    System.out.printf("  %d - %s%n", lvl.getCode(), lvl.name());
                }
                final int code = Integer.parseInt(Console.readLine("Choice").trim());
                final SecurityLevel level = SecurityLevel.fromCode(code);
                final LocalDate expiry = readDate("Security Clearance Expiration (YYYY-MM-DD)");
                return new SecurityClearance(level, expiry);
            } catch (final NumberFormatException e) {
                System.out.println("Invalid choice. Enter a number between 1 and 5.");
            } catch (final IllegalArgumentException e) {
                System.out.println("Invalid clearance: " + e.getMessage() + ". Please try again.");
            }
        }
    }

    private LocalDate readDate(final String prompt) {
        while (true) {
            try {
                return LocalDate.parse(Console.readLine(prompt).trim());
            } catch (final DateTimeParseException e) {
                System.out.println("Invalid date format. Use YYYY-MM-DD.");
            }
        }
    }

    private Set<Role> readRoles() {
        final Role[] available = controller.getRoleTypes();
        while (true) {
            System.out.println("\nSelect Role >");
            for (int i = 0; i < available.length; i++) {
                System.out.printf("  %d - %s%n", i + 1, available[i]);
            }
            try {
                final int choice = Integer.parseInt(Console.readLine("Choice").trim());
                if (choice >= 1 && choice <= available.length) {
                    final Set<Role> selected = new HashSet<>();
                    selected.add(available[choice - 1]);
                    return selected;
                }
                System.out.println("Invalid option. Enter a number between 1 and " + available.length + ".");
            } catch (final NumberFormatException e) {
                System.out.println("Enter a valid number.");
            }
        }
    }

    @Override
    public String headline() {
        return "Add User";
    }
}
