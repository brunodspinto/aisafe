package aisafe.app.console.presentation.collaborator;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.collaborator.application.EditCollaboratorController;
import aisafe.collaborator.domain.Collaborator;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

/**
 * Console UI for the "Edit Customer's Collaborator" use case (US063).
 * Allows updating the email and phone number of an active collaborator.
 */
public class EditCollaboratorUI extends AbstractUI {

    private final EditCollaboratorController controller = new EditCollaboratorController();

    @Override
    protected boolean doShow() {
        try {
            System.out.println("\nCustomer Type:");
            System.out.println("  1 - Air Transport Company");
            System.out.println("  2 - Air Control Area");
            final int customerType = Console.readInteger("Choice: ");

            final List<Collaborator> collaborators = new ArrayList<>();

            if (customerType == 1) {
                System.out.println("\n--- Available Air Transport Companies ---");
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
                final AirTransportCompany selected = companies.stream()
                        .filter(c -> c.identity().toString().equalsIgnoreCase(iataCode))
                        .findFirst().orElse(null);
                if (selected == null) {
                    System.out.println("Company not found.");
                    return false;
                }
                for (final Collaborator c : controller.activeCollaboratorsByCompany(selected)) {
                    collaborators.add(c);
                }
            } else if (customerType == 2) {
                System.out.println("\n--- Available Air Control Areas ---");
                final List<AirControlArea> areas = new ArrayList<>();
                for (final AirControlArea area : controller.allAreas()) {
                    System.out.printf("  [%s] %s%n", area.areaCode(), area.name());
                    areas.add(area);
                }
                if (areas.isEmpty()) {
                    System.out.println("  No areas registered.");
                    return false;
                }
                final String areaCode = Console.readLine("Area Code: ");
                final AirControlArea selected = areas.stream()
                        .filter(a -> a.areaCode().toString().equalsIgnoreCase(areaCode))
                        .findFirst().orElse(null);
                if (selected == null) {
                    System.out.println("Area not found.");
                    return false;
                }
                for (final Collaborator c : controller.activeCollaboratorsByArea(selected)) {
                    collaborators.add(c);
                }
            } else {
                System.out.println("Invalid selection.");
                return false;
            }

            if (collaborators.isEmpty()) {
                System.out.println("No active collaborators found.");
                return false;
            }

            System.out.println("\n--- Active Collaborators ---");
            for (int i = 0; i < collaborators.size(); i++) {
                final Collaborator c = collaborators.get(i);
                System.out.printf("  [%d] %s %s | Email: %s | Phone: %s%n",
                        i + 1,
                        c.user().systemUser().name().firstName(),
                        c.user().systemUser().name().lastName(),
                        c.user().email().address(),
                        c.user().phoneNumber());
            }

            final int choice = Console.readInteger("Select collaborator to edit (number): ");
            if (choice < 1 || choice > collaborators.size()) {
                System.out.println("Invalid selection.");
                return false;
            }

            final Collaborator selected = collaborators.get(choice - 1);
            System.out.println("\nLeave blank to keep current value.");
            final String newEmail = Console.readLine("New Email [" + selected.user().email().address() + "]: ");
            final String newPhone = Console.readLine("New Phone [" + selected.user().phoneNumber() + "]: ");

            final String finalEmail = newEmail.isBlank() ? selected.user().email().address() : newEmail;
            final String finalPhone = newPhone.isBlank() ? selected.user().phoneNumber() : newPhone;

            controller.updateContact(selected, finalEmail, finalPhone);

            System.out.println("\n Collaborator successfully updated!");
            System.out.println("  Email : " + finalEmail);
            System.out.println("  Phone : " + finalPhone);

        } catch (final IllegalArgumentException e) {
            System.out.println("\n Validation Error: " + e.getMessage());
        } catch (final Exception e) {
            System.out.println("\n An error occurred: " + e.getMessage());
        }
        return false;
    }

    @Override
    public String headline() {
        return "Edit Customer's Collaborator";
    }
}
