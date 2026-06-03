package aisafe.tcpserver.pilot;

import aisafe.flightplan.application.CreateFlightPlanFromFileController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Handles the command loop for an authenticated Pilot TCP session.
 */
public final class PilotSessionHandler {

    private final BufferedReader in;
    private final PrintWriter out;

    public PilotSessionHandler(final BufferedReader in, final PrintWriter out) {
        this.in = in;
        this.out = out;
    }

    /**
     * Reads and dispatches Pilot commands until EXIT or the connection closes.
     */
    public void handle() throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
            if (line.startsWith("CREATE_FLIGHT_PLAN")) {
                handleCreateFlightPlan(line);
            } else if (line.equals("EXIT")) {
                out.println("BYE");
                return;
            } else {
                out.println("UNKNOWN_COMMAND");
            }
        }
    }

    private void handleCreateFlightPlan(final String commandLine) throws IOException {
        final String[] parts = commandLine.split(" ", 2);
        if (parts.length < 2) {
            out.println("ERROR usage: CREATE_FLIGHT_PLAN <charLength>");
            return;
        }

        final int byteLength;
        try {
            byteLength = Integer.parseInt(parts[1].trim());
            if (byteLength <= 0) throw new NumberFormatException();
        } catch (final NumberFormatException e) {
            out.println("ERROR invalid byte length");
            return;
        }

        final char[] buffer = new char[byteLength];
        int read = 0;
        while (read < byteLength) {
            final int n = in.read(buffer, read, byteLength - read);
            if (n == -1) {
                out.println("ERROR connection closed while reading DSL content");
                return;
            }
            read += n;
        }
        final String dslContent = new String(buffer);

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("aisafe-dsl-", ".dsl");
            Files.writeString(tempFile, dslContent);

            final var controller = new CreateFlightPlanFromFileController();
            final var flightPlan = controller.createFromFile(tempFile.toString());
            out.println("OK " + flightPlan.identity());

        } catch (final Exception e) {
            out.println("ERROR " + e.getMessage().replace('\n', ' '));
        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }
}
