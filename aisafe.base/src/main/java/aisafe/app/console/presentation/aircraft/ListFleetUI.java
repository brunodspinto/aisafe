package aisafe.app.console.presentation.aircraft;

import aisafe.aircraft.application.ListFleetController;
import aisafe.aircraft.domain.Aircraft;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.List;

/**
 * Console UI for the "List Company Fleet" use case (US072).
 * Offers filter options (model, maker, capacity, year) and prints a tabular fleet summary.
 */
public class ListFleetUI extends AbstractUI {

    private final ListFleetController controller = new ListFleetController();

    @Override
    protected boolean doShow() {
        try {
            System.out.println("\n  1. All aircraft");
            System.out.println("  2. Filter by model");
            System.out.println("  3. Filter by maker");
            System.out.println("  4. Filter by minimum total seats");
            System.out.println("  5. Filter by year of manufacture (from year)");
            System.out.println("  0. Return");

            final int choice = Console.readInteger("Select filter: ");

            if (choice == 0) return false;

            final List<Aircraft> fleet;
            if (choice == 1) {
                fleet = controller.companyFleet();
            } else if (choice == 2) {
                final String model = Console.readLine("Model name: ").trim();
                fleet = controller.fleetByModel(model);
            } else if (choice == 3) {
                final String maker = Console.readLine("Maker name: ").trim();
                fleet = controller.fleetByMaker(maker);
            } else if (choice == 4) {
                final int minSeats = Console.readInteger("Minimum total seats: ");
                fleet = controller.fleetByMinCapacity(minSeats);
            } else if (choice == 5) {
                final int fromYear = Console.readInteger("Manufactured from year: ");
                fleet = controller.fleetByManufactureYearFrom(fromYear);
            } else {
                System.out.println("Invalid option.");
                return false;
            }

            printFleet(fleet);

        } catch (final IllegalStateException e) {
            System.out.println("\nError: " + e.getMessage());
        } catch (final Exception e) {
            System.out.println("\nAn error occurred: " + e.getMessage());
        }
        return false;
    }

    /**
     * Prints the fleet list as a formatted table, or a "no aircraft found" message if empty.
     *
     * @param fleet aircraft to display
     */
    private void printFleet(final List<Aircraft> fleet) {
        if (fleet.isEmpty()) {
            System.out.println("\nNo aircraft found matching the criteria.");
            return;
        }
        System.out.printf("%n%-12s %-12s %-20s %-15s %6s %5s%n",
                "REGISTRATION", "COUNTRY", "MODEL", "MAKER", "SEATS", "YEAR");
        System.out.println("-".repeat(75));
        for (final Aircraft a : fleet) {
            final String seats = a.cabinConfiguration() == null
                    ? "CARGO"
                    : String.valueOf(a.cabinConfiguration().totalSeats());
            System.out.printf("%-12s %-12s %-20s %-15s %6s %5d%n",
                    a.registrationNumber(),
                    a.registeredCountry(),
                    a.aircraftModel().modelName(),
                    a.aircraftModel().makerName(),
                    seats,
                    a.yearOfManufacture());
        }
        System.out.printf("%nTotal: %d aircraft%n", fleet.size());
    }

    @Override
    public String headline() {
        return "List Company Fleet";
    }
}
