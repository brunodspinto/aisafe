package aisafe.app.console.presentation.aircraft;

import aisafe.aircraft.application.DecommissionAircraftController;
import aisafe.aircraft.domain.Aircraft;
import aisafe.airtransportcompany.domain.AirTransportCompany;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

/**
 * Console UI for the "Decommission Aircraft" use case (US071).
 * Allows selection of a company and one of its active aircraft for decommissioning,
 * with an explicit confirmation step.
 */
public class DecommissionAircraftUI extends AbstractUI {

    private final DecommissionAircraftController controller =
            new DecommissionAircraftController();

    @Override
    protected boolean doShow() {
        try {
            System.out.println("\n--- Available Companies ---");
            final List<AirTransportCompany> companies = new ArrayList<>();
            for (final AirTransportCompany company : controller.allCompanies()) {
                System.out.printf("  [%s] %s%n", company.identity(), company.name());
                companies.add(company);
            }
            if (companies.isEmpty()) {
                System.out.println("  No companies registered.");
                return false;
            }

            final String iataCode = Console.readLine("Company IATA Code: ");

            System.out.println("\n--- Active Aircraft ---");
            final List<Aircraft> aircraft = new ArrayList<>();
            for (final Aircraft a : controller.activeAircraftByCompany(iataCode)) {
                System.out.printf("  [%d] %s | Model: %s | Country: %s%n",
                        aircraft.size() + 1,
                        a.registrationNumber(),
                        a.aircraftModel().modelName(),
                        a.registeredCountry());
                aircraft.add(a);
            }

            if (aircraft.isEmpty()) {
                System.out.println("  No active aircraft found.");
                return false;
            }

            final int choice = Console.readInteger("Select aircraft to decommission (number): ");
            if (choice < 1 || choice > aircraft.size()) {
                System.out.println("Invalid selection.");
                return false;
            }

            final Aircraft selected = aircraft.get(choice - 1);
            final String confirm = Console.readLine(
                    "Are you sure you want to decommission " + selected.registrationNumber() + "? (yes/no): ");

            if (!confirm.equalsIgnoreCase("yes")) {
                System.out.println("Operation cancelled.");
                return false;
            }

            controller.decommission(selected);

            System.out.println("\n Aircraft successfully decommissioned!");
            System.out.println("  Registration : " + selected.registrationNumber());
            System.out.println("  Status       : DECOMMISSIONED");

        } catch (final IllegalStateException e) {
            System.out.println("\n Error: " + e.getMessage());
        } catch (final IllegalArgumentException e) {
            System.out.println("\n Validation Error: " + e.getMessage());
        } catch (final Exception e) {
            System.out.println("\n An error occurred: " + e.getMessage());
        }
        return false;
    }

    @Override
    public String headline() {
        return "Decommission Aircraft";
    }
}
