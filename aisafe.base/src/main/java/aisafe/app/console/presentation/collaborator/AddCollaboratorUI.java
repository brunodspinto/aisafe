package aisafe.app.console.presentation.collaborator;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.collaborator.application.AddCollaboratorController;
import aisafe.collaborator.domain.Collaborator;
import aisafe.usermanagement.domain.AiSafeRoles;
import aisafe.usermanagement.domain.Email;
import aisafe.usermanagement.domain.SecurityClearance;
import aisafe.usermanagement.domain.SecurityLevel;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AddCollaboratorUI extends AbstractUI {

    private final AddCollaboratorController controller = new AddCollaboratorController();

    @Override
    protected boolean doShow() {
        try {
            System.out.println("\nCustomer Type:");
            System.out.println("  1 - Air Transport Company");
            System.out.println("  2 - Air Control Area");
            final int customerType = Console.readInteger("Choice: ");

            if (customerType == 1) {
                addCompanyCollaborator();
            } else if (customerType == 2) {
                addAreaCollaborator();
            } else {
                System.out.println("Invalid selection.");
            }

        } catch (final IllegalArgumentException e) {
            System.out.println("\n Validation Error: " + e.getMessage());
        } catch (final Exception e) {
            System.out.println("\n An error occurred: " + e.getMessage());
        }
        return false;
    }

    private void addCompanyCollaborator() {
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
        final Role role = selectRole(new Role[]{AiSafeRoles.ATCC, AiSafeRoles.PILOT});
        final Collaborator collaborator = controller.addCompanyCollaborator(
                readUsername(), readPassword(), readFirstName(), readLastName(),
                readEmail(), Set.of(role), readPhone(), readPosition(),
                readEmailVO(), readSecurityClearance(), readSkillsDate(), iataCode);

        printSuccess(collaborator);
    }

    private void addAreaCollaborator() {
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
        final Role role = AiSafeRoles.FLIGHT_CONTROL_OPERATOR;
        final Collaborator collaborator = controller.addAreaCollaborator(
                readUsername(), readPassword(), readFirstName(), readLastName(),
                readEmail(), Set.of(role), readPhone(), readPosition(),
                readEmailVO(), readSecurityClearance(), readSkillsDate(), areaCode);

        printSuccess(collaborator);
    }

    private Role selectRole(final Role[] roles) {
        System.out.println("\nSelect Role:");
        for (int i = 0; i < roles.length; i++) {
            System.out.printf("  %d - %s%n", i + 1, roles[i]);
        }
        final int choice = Console.readInteger("Choice: ");
        return roles[choice - 1];
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
        final LocalDate expiration = LocalDate.parse(
                Console.readLine("Expiration Date (YYYY-MM-DD): "));
        return new SecurityClearance(levels[choice - 1], expiration);
    }

    private LocalDate readSkillsDate() {
        return LocalDate.parse(Console.readLine("Skills Assessment Date (YYYY-MM-DD): "));
    }

    private void printSuccess(final Collaborator collaborator) {
        System.out.println("\n Collaborator successfully registered!");
        System.out.println("  Customer : " + collaborator.customerName());
        System.out.println("  Username : " + collaborator.user().systemUser().username());
    }

    @Override
    public String headline() {
        return "Add Customer's Collaborator";
    }
}
