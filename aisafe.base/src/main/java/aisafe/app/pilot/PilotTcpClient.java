package aisafe.app.pilot;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

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
     * Sends EXIT and reads the BYE response.
     */
    public void exit() throws IOException {
        out.println("EXIT");
        in.readLine();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
