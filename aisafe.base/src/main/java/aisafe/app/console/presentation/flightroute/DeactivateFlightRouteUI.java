package aisafe.app.console.presentation.flightroute;

import aisafe.flightroute.application.DeactivateFlightRouteController;
import aisafe.flightroute.domain.FlightRoute;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Console UI for the "Deactivate Flight Route" use case (US074).
 * Lists active routes of the ATCC's company and collects the deactivation date.
 */
public class DeactivateFlightRouteUI extends AbstractUI {

    private final DeactivateFlightRouteController controller = new DeactivateFlightRouteController();

    @Override
    protected boolean doShow() {
        final List<FlightRoute> routes = new ArrayList<>();

        System.out.println("\n--- Active Flight Routes ---");
        int i = 1;
        for (final FlightRoute r : controller.activeRoutesByCompany()) {
            System.out.printf("  [%d] %s -> %s  (%s)  — ACTIVE%n",
                    i++, r.originAirport().code(), r.destinationAirport().code(),
                    r.companyIataCode().code());
            routes.add(r);
        }

        if (routes.isEmpty()) {
            System.out.println("No active routes found for your company.");
            return false;
        }

        final int idx = Console.readInteger("\nSelect route number") - 1;
        if (idx < 0 || idx >= routes.size()) {
            System.out.println("Invalid selection.");
            return false;
        }

        final LocalDate date = readDate("Deactivation date (YYYY-MM-DD)");
        if (date == null) return false;

        try {
            final FlightRoute saved = controller.deactivateFlightRoute(
                    routes.get(idx).identity(), date);
            System.out.printf("%nRoute '%s' deactivated from %s onwards.%n",
                    saved.identity(), date);
        } catch (final IllegalArgumentException | IllegalStateException e) {
            System.out.println("Error: " + e.getMessage());
        }

        return false;
    }

    private LocalDate readDate(final String prompt) {
        while (true) {
            final String value = Console.readLine(prompt).trim();
            try {
                return LocalDate.parse(value);
            } catch (final DateTimeParseException e) {
                System.out.println("Invalid date format. Use YYYY-MM-DD (e.g. 2025-08-01).");
            }
        }
    }

    @Override
    public String headline() {
        return "Deactivate Flight Route";
    }
}
