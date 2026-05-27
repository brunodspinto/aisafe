package aisafe.app.console.presentation.pilot;

import aisafe.aircraftmodel.domain.AircraftModel;
import aisafe.pilot.application.AddPilotController;
import aisafe.pilot.domain.Pilot;
import aisafe.usermanagement.domain.Email;
import aisafe.usermanagement.domain.SecurityClearance;
import aisafe.usermanagement.domain.SecurityLevel;
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
            if (certifications.isEmpty()) {
                System.out.println("A pilot must be certified for at least one aircraft model.");
                return false;
            }

            final Pilot pilot = controller.addPilot(
                    readUsername(), readPassword(), readFirstName(), readLastName(),
                    readEmail(), readPhone(), readPosition(),
                    readEmailVO(), readSecurityClearance(), readSkillsDate(), certifications);

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
     *
     * @return the set of selected aircraft model identities
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
            return Set.of();
        }

        final String raw = Console.readLine("Certified model ids (comma-separated): ");
        final Set<Long> selected = new HashSet<>();
        for (final String token : raw.split(",")) {
            final String trimmed = token.trim();
            if (trimmed.isEmpty()) continue;
            try {
                final Long id = Long.valueOf(trimmed);
                if (models.containsKey(id)) {
                    selected.add(id);
                } else {
                    System.out.println("  Ignoring unknown model id: " + trimmed);
                }
            } catch (final NumberFormatException e) {
                System.out.println("  Ignoring invalid id: " + trimmed);
            }
        }
        return selected;
    }

    private String readUsername() { return Console.readLine("Username: "); }
    private String readPassword() { return Console.readLine("Password (min 6 chars, 1 digit, 1 uppercase): "); }
    private String readFirstName() { return Console.readLine("First Name: "); }
    private String readLastName() { return Console.readLine("Last Name: "); }
    private String readEmail() { return Console.readLine("E-Mail (for system user): "); }
    private String readPhone() { return Console.readLine("Phone Number: "); }
    private String readPosition() { return Console.readLine("Position: "); }

    private Email readEmailVO() {
        return new Email(Console.readLine("E-Mail (for AISafe user): "));
    }

    private SecurityClearance readSecurityClearance() {
        System.out.println("\nSecurity Clearance Level:");
        final SecurityLevel[] levels = SecurityLevel.values();
        for (int i = 0; i < levels.length; i++) {
            System.out.printf("  %d - %s%n", i + 1, levels[i]);
        }
        final int choice = Console.readInteger("Choice: ");
        while (true) {
            try {
                final LocalDate expiration = LocalDate.parse(
                        Console.readLine("Expiration Date (YYYY-MM-DD): ").trim());
                return new SecurityClearance(levels[choice - 1], expiration);
            } catch (final DateTimeParseException e) {
                System.out.println("Invalid date format. Use YYYY-MM-DD.");
            }
        }
    }

    private LocalDate readSkillsDate() {
        while (true) {
            try {
                return LocalDate.parse(Console.readLine("Skills Assessment Date (YYYY-MM-DD): ").trim());
            } catch (final DateTimeParseException e) {
                System.out.println("Invalid date format. Use YYYY-MM-DD.");
            }
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
