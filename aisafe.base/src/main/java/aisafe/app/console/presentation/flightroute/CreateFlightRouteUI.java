package aisafe.app.console.presentation.flightroute;

import aisafe.airport.domain.Airport;
import aisafe.flightroute.application.CreateFlightRouteController;
import aisafe.flightroute.domain.FlightRoute;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

/**
 * Console UI for the "Create Flight Route" use case (US073).
 * Collects route name, origin and destination airport from the authenticated ATCC.
 */
public class CreateFlightRouteUI extends AbstractUI {

    private final CreateFlightRouteController controller = new CreateFlightRouteController();

    @Override
    protected boolean doShow() {
        System.out.println("\n--- Available Airports ---");
        for (final Airport airport : controller.allAirports()) {
            System.out.printf("  %s — %s, %s%n",
                    airport.iataCode().code(), airport.name(), airport.country());
        }

        final String routeName = readRouteName();
        final String originCode = readAirportCode("Origin Airport IATA Code");
        final String destCode = readAirportCode("Destination Airport IATA Code");

        try {
            final FlightRoute route = controller.createFlightRoute(routeName, originCode, destCode);
            System.out.println("\nFlight route successfully created!");
            System.out.printf("  Route   : %s%n", route.identity());
            System.out.printf("  Origin  : %s%n", route.originAirport().code());
            System.out.printf("  Dest    : %s%n", route.destinationAirport().code());
            System.out.printf("  Company : %s%n", route.companyIataCode().code());
            System.out.printf("  Status  : %s%n", route.status());
        } catch (final IllegalArgumentException | IllegalStateException e) {
            System.out.println("Error: " + e.getMessage());
        }

        return false;
    }

    private String readRouteName() {
        while (true) {
            final String value = Console.readLine("Route Name (e.g. TP123)").trim().toUpperCase();
            if (value.matches("[A-Z]{2}[0-9]{1,4}")) {
                return value;
            }
            System.out.println("Invalid format. Route name must be 2 uppercase letters followed by 1 to 4 digits (e.g. TP123).");
        }
    }

    private String readAirportCode(final String prompt) {
        while (true) {
            final String value = Console.readLine(prompt).trim().toUpperCase();
            if (value.matches("[A-Z]{3}")) {
                return value;
            }
            System.out.println("Invalid format. Airport IATA code must be exactly 3 uppercase letters.");
        }
    }

    @Override
    public String headline() {
        return "Create Flight Route";
    }
}