package aisafe.tcpserver.collaborator;

import aisafe.aircraft.application.ListFleetController;
import aisafe.aircraft.domain.Aircraft;
import aisafe.flightroute.application.CreateFlightRouteController;
import aisafe.flightroute.application.DeactivateFlightRouteController;
import aisafe.flightroute.domain.FlightRoute;
import aisafe.flightroute.domain.RouteName;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the command loop for an authenticated Air Transport Company Collaborator (ATCC)
 * TCP session (US078). Each command is delegated to an existing application controller —
 * this class contains no business logic (it is a delivery-mechanism adapter).
 *
 * <p>Supported commands (Option A): {@code LIST_FLEET}, {@code LIST_ROUTES},
 * {@code DEACTIVATE_ROUTE}, {@code CREATE_ROUTE}, {@code EXIT}, with the
 * {@code UNKNOWN_COMMAND} fallback.
 */
public final class CollaboratorSessionHandler {

    private final BufferedReader in;
    private final PrintWriter out;

    public CollaboratorSessionHandler(final BufferedReader in, final PrintWriter out) {
        this.in = in;
        this.out = out;
    }

    /**
     * Reads and dispatches ATCC commands until EXIT or the connection closes.
     *
     * @throws IOException if reading from the stream fails
     */
    public void handle() throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
            final String command = line.trim();
            final String verb = command.split(" ", 2)[0];
            switch (verb) {
                case "LIST_FLEET" -> handleListFleet();
                case "LIST_ROUTES" -> handleListRoutes();
                case "DEACTIVATE_ROUTE" -> handleDeactivateRoute(command);
                case "CREATE_ROUTE" -> handleCreateRoute(command);
                case "EXIT" -> {
                    out.println("BYE");
                    return;
                }
                default -> out.println("UNKNOWN_COMMAND");
            }
        }
    }

    private void handleListFleet() {
        try {
            final List<Aircraft> fleet = new ListFleetController().companyFleet();
            out.println("OK " + fleet.size());
            for (final Aircraft a : fleet) {
                out.println(String.format("%s | %s | %s | %d | %s",
                        a.registrationNumber(),
                        a.aircraftModel().modelName(),
                        a.aircraftModel().makerName(),
                        a.yearOfManufacture(),
                        a.operationalStatus()));
            }
        } catch (final Exception e) {
            out.println("ERROR " + safeMessage(e));
        }
    }

    private void handleListRoutes() {
        try {
            final List<FlightRoute> routes = new ArrayList<>();
            new DeactivateFlightRouteController().activeRoutesByCompany().forEach(routes::add);
            out.println("OK " + routes.size());
            for (final FlightRoute r : routes) {
                out.println(String.format("%s | %s | %s | ACTIVE",
                        r.identity(),
                        r.originAirport().code(),
                        r.destinationAirport().code()));
            }
        } catch (final Exception e) {
            out.println("ERROR " + safeMessage(e));
        }
    }

    private void handleDeactivateRoute(final String commandLine) {
        final String[] parts = commandLine.split(" ");
        if (parts.length < 3) {
            out.println("ERROR usage: DEACTIVATE_ROUTE <routeName> <yyyy-MM-dd>");
            return;
        }
        try {
            final RouteName routeName = new RouteName(parts[1].trim());
            final LocalDate date = LocalDate.parse(parts[2].trim());
            final FlightRoute saved = new DeactivateFlightRouteController()
                    .deactivateFlightRoute(routeName, date);
            out.println("OK " + saved.identity() + " deactivated from " + date);
        } catch (final Exception e) {
            out.println("ERROR " + safeMessage(e));
        }
    }

    private void handleCreateRoute(final String commandLine) {
        final String[] cmd = commandLine.split(" ", 2);
        final String[] fields = cmd.length >= 2 ? cmd[1].split(";") : new String[0];
        if (fields.length < 3) {
            out.println("ERROR usage: CREATE_ROUTE <routeName>;<originIATA>;<destinationIATA>");
            return;
        }
        try {
            final FlightRoute saved = new CreateFlightRouteController()
                    .createFlightRoute(fields[0].trim(), fields[1].trim(), fields[2].trim());
            out.println("OK " + saved.identity());
        } catch (final Exception e) {
            out.println("ERROR " + safeMessage(e));
        }
    }

    private static String safeMessage(final Exception e) {
        final String msg = e.getMessage();
        return msg == null ? e.getClass().getSimpleName() : msg.replace('\n', ' ');
    }
}
