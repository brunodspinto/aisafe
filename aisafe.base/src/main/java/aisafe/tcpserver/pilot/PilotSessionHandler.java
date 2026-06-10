package aisafe.tcpserver.pilot;

import aisafe.flightplan.application.CreateFlightPlanFromFileController;
import aisafe.flightplan.application.InsertWeatherDataController;
import aisafe.flightplan.application.TestFlightPlanController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.StreamSupport;

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
            } else if (line.startsWith("INSERT_WEATHER_DATA")) {
                handleInsertWeatherData(line);
            } else if (line.startsWith("TEST_FLIGHT_PLAN")) {
                handleTestFlightPlan(line);
            } else if (line.equals("LIST_MY_PLANS")) {
                handleListMyPlans();
            } else if (line.equals("LIST_WEATHER_DATA")) {
                handleListWeatherData();
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

    private void handleInsertWeatherData(final String commandLine) {
        final String[] parts = commandLine.split(" ", 3);
        if (parts.length < 3) {
            out.println("ERROR usage: INSERT_WEATHER_DATA <designator> <weatherDataId>");
            return;
        }
        final long weatherDataId;
        try {
            weatherDataId = Long.parseLong(parts[2].trim());
            if (weatherDataId <= 0) throw new NumberFormatException();
        } catch (final NumberFormatException e) {
            out.println("ERROR invalid weather data id — must be a positive number");
            return;
        }
        try {
            final var plan = new InsertWeatherDataController()
                    .insertWeatherData(parts[1].trim(), weatherDataId);
            out.println("OK " + plan.identity());
        } catch (final Exception e) {
            out.println("ERROR " + e.getMessage().replace('\n', ' '));
        }
    }

    private void handleTestFlightPlan(final String commandLine) throws IOException {
        final String[] parts = commandLine.split(" ", 2);
        if (parts.length < 2) {
            out.println("ERROR usage: TEST_FLIGHT_PLAN <designator>");
            return;
        }
        try {
            final var plan = new TestFlightPlanController().testFlightPlan(parts[1].trim());
            out.println("OK " + plan.identity());
        } catch (final Exception e) {
            out.println("ERROR " + e.getMessage().replace('\n', ' '));
        }
    }

    private void handleListMyPlans() {
        try {
            final var list = StreamSupport.stream(
                    new InsertWeatherDataController().myFlightPlans().spliterator(), false).toList();
            out.println("OK " + list.size());
            for (final var plan : list) {
                out.println(plan.identity() + " " + plan.status());
            }
        } catch (final Exception e) {
            out.println("ERROR " + e.getMessage().replace('\n', ' '));
        }
    }

    private void handleListWeatherData() {
        try {
            final var list = StreamSupport.stream(
                    new InsertWeatherDataController().availableWeatherData().spliterator(), false).toList();
            out.println("OK " + list.size());
            for (final var wd : list) {
                out.println(wd.identity() + " " + wd.areaCode() + " " + wd.date());
            }
        } catch (final Exception e) {
            out.println("ERROR " + e.getMessage().replace('\n', ' '));
        }
    }
}
