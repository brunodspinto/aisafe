package aisafe.app.console.presentation.pilot;

import aisafe.pilot.application.RemovePilotController;
import aisafe.pilot.domain.Pilot;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

/**
 * Console UI for the "Remove a Pilot" use case (US077).
 * Lists the active pilots of the authenticated collaborator's company,
 * asks for selection and confirmation, then deactivates the chosen pilot.
 */
public class RemovePilotUI extends AbstractUI {

    private final RemovePilotController controller = new RemovePilotController();

    @Override
    protected boolean doShow() {
        final List<Pilot> pilots = toList(controller.allActivePilotsOfCompany());

        if (pilots.isEmpty()) {
            System.out.println("No active pilots in your company.");
            return false;
        }

        System.out.println("\nActive pilots:");
        for (int i = 0; i < pilots.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, pilots.get(i));
        }

        final int choice = Console.readInteger("\nSelect pilot number (0 to cancel): ");
        if (choice < 1 || choice > pilots.size()) {
            System.out.println("Operation cancelled.");
            return false;
        }

        final Pilot selected = pilots.get(choice - 1);
        final String confirm = Console.readLine(
                "Deactivate pilot " + selected + "? (yes/no): ");
        if (!"yes".equalsIgnoreCase(confirm.trim())) {
            System.out.println("Operation cancelled.");
            return false;
        }

        try {
            controller.deactivatePilot(selected.identity());
            System.out.println("Pilot successfully deactivated.");
        } catch (final Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Remove Pilot";
    }

    private <T> List<T> toList(final Iterable<T> iterable) {
        final List<T> list = new ArrayList<>();
        iterable.forEach(list::add);
        return list;
    }
}
