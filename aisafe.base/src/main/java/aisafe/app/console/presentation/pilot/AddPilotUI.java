package aisafe.app.console.presentation.pilot;

import aisafe.aircraftmodel.domain.AircraftModel;
import aisafe.pilot.application.AddPilotController;
import aisafe.pilot.domain.Pilot;
import aisafe.usermanagement.domain.AiSafePasswordPolicy;
import aisafe.usermanagement.domain.Email;
import aisafe.usermanagement.domain.SecurityClearance;
import aisafe.usermanagement.domain.SecurityLevel;
import eapli.framework.infrastructure.authz.domain.model.Name;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Console UI for the "Add a Pilot" use case (US075).
 * The pilot is added to the authenticated collaborator's own company and certified
 * for one or more aircraft models selected from the registered ones.
 */
public class AddPilotUI extends AbstractUI {

    private final AddPilotController controller = new AddPilotController();

    @Override
    protected boolean doShow() {
        try {
            final Set<Long> certifications = selectCertifications();
            if (certifications == null) {
                // no models available, or the operator cancelled — message already shown
                return false;
            }

            final String username = readUsername();
            final String password = readPassword();
            final String firstName = readFirstName();
            final String lastName = readLastName();
            final String email = readEmail();
            final String phone = readPhone();
            final String position = readPosition();
            final SecurityClearance clearance = readSecurityClearance();
            final LocalDate skillsDate = readSkillsDate();

            if (!confirmRegistration(username, firstName, lastName, email, phone, position,
                    clearance, skillsDate, certifications.size())) {
                System.out.println("  Registration cancelled.");
                return false;
            }

            final Pilot pilot = controller.addPilot(
                    username, password, firstName, lastName,
                    email, phone, position,
                    new Email(email), clearance, skillsDate, certifications);

            printSuccess(pilot);

        } catch (final IllegalArgumentException e) {
            System.out.println("\n Validation Error: " + e.getMessage());
        } catch (final Exception e) {
            System.out.println("\n An error occurred: " + e.getMessage());
        }
        return false;
    }

    /**
     * Lists the registered aircraft models and lets the user pick one or more by id.
     * Keeps prompting until at least one valid model id is entered, or the operator cancels.
     *
     * @return the set of selected aircraft model identities, or {@code null} if there are no
     *         models to choose from or the operator cancelled the operation
     */
    private Set<Long> selectCertifications() {
        System.out.println("\n--- Available Aircraft Models ---");
        final Map<Long, AircraftModel> models = new HashMap<>();
        for (final AircraftModel model : controller.allAircraftModels()) {
            System.out.printf("  [%s] %s (%s)%n", model.identity(), model.modelName(), model.makerName());
            models.put(model.identity(), model);
        }
        if (models.isEmpty()) {
            System.out.println("  No aircraft models registered.");
            return null;
        }

        while (true) {
            final String raw = Console.readLine("Certified model ids (comma-separated, or 0 to cancel): ");
            if (raw.trim().equals("0")) {
                System.out.println("  Operation cancelled.");
                return null;
            }

            final Set<Long> selected = new HashSet<>();
            for (final String token : raw.split(",")) {
                final String trimmed = token.trim();
                if (trimmed.isEmpty()) continue;
                try {
                    final Long id = Long.valueOf(trimmed);
                    if (models.containsKey(id)) {
                        selected.add(id);
                    } else {
                        System.out.println("  Unknown model id: " + trimmed);
                    }
                } catch (final NumberFormatException e) {
                    System.out.println("  Invalid id: " + trimmed);
                }
            }

            if (!selected.isEmpty()) {
                return selected;
            }
            System.out.println("  Please enter at least one valid aircraft model id (or 0 to cancel).");
        }
    }

    private String readUsername() {
        while (true) {
            final String v = Console.readLine("Username: ").trim();
            if (!v.isEmpty()) return v;
            System.out.println("  Username cannot be empty.");
        }
    }

    private String readPassword() {
        final AiSafePasswordPolicy policy = new AiSafePasswordPolicy();
        while (true) {
            final String v = Console.readLine("Password (min 6 chars, 1 digit, 1 uppercase): ");
            if (policy.isSatisfiedBy(v)) return v;
            System.out.println("  Password must have at least 6 characters, one digit and one uppercase letter.");
        }
    }

    private String readFirstName() {
        while (true) {
            final String v = Console.readLine("First Name: ").trim();
            try {
                Name.valueOf(v, "Placeholder"); // validate first name against the framework rule
                return v;
            } catch (final RuntimeException e) {
                System.out.println("  " + e.getMessage());
            }
        }
    }

