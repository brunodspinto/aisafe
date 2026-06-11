package aisafe.tcpserver.collaborator;

import aisafe.aircraft.application.DecommissionAircraftController;
import aisafe.aircraft.application.ListFleetController;
import aisafe.aircraft.domain.Aircraft;
import aisafe.flightroute.application.CreateFlightRouteController;
import aisafe.flightroute.application.DeactivateFlightRouteController;
import aisafe.flightroute.domain.FlightRoute;
import aisafe.flightroute.domain.RouteName;
import aisafe.pilot.application.ListPilotRosterController;
import aisafe.pilot.application.RemovePilotController;
import aisafe.pilot.domain.Pilot;

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
 * <p>Supported commands:
 * <ul>
 *   <li>Fleet (US072 / US072a-d): {@code LIST_FLEET}, {@code LIST_FLEET_BY_MODEL},
 *       {@code LIST_FLEET_BY_MAKER}, {@code LIST_FLEET_BY_CAPACITY}, {@code LIST_FLEET_BY_AGE}</li>
 *   <li>Aircraft (US071): {@code DECOMMISSION_AIRCRAFT}</li>
 *   <li>Routes (US073 / US074): {@code CREATE_ROUTE}, {@code LIST_ROUTES}, {@code DEACTIVATE_ROUTE}</li>
 *   <li>Pilots (US076 / US077): {@code LIST_PILOTS}, {@code REMOVE_PILOT}</li>
 *   <li>{@code EXIT}, with the {@code UNKNOWN_COMMAND} fallback.</li>
 * </ul>
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
                case "LIST_FLEET_BY_MODEL" -> handleFleetByModel(command);
                case "LIST_FLEET_BY_MAKER" -> handleFleetByMaker(command);
                case "LIST_FLEET_BY_CAPACITY" -> handleFleetByCapacity(command);
                case "LIST_FLEET_BY_AGE" -> handleFleetByAge(command);
                case "DECOMMISSION_AIRCRAFT" -> handleDecommissionAircraft(command);
                case "LIST_ROUTES" -> handleListRoutes();
                case "DEACTIVATE_ROUTE" -> handleDeactivateRoute(command);
                case "CREATE_ROUTE" -> handleCreateRoute(command);
                case "LIST_PILOTS" -> handleListPilots();
                case "REMOVE_PILOT" -> handleRemovePilot(command);
                case "EXIT" -> {
                    out.println("BYE");
                    return;
                }
                default -> out.println("UNKNOWN_COMMAND");
            }
        }
    }

    // ----- Fleet (US072 / US072a-d) -----

    private void handleListFleet() {
        try {
            writeFleet(new ListFleetController().companyFleet());
        } catch (final Exception e) {
            out.println("ERROR " + safeMessage(e));
        }
    }

    private void handleFleetByModel(final String commandLine) {
        final String model = argOf(commandLine);
        if (model == null) {
            out.println("ERROR usage: LIST_FLEET_BY_MODEL <modelName>");
            return;
        }
        try {
            writeFleet(new ListFleetController().fleetByModel(model));
        } catch (final Exception e) {
            out.println("ERROR " + safeMessage(e));
        }
    }

    private void handleFleetByMaker(final String commandLine) {
        final String maker = argOf(commandLine);
        if (maker == null) {
            out.println("ERROR usage: LIST_FLEET_BY_MAKER <makerName>");
            return;
        }
        try {
            writeFleet(new ListFleetController().fleetByMaker(maker));
        } catch (final Exception e) {
            out.println("ERROR " + safeMessage(e));
        }
    }

    private void handleFleetByCapacity(final String commandLine) {
        final String arg = argOf(commandLine);
        if (arg == null) {
            out.println("ERROR usage: LIST_FLEET_BY_CAPACITY <minSeats>");
            return;
        }
        final int minSeats;
        try {
            minSeats = Integer.parseInt(arg);
        } catch (final NumberFormatException e) {
            out.println("ERROR invalid number: " + arg);
            return;
        }
        try {
            writeFleet(new ListFleetController().fleetByMinCapacity(minSeats));
        } catch (final Exception e) {
            out.println("ERROR " + safeMessage(e));
        }
    }

    private void handleFleetByAge(final String commandLine) {
        final String arg = argOf(commandLine);
        if (arg == null) {
            out.println("ERROR usage: LIST_FLEET_BY_AGE <fromYear>");
            return;
        }
        final int fromYear;
        try {
            fromYear = Integer.parseInt(arg);
        } catch (final NumberFormatException e) {
            out.println("ERROR invalid number: " + arg);
            return;
        }
        try {
            writeFleet(new ListFleetController().fleetByManufactureYearFrom(fromYear));
        } catch (final Exception e) {
            out.println("ERROR " + safeMessage(e));
        }
    }

    /** Writes a fleet list in the {@code OK <count>} + one-line-per-aircraft protocol. */
    private void writeFleet(final List<Aircraft> fleet) {
        out.println("OK " + fleet.size());
        for (final Aircraft a : fleet) {
            out.println(String.format("%s | %s | %s | %d | %s",
                    a.registrationNumber(),
                    a.aircraftModel().modelName(),
                    a.aircraftModel().makerName(),
                    a.yearOfManufacture(),
                    a.operationalStatus()));
        }
    }

    // ----- Aircraft (US071) -----

    private void handleDecommissionAircraft(final String commandLine) {
        final String reg = argOf(commandLine);
        if (reg == null) {
            out.println("ERROR usage: DECOMMISSION_AIRCRAFT <registration>");
            return;
        }
        try {
            // Resolve from the authenticated collaborator's own fleet (enforces ownership),
            // then delegate the state change to the existing controller.
            final Aircraft target = new ListFleetController().companyFleet().stream()
                    .filter(a -> String.valueOf(a.registrationNumber()).equalsIgnoreCase(reg))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Aircraft '" + reg + "' not found in your fleet."));
            new DecommissionAircraftController().decommission(target);
            out.println("OK " + reg + " decommissioned");
        } catch (final Exception e) {
            out.println("ERROR " + safeMessage(e));
        }
    }

    // ----- Routes (US073 / US074) -----

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

    // ----- Pilots (US076 / US077) -----

    private void handleListPilots() {
        try {
            final List<Pilot> roster = new ListPilotRosterController().allPilots();
            out.println("OK " + roster.size());
            for (final Pilot p : roster) {
                // Only eagerly-loaded fields are read here (id, company, active) to stay safe
                // outside the persistence context. The id is what REMOVE_PILOT needs.
                out.println(String.format("%d | %s | %s",
                        p.identity(),
                        p.companyIataCode(),
                        p.isActive() ? "ACTIVE" : "INACTIVE"));
            }
        } catch (final Exception e) {
            out.println("ERROR " + safeMessage(e));
        }
    }

    private void handleRemovePilot(final String commandLine) {
        final String arg = argOf(commandLine);
        if (arg == null) {
            out.println("ERROR usage: REMOVE_PILOT <pilotId>");
            return;
        }
        final long pilotId;
        try {
            pilotId = Long.parseLong(arg);
        } catch (final NumberFormatException e) {
            out.println("ERROR invalid pilot id: " + arg);
            return;
        }
        try {
            final Pilot removed = new RemovePilotController().deactivatePilot(pilotId);
            out.println("OK pilot " + removed.identity() + " deactivated");
        } catch (final Exception e) {
            out.println("ERROR " + safeMessage(e));
        }
    }

    // ----- helpers -----

    /** @return the trimmed argument after the command verb, or {@code null} if absent/empty. */
    private static String argOf(final String commandLine) {
        final String[] parts = commandLine.split(" ", 2);
        return parts.length >= 2 && !parts[1].trim().isEmpty() ? parts[1].trim() : null;
    }

    private static String safeMessage(final Exception e) {
        final String msg = e.getMessage();
        return msg == null ? e.getClass().getSimpleName() : msg.replace('\n', ' ');
    }
}
