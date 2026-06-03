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
 * <p>Phase 1 (walking skeleton) exposes {@code login}, {@code listFleet} and {@code exit}.
 * It has no dependency on any JPA or repository class — persistence is performed
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
     * Sends LOGIN and reads the server response.
     *
     * @return {@code true} if the server responded {@code OK}
     */
    public boolean login(final String username, final String password) throws IOException {
        out.println("LOGIN " + username + " " + password);
        return "OK".equals(in.readLine());
    }

    /**
     * Sends LIST_FLEET and reads the response (a count header followed by that many lines).
     *
     * @return one string per aircraft, as formatted by the server
     * @throws IOException if there is no response or the server returns an error
     */
    public List<String> listFleet() throws IOException {
        out.println("LIST_FLEET");
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
