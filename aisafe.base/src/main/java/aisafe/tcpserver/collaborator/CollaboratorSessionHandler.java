package aisafe.tcpserver.collaborator;

import aisafe.aircraft.application.ListFleetController;
import aisafe.aircraft.domain.Aircraft;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Handles the command loop for an authenticated Air Transport Company Collaborator (ATCC)
 * TCP session (US078). Each command is delegated to an existing application controller —
 * this class contains no business logic (it is a delivery-mechanism adapter).
 *
 * <p>Phase 1 (walking skeleton) supports {@code LIST_FLEET}, {@code EXIT} and the
 * {@code UNKNOWN_COMMAND} fallback. Further commands are added in later phases.
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
            if (command.equals("LIST_FLEET")) {
                handleListFleet();
            } else if (command.equals("EXIT")) {
                out.println("BYE");
                return;
            } else {
                out.println("UNKNOWN_COMMAND");
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

    private static String safeMessage(final Exception e) {
        final String msg = e.getMessage();
        return msg == null ? e.getClass().getSimpleName() : msg.replace('\n', ' ');
    }
}