    private String readLastName() {
        while (true) {
            final String v = Console.readLine("Last Name: ").trim();
            try {
                Name.valueOf("Placeholder", v); // validate last name against the framework rule
                return v;
            } catch (final RuntimeException e) {
                System.out.println("  " + e.getMessage());
            }
        }
    }

    private String readEmail() {
        while (true) {
            final String v = Console.readLine("E-Mail: ").trim();
            try {
                new Email(v); // validate format via the Email value object
                return v;
            } catch (final IllegalArgumentException e) {
                System.out.println("  " + e.getMessage());
            }
        }
    }

    private String readPhone() {
        while (true) {
            final String v = Console.readLine("Phone Number: ").trim();
            final String digits = v.replaceAll("[+\\s]", "");
            if (digits.matches("\\d{9,15}")) return v;
            System.out.println("  Phone number must have 9 to 15 digits (optionally starting with '+').");
        }
    }

    private String readPosition() {
        while (true) {
            final String v = Console.readLine("Position: ").trim();
            if (!v.isEmpty()) return v;
            System.out.println("  Position cannot be empty.");
        }
    }

    private SecurityClearance readSecurityClearance() {
        final SecurityLevel level = readSecurityLevel();
        while (true) {
            try {
                final LocalDate expiration = LocalDate.parse(
                        Console.readLine("Expiration Date (YYYY-MM-DD): ").trim());
                return new SecurityClearance(level, expiration);
            } catch (final DateTimeParseException e) {
                System.out.println("  Invalid date format. Use YYYY-MM-DD.");
            } catch (final IllegalArgumentException e) {
                System.out.println("  " + e.getMessage());
            }
        }
    }

    private SecurityLevel readSecurityLevel() {
        System.out.println("\nSecurity Clearance Level:");
        final SecurityLevel[] levels = SecurityLevel.values();
        for (int i = 0; i < levels.length; i++) {
            System.out.printf("  %d - %s%n", i + 1, levels[i]);
        }
        while (true) {
            final int choice = Console.readInteger("Choice: ");
            if (choice >= 1 && choice <= levels.length) {
                return levels[choice - 1];
            }
            System.out.println("  Please choose a number between 1 and " + levels.length + ".");
        }
    }

    private LocalDate readSkillsDate() {
        while (true) {
            try {
                return LocalDate.parse(Console.readLine("Skills Assessment Date (YYYY-MM-DD): ").trim());
            } catch (final DateTimeParseException e) {
                System.out.println("  Invalid date format. Use YYYY-MM-DD.");
            }
        }
    }

    /**
     * Shows a summary of the data entered and asks the operator to confirm before registering.
     *
     * @return {@code true} if the operator confirms; {@code false} to abort
     */
    private boolean confirmRegistration(final String username, final String firstName,
                                        final String lastName, final String email,
                                        final String phone, final String position,
                                        final SecurityClearance clearance,
                                        final LocalDate skillsDate, final int certificationCount) {
        System.out.println("\n--- Review Pilot ---");
        System.out.println("  Username       : " + username);
        System.out.println("  Name           : " + firstName + " " + lastName);
        System.out.println("  E-Mail         : " + email);
        System.out.println("  Phone          : " + phone);
        System.out.println("  Position       : " + position);
        System.out.println("  Security level : " + clearance.level());
        System.out.println("  Clearance exp. : " + clearance.expirationDate());
        System.out.println("  Skills date    : " + skillsDate);
        System.out.println("  Certifications : " + certificationCount + " aircraft model(s)");
        while (true) {
            final String answer = Console.readLine("Confirm registration? (y/n): ").trim().toLowerCase();
            if (answer.equals("y") || answer.equals("yes")) return true;
            if (answer.equals("n") || answer.equals("no")) return false;
            System.out.println("  Please answer 'y' or 'n'.");
        }
    }

    /**
     * Prints a success message with the newly created pilot's details.
     *
     * @param pilot the saved pilot
     */
    private void printSuccess(final Pilot pilot) {
        System.out.println("\n Pilot successfully registered!");
        System.out.println("  Company        : " + pilot.companyIataCode());
        System.out.println("  Username       : " + pilot.user().systemUser().username());
        System.out.println("  Certifications : " + pilot.certifiedAircraftModelIds().size() + " aircraft model(s)");
    }

    @Override
    public String headline() {
        return "Add Pilot";
    }
}
