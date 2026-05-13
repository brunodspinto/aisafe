package aisafe.app.console.presentation.collaborator;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.collaborator.application.ListCollaboratorsByCustomerController;
import aisafe.collaborator.domain.Collaborator;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

/**
 * Console UI for the "List Customer's Active Collaborators" use case (US060).
 * Displays a tabular list of active collaborators filtered by company or area.
 */
public class ListCollaboratorsByCustomerUI extends AbstractUI {

    private final ListCollaboratorsByCustomerController controller =
            new ListCollaboratorsByCustomerController();

    @Override
    protected boolean doShow() {
        System.out.println("\nCustomer Type:");
        System.out.println("  1 - Air Transport Company");
        System.out.println("  2 - Air Control Area");
        final int customerType = Console.readInteger("Choice: ");

        if (customerType == 1) {
            showByCompany();
        } else if (customerType == 2) {
            showByArea();
        } else {
            System.out.println("Invalid selection.");
        }
        return false;
    }

    /** Lists active collaborators for a company chosen by IATA code. */
    private void showByCompany() {
        System.out.println("\n--- Available Air Transport Companies ---");
        final List<AirTransportCompany> companies = new ArrayList<>();
        for (final AirTransportCompany company : controller.allCompanies()) {
            System.out.printf("  [%s] %s%n", company.identity(), company.name());
            companies.add(company);
        }
        if (companies.isEmpty()) {
            System.out.println("  No companies registered.");
            return;
        }

        final String iataCode = Console.readLine("Company IATA Code: ");
        final AirTransportCompany selected = companies.stream()
                .filter(c -> c.identity().toString().equalsIgnoreCase(iataCode))
                .findFirst()
                .orElse(null);

        if (selected == null) {
            System.out.println("Company not found: " + iataCode);
            return;
        }

        printCollaborators(controller.listActiveByCompany(selected));
    }

    /** Lists active collaborators for an air control area chosen by code. */
    private void showByArea() {
        System.out.println("\n--- Available Air Control Areas ---");
        final List<AirControlArea> areas = new ArrayList<>();
        for (final AirControlArea area : controller.allAreas()) {
            System.out.printf("  [%s] %s%n", area.areaCode(), area.name());
            areas.add(area);
        }
        if (areas.isEmpty()) {
            System.out.println("  No areas registered.");
            return;
        }

        final String areaCode = Console.readLine("Area Code: ");
        final AirControlArea selected = areas.stream()
                .filter(a -> a.areaCode().equalsIgnoreCase(areaCode))
                .findFirst()
                .orElse(null);

        if (selected == null) {
            System.out.println("Area not found: " + areaCode);
            return;
        }

        printCollaborators(controller.listActiveByArea(selected));
    }

    /**
     * Prints collaborators as a formatted table.
     * Outputs a "no results" message if the iterable is empty.
     *
     * @param collaborators collaborators to display
     */
    private void printCollaborators(final Iterable<Collaborator> collaborators) {
        System.out.println();
        System.out.printf("%-20s %-25s %-20s %-8s%n",
                "Mecanographic No.", "Name", "Position", "Status");
        System.out.println("-".repeat(76));
        boolean any = false;
        for (final Collaborator c : collaborators) {
            any = true;
            final String mecNo = c.user().identity() != null
                    ? c.user().identity().toString() : "-";
            final String name = c.user().systemUser() != null
                    ? c.user().systemUser().name().firstName()
                      + " " + c.user().systemUser().name().lastName()
                    : "-";
            final String position = c.user().position() != null
                    ? c.user().position() : "-";
            System.out.printf("%-20s %-25s %-20s %-8s%n",
                    mecNo, name, position, "ACTIVE");
        }
        if (!any) {
            System.out.println("No active collaborators found for this customer.");
        }
    }

    @Override
    public String headline() {
        return "List Customer's Active Collaborators";
    }
}
