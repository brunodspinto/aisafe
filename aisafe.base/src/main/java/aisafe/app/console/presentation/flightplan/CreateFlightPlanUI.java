package aisafe.app.console.presentation.flightplan;

import aisafe.aircraft.domain.Aircraft;
import aisafe.dsl.ast.FlightType;
import aisafe.flightplan.application.CreateFlightPlanController;
import aisafe.flightplan.domain.FlightPlan;
import aisafe.flightroute.domain.FlightRoute;
import aisafe.pilot.domain.Pilot;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

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
            final String designator = Console.readLine("Flight plan designator (e.g. TP1234): ");
            final LocalDateTime departureDateTime = readDepartureDateTime();
            final double fuelAmount = readFuel();

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
        final List<FlightRoute> routes = new ArrayList<>();
        for (final FlightRoute route : controller.availableRoutes()) {
            System.out.printf("  [%s] %s -> %s%n",
                    route.routeName(), route.originAirport(), route.destinationAirport());
            routes.add(route);
        }
        if (routes.isEmpty()) {
            System.out.println("  No active routes available for your company.");
            return null;
        }
        return Console.readLine("Route name: ");
    }

    private String selectAircraft() {
        System.out.println("\n--- Available Aircraft ---");
        final List<Aircraft> aircraft = new ArrayList<>();
        for (final Aircraft a : controller.availableAircraft()) {
            System.out.printf("  [%s] %s%n", a.registrationNumber(), a.aircraftModel().modelName());
            aircraft.add(a);
        }
        if (aircraft.isEmpty()) {
            System.out.println("  No active aircraft available for your company.");
            return null;
        }
        return Console.readLine("Aircraft registration: ");
    }

    private Long selectPilot() {
        System.out.println("\n--- Available Pilots ---");
        final List<Pilot> pilots = new ArrayList<>();
        for (final Pilot p : controller.availablePilots()) {
            System.out.printf("  [%s] %s%n", p.identity(), p.user().systemUser().username());
            pilots.add(p);
        }
        if (pilots.isEmpty()) {
            System.out.println("  No active pilots available for your company.");
            return null;
        }
        return Console.readLong("Assigned pilot id: ");
    }

    private FlightType selectFlightType() {
        System.out.println("\nFlight Type:");
        final FlightType[] types = FlightType.values();
        for (int i = 0; i < types.length; i++) {
            System.out.printf("  %d - %s%n", i + 1, types[i]);
        }
        final int choice = Console.readInteger("Choice: ");
        return types[choice - 1];
    }

    private LocalDateTime readDepartureDateTime() {
        while (true) {
            try {
                return LocalDateTime.parse(
                        Console.readLine("Departure date/time (YYYY-MM-DDTHH:MM, e.g. 2026-07-01T14:30): ").trim());
            } catch (final DateTimeParseException e) {
                System.out.println("Invalid format. Use YYYY-MM-DDTHH:MM.");
            }
        }
    }

    private double readFuel() {
        return Console.readDouble("Fuel quantity (kg): ");
    }

    @Override
    public String headline() {
        return "Create Flight Plan";
    }
}
