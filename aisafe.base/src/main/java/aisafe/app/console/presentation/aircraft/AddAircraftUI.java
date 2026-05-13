package aisafe.app.console.presentation.aircraft;

import aisafe.aircraft.application.AddAircraftController;
import aisafe.aircraftmodel.domain.AircraftModel;
import aisafe.airtransportcompany.domain.AirTransportCompany;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

public class AddAircraftUI extends AbstractUI {

    private final AddAircraftController controller = new AddAircraftController();

    @Override
    protected boolean doShow() {
        try {
            System.out.println("\n--- Available Aircraft Models ---");
            final List<AircraftModel> models = new ArrayList<>();
            for (final AircraftModel m : controller.allAircraftModels()) {
                System.out.printf("  [%d] %s — %s (max seats: %s)%n",
                        models.size() + 1,
                        m.modelName(),
                        m.maker().name(),
                        m.maxCapacity() > 0 ? m.maxCapacity() : "unlimited");
                models.add(m);
            }
            if (models.isEmpty()) {
                System.out.println("  No aircraft models registered. Please register a model first.");
                return false;
            }

            int modelChoice;
            do {
                modelChoice = Console.readInteger("Select model (number): ");
            } while (modelChoice < 1 || modelChoice > models.size());
            final AircraftModel selectedModel = models.get(modelChoice - 1);

            String registration;
            do {
                registration = Console.readLine("Registration number (e.g. CS-TUG): ").trim();
            } while (registration.isBlank());

            String country;
            do {
                country = Console.readLine("Registered country: ").trim();
            } while (country.isBlank());

            int crew;
            do {
                crew = Console.readInteger("Number of crew elements: ");
            } while (crew < 1);

            int firstClass;
            do {
                firstClass = Console.readInteger("First class seats: ");
            } while (firstClass < 0);

            int business;
            do {
                business = Console.readInteger("Business class seats: ");
            } while (business < 0);

            int economy;
            do {
                economy = Console.readInteger("Economy class seats: ");
            } while (economy < 0);

            if (firstClass + business + economy == 0) {
                System.out.println("Cabin must have at least one seat.");
                return false;
            }

            final int currentYear = java.time.Year.now().getValue();
            int year;
            do {
                year = Console.readInteger("Year of manufacture (1900–" + currentYear + "): ");
            } while (year < 1900 || year > currentYear);

            final AirTransportCompany company = controller.addAircraft(
                    selectedModel, registration, country, crew, year, firstClass, business, economy);

            System.out.printf("%nAircraft '%s' successfully added to fleet of %s!%n",
                    registration.toUpperCase(), company.name());
            System.out.printf("  Model      : %s (%s)%n", selectedModel.modelName(), selectedModel.maker().name());
            System.out.printf("  Country    : %s%n", country);
            System.out.printf("  Crew       : %d%n", crew);
            System.out.printf("  Cabin      : First=%d  Business=%d  Economy=%d  (Total=%d)%n",
                    firstClass, business, economy, firstClass + business + economy);

        } catch (final IllegalArgumentException e) {
            System.out.println("\nValidation Error: " + e.getMessage());
        } catch (final IllegalStateException e) {
            System.out.println("\nError: " + e.getMessage());
        } catch (final Exception e) {
            System.out.println("\nAn error occurred: " + e.getMessage());
        }
        return false;
    }

    @Override
    public String headline() {
        return "Add Aircraft to Fleet";
    }
}
