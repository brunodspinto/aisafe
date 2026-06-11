package aisafe.app.console.presentation.flightplan;

import aisafe.aircraft.domain.Aircraft;
import aisafe.dsl.ast.FlightType;
import aisafe.flightplan.application.CreateFlightPlanController;
import aisafe.flightplan.domain.FlightPlan;
import aisafe.flightplan.domain.FlightPlanDesignator;
import aisafe.flightroute.domain.FlightRoute;
import aisafe.pilot.domain.Pilot;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Set;

/**
 * Console UI for the "Create a Flight Plan" use case (US080).
 * Guides the authenticated pilot through selecting a route, an aircraft and an assigned pilot
 * of their company, and entering the remaining flight plan data.
 */
public class CreateFlightPlanUI extends AbstractUI {

    private final CreateFlightPlanController controller = new CreateFlightPlanController();

    @Override
    protected boolean doShow() {
        try {
            final String routeName = selectRoute();
            if (routeName == null) return false;

            final String aircraftRegistration = selectAircraft();
            if (aircraftRegistration == null) return false;

            final Long assignedPilotId = selectPilot();
            if (assignedPilotId == null) return false;

            final FlightType flightType = selectFlightType();
            final String designator = readDesignator();
            if (designator == null) return false;
            final LocalDateTime departureDateTime = readDepartureDateTime();
            final double fuelAmount = readFuel();

            if (!confirmCreation(routeName, aircraftRegistration, assignedPilotId,
                    flightType, designator, departureDateTime, fuelAmount)) {
                System.out.println("  Operation cancelled.");
                return false;
            }

            final FlightPlan plan = controller.createFlightPlan(
                    routeName, aircraftRegistration, assignedPilotId,
                    flightType, designator, departureDateTime, fuelAmount);

            System.out.println("\n Flight plan successfully created!");
            System.out.println("  Designator : " + plan.designator());
            System.out.println("  Route      : " + plan.routeName());
            System.out.println("  Aircraft   : " + plan.aircraftRegistration());
            System.out.println("  Type       : " + plan.flightType());
            System.out.println("  Departure  : " + plan.departureDateTime());
            System.out.println("  Fuel       : " + plan.fuelQuantity());
            System.out.println("  Status     : " + plan.status());

        } catch (final IllegalArgumentException e) {
            System.out.println("\n Validation Error: " + e.getMessage());
        } catch (final IllegalStateException e) {
            System.out.println("\n Error: " + e.getMessage());
        } catch (final Exception e) {
            System.out.println("\n An error occurred: " + e.getMessage());
        }
        return false;
    }

    private String selectRoute() {
        System.out.println("\n--- Available Flight Routes ---");
        final Set<String> routeNames = new HashSet<>();
        for (final FlightRoute route : controller.availableRoutes()) {
            System.out.printf("  [%s] %s -> %s%n",
                    route.routeName(), route.originAirport(), route.destinationAirport());
            routeNames.add(route.routeName().toString());
        }
        if (routeNames.isEmpty()) {
            System.out.println("  No active routes available for your company.");
            return null;
        }
        while (true) {
            final String input = Console.readLine("Route name (or 0 to cancel): ").trim().toUpperCase();
            if (input.equals("0")) {
                System.out.println("  Operation cancelled.");
                return null;
            }
            if (routeNames.contains(input)) {
                return input;
            }
            System.out.println("  Unknown route. Please enter one of the listed route names (or 0 to cancel).");
        }
    }

    private String selectAircraft() {
        System.out.println("\n--- Available Aircraft ---");
        final Set<String> registrations = new HashSet<>();
        for (final Aircraft a : controller.availableAircraft()) {
            System.out.printf("  [%s] %s%n", a.registrationNumber(), a.aircraftModel().modelName());
            registrations.add(a.registrationNumber().toString());
        }
        if (registrations.isEmpty()) {
            System.out.println("  No active aircraft available for your company.");
            return null;
        }
        while (true) {
            final String input = Console.readLine("Aircraft registration (or 0 to cancel): ").trim().toUpperCase();
            if (input.equals("0")) {
                System.out.println("  Operation cancelled.");
                return null;
            }
            if (registrations.contains(input)) {
                return input;
            }
            System.out.println("  Unknown aircraft. Please enter one of the listed registrations (or 0 to cancel).");
        }
    }

