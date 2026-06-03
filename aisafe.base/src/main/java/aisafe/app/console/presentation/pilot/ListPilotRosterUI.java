package aisafe.app.console.presentation.pilot;

import aisafe.pilot.application.ListPilotRosterController;
import aisafe.pilot.domain.Pilot;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.List;

/**
 * Console UI for the "List Company Pilot Roster" use case (US076).
 * Presents three filter options and renders the result as a formatted table.
 */
public class ListPilotRosterUI extends AbstractUI {

    private final ListPilotRosterController controller = new ListPilotRosterController();

    @Override
    protected boolean doShow() {
        try {
            System.out.println("\n  1. All pilots");
            System.out.println("  2. Active pilots only");
            System.out.println("  3. By certified aircraft model name");
            System.out.println("  0. Return");

            final int choice = Console.readInteger("Select filter: ");

            if (choice == 0) return false;

            final List<Pilot> pilots;
            if (choice == 1) {
                pilots = controller.allPilots();
            } else if (choice == 2) {
                pilots = controller.activePilots();
            } else if (choice == 3) {
                final String modelName = Console.readLine("Aircraft model name: ").trim();
                pilots = controller.pilotsByCertifiedModel(modelName);
            } else {
                System.out.println("Invalid option.");
                return false;
            }

            printRoster(pilots);

        } catch (final IllegalStateException e) {
            System.out.println("\nError: " + e.getMessage());
        } catch (final Exception e) {
            System.out.println("\nAn error occurred: " + e.getMessage());
        }
        return false;
    }

    /**
     * Renders the pilot list as a formatted table, or the standard empty-result message.
     *
     * @param pilots pilots to display
     */
    private void printRoster(final List<Pilot> pilots) {
        if (pilots.isEmpty()) {
            System.out.println("\nNo pilots found for the selected filter.");
            return;
        }
        System.out.printf("%n%-24s %-10s %-14s %14s%n",
                "USERNAME", "STATUS", "COMPANY (IATA)", "CERTIFICATIONS");
        System.out.println("-".repeat(66));
        for (final Pilot p : pilots) {
            System.out.printf("%-24s %-10s %-14s %14d%n",
                    p.user().systemUser().username(),
                    p.isActive() ? "ACTIVE" : "INACTIVE",
                    p.companyIataCode(),
                    p.certifiedAircraftModelIds().size());
        }
        System.out.printf("%nTotal: %d pilot(s)%n", pilots.size());
    }

    @Override
    public String headline() {
        return "List Pilot Roster";
    }
}
