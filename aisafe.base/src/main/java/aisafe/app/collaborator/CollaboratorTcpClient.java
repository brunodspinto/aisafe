package aisafe.app.collaborator;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * TCP client for the Air Transport Company Collaborator remote application (US078).
 * Encapsulates protocol communication with the AISafe TCP server (Facade over the socket).
 *
 * <p>It has no dependency on any JPA or repository class — persistence is performed
 * exclusively server-side (AC078.2).
 */
public final class CollaboratorTcpClient implements Closeable {

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;

    public CollaboratorTcpClient(final String host, final int port) throws IOException {
        this.socket = new Socket(host, port);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    /**
     * Sends LOGIN (declaring the {@code ATCC} service) and reads the server response.
     *
     * @return {@code true} if the server responded {@code OK}
     */
    public boolean login(final String username, final String password) throws IOException {
        // Declare the requested service ("ATCC") so the shared server only authorizes
        // Air Transport Company collaborators on this app (US078, AC078.4).
        out.println("LOGIN " + username + " " + password + " ATCC");
        return "OK".equals(in.readLine());
    }

    /**
     * Sends LIST_FLEET and reads the count-delimited response.
     *
     * @return one string per aircraft, as formatted by the server
     */
    public List<String> listFleet() throws IOException {
        return readList("LIST_FLEET");
    }

    /**
     * Sends LIST_ROUTES and reads the count-delimited response.
     *
     * @return one string per active route, as formatted by the server
     */
    public List<String> listRoutes() throws IOException {
        return readList("LIST_ROUTES");
    }

    /**
     * Sends DEACTIVATE_ROUTE and returns the single-line server response.
     *
     * @param routeName the route to deactivate
     * @param date      the deactivation date (yyyy-MM-dd)
     * @return the server response line (e.g. {@code OK ...} or {@code ERROR ...})
     */
    public String deactivateRoute(final String routeName, final String date) throws IOException {
        out.println("DEACTIVATE_ROUTE " + routeName + " " + date);
        return in.readLine();
    }

    /**
     * Sends CREATE_ROUTE and returns the single-line server response.
     *
     * @param routeName   the new route name
     * @param origin      origin airport IATA code
     * @param destination destination airport IATA code
     * @return the server response line (e.g. {@code OK <routeName>} or {@code ERROR ...})
     */
    public String createRoute(final String routeName, final String origin, final String destination)
            throws IOException {
        out.println("CREATE_ROUTE " + routeName + ";" + origin + ";" + destination);
        return in.readLine();
    }

    /**
     * Sends EXIT and reads the BYE response.
     */
    public void exit() throws IOException {
        out.println("EXIT");
        in.readLine();
    }

    /** @return the local address of the TCP socket (used by the UDP access logger). */
    public InetAddress getLocalAddress() {
        return socket.getLocalAddress();
    }

    /** @return the local port of the TCP socket (used by the UDP access logger). */
    public int getLocalPort() {
        return socket.getLocalPort();
    }

    private List<String> readList(final String command) throws IOException {
        out.println(command);
        final String header = in.readLine();
        if (header == null) {
            throw new IOException("No response from server.");
        }
        if (!header.startsWith("OK")) {
            throw new IOException(header);
        }
        final int count = parseCount(header);
        final List<String> lines = new ArrayList<>(Math.max(0, count));
        for (int i = 0; i < count; i++) {
            lines.add(in.readLine());
        }
        return lines;
    }

    private static int parseCount(final String header) throws IOException {
        try {
            return Integer.parseInt(header.substring(2).trim());
        } catch (final NumberFormatException | IndexOutOfBoundsException e) {
            throw new IOException("Malformed server response: " + header);
        }
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