    private Long selectPilot() {
        System.out.println("\n--- Available Pilots ---");
        final Set<Long> pilotIds = new HashSet<>();
        for (final Pilot p : controller.availablePilots()) {
            System.out.printf("  [%s] %s%n", p.identity(), p.user().systemUser().username());
            pilotIds.add(p.identity());
        }
        if (pilotIds.isEmpty()) {
            System.out.println("  No active pilots available for your company.");
            return null;
        }
        while (true) {
            final String input = Console.readLine("Assigned pilot id (or 0 to cancel): ").trim();
            if (input.equals("0")) {
                System.out.println("  Operation cancelled.");
                return null;
            }
            try {
                final Long id = Long.valueOf(input);
                if (pilotIds.contains(id)) {
                    return id;
                }
                System.out.println("  Unknown pilot id. Please enter one of the listed ids (or 0 to cancel).");
            } catch (final NumberFormatException e) {
                System.out.println("  Invalid id. Please enter a number (or 0 to cancel).");
            }
        }
    }

    private boolean confirmCreation(final String routeName, final String aircraftRegistration,
                                    final Long assignedPilotId, final FlightType flightType,
                                    final String designator, final LocalDateTime departureDateTime,
                                    final double fuelAmount) {
        System.out.println("\n--- Review Flight Plan ---");
        System.out.println("  Designator : " + designator);
        System.out.println("  Route      : " + routeName);
        System.out.println("  Aircraft   : " + aircraftRegistration);
        System.out.println("  Pilot id   : " + assignedPilotId);
        System.out.println("  Type       : " + flightType);
        System.out.println("  Departure  : " + departureDateTime);
        System.out.println("  Fuel       : " + fuelAmount + " kg");
        while (true) {
            final String answer = Console.readLine("Confirm creation? (y/n): ").trim().toLowerCase();
            if (answer.equals("y") || answer.equals("yes")) return true;
            if (answer.equals("n") || answer.equals("no")) return false;
            System.out.println("  Please answer 'y' or 'n'.");
        }
    }

    private FlightType selectFlightType() {
        System.out.println("\nFlight Type:");
        final FlightType[] types = FlightType.values();
        for (int i = 0; i < types.length; i++) {
            System.out.printf("  %d - %s%n", i + 1, types[i]);
        }
        while (true) {
            final int choice = Console.readInteger("Choice: ");
            if (choice >= 1 && choice <= types.length) {
                return types[choice - 1];
            }
            System.out.println("  Please choose a number between 1 and " + types.length + ".");
        }
    }

    /**
     * Reads the flight plan designator, validating its format against {@link FlightPlanDesignator}
     * on the spot and re-prompting until it is valid, or the operator cancels with 0.
     *
     * @return the validated (normalised) designator, or {@code null} if cancelled
     */
    private String readDesignator() {
        while (true) {
            final String input = Console.readLine(
                    "Flight plan designator (e.g. TP1234, or 0 to cancel): ").trim();
            if (input.equals("0")) {
                System.out.println("  Operation cancelled.");
                return null;
            }
            try {
                return FlightPlanDesignator.valueOf(input).toString();
            } catch (final IllegalArgumentException e) {
                System.out.println("  " + e.getMessage());
            }
        }
    }

    private LocalDateTime readDepartureDateTime() {
        while (true) {
            final String input = Console.readLine(
                    "Departure date/time (YYYY-MM-DDTHH:MM, e.g. 2026-07-01T14:30): ").trim();
            try {
                final LocalDateTime when = LocalDateTime.parse(input);
                if (when.isBefore(LocalDateTime.now())) {
                    System.out.println("  Departure date/time must be in the future.");
                    continue;
                }
                return when;
            } catch (final DateTimeParseException e) {
                System.out.println("  Invalid format. Use YYYY-MM-DDTHH:MM.");
            }
        }
    }

    private double readFuel() {
        while (true) {
            final String input = Console.readLine("Fuel quantity (kg): ").trim();
            try {
                final double fuel = Double.parseDouble(input);
                if (fuel > 0) {
                    return fuel;
                }
                System.out.println("  Fuel quantity must be strictly positive.");
            } catch (final NumberFormatException e) {
                System.out.println("  Invalid number. Please enter the fuel quantity in kg.");
            }
        }
    }

    @Override
    public String headline() {
        return "Create Flight Plan";
    }
}
