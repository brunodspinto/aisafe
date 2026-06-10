package aisafe.app.pilot;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * TCP client for the Pilot remote application.
 * Encapsulates protocol communication with the AISafe TCP server.
 */
public final class PilotTcpClient implements Closeable {

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;

    public PilotTcpClient(final String host, final int port) throws IOException {
        this.socket = new Socket(host, port);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    /**
     * @return the local address of the underlying TCP socket (the client's IP)
     */
    public java.net.InetAddress getLocalAddress() {
        return socket.getLocalAddress();
    }

    /**
     * @return the local (client) TCP port of the underlying socket
     */
    public int getLocalPort() {
        return socket.getLocalPort();
    }

    /**
     * Sends LOGIN and reads the server response.
     *
     * @return {@code true} if the server responded {@code OK}
     */
    public boolean login(final String username, final String password) throws IOException {
        out.println("LOGIN " + username + " " + password);
        final String response = in.readLine();
        return response != null && response.equals("OK");
    }

    /**
     * Sends a DSL file to the server via the CREATE_FLIGHT_PLAN command.
     *
     * @param filePath path to the local DSL file
     * @return the server response line (e.g. {@code OK TP800} or {@code ERROR ...})
     */
    public String createFlightPlanFromFile(final String filePath) throws IOException {
        final String dslContent = Files.readString(Path.of(filePath));
        out.println("CREATE_FLIGHT_PLAN " + dslContent.length());
        out.print(dslContent);
        out.flush();
        return in.readLine();
    }

    /**
     * Lists the authenticated pilot's flight plans.
     *
     * @return list of "designator status" strings, one per plan (may be empty)
     */
    public List<String> listMyPlans() throws IOException {
        out.println("LIST_MY_PLANS");
        return readListResponse();
    }

    /**
     * Lists all weather data records available in the system.
     *
     * @return list of "id areaCode date" strings (may be empty)
     */
    public List<String> listWeatherData() throws IOException {
        out.println("LIST_WEATHER_DATA");
        return readListResponse();
    }

    /**
     * Attaches existing weather data to one of the pilot's flight plans (US082).
     *
     * @param designator    the flight plan designator
     * @param weatherDataId the id of an existing weather data record
     * @return the server response line (e.g. {@code OK TP800} or {@code ERROR ...})
     */
    public String insertWeatherData(final String designator, final long weatherDataId) throws IOException {
        out.println("INSERT_WEATHER_DATA " + designator + " " + weatherDataId);
        return in.readLine();
    }

    /**
     * Requests simulation testing of a validated flight plan (US085).
     * Note: may take up to 30 seconds while the C binary runs.
     *
     * @param designator the flight plan designator
     * @return the server response line (e.g. {@code OK TP800} or {@code ERROR ...})
     */
    public String testFlightPlan(final String designator) throws IOException {
        out.println("TEST_FLIGHT_PLAN " + designator);
        return in.readLine();
    }

    /**
     * Sends EXIT and reads the BYE response.
     */
    public void exit() throws IOException {
        out.println("EXIT");
        in.readLine();
    }

    private List<String> readListResponse() throws IOException {
        final String header = in.readLine();
        if (header == null || header.startsWith("ERROR")) {
            return List.of();
        }
        final String[] parts = header.split(" ", 2);
        final int count;
        try {
            count = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
        } catch (final NumberFormatException e) {
            return List.of();
        }
        final List<String> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            final String line = in.readLine();
            if (line != null) result.add(line);
        }
        return result;
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
