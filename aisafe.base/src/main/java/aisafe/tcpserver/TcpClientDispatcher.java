package aisafe.tcpserver;

import aisafe.auth.AuthenticationContext;
import aisafe.tcpserver.collaborator.CollaboratorSessionHandler;
import aisafe.tcpserver.pilot.PilotSessionHandler;
import aisafe.usermanagement.domain.AiSafeRoles;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Handles one TCP client connection: authenticates the user, checks the role,
 * and delegates to the appropriate role-specific session handler.
 */
public final class TcpClientDispatcher implements Runnable {

    private final Socket socket;

    public TcpClientDispatcher(final Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (socket;
             final BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()));
             final PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            final String line = in.readLine();
            if (line == null || !line.startsWith("LOGIN ")) {
                out.println("FAIL expected LOGIN command");
                return;
            }

            final String[] parts = line.split(" ", 3);
            if (parts.length < 3) {
                out.println("FAIL usage: LOGIN <username> <password>");
                return;
            }

            final String username = parts[1];
            final String password = parts[2];

            if (!AuthenticationContext.authenticate(username, password)) {
                out.println("FAIL invalid credentials");
                return;
            }

            if (AuthenticationContext.hasRole(AiSafeRoles.PILOT)) {
                out.println("OK");
                new PilotSessionHandler(in, out).handle();
            } else if (AuthenticationContext.hasRole(AiSafeRoles.ATCC)) {
                out.println("OK");
                new CollaboratorSessionHandler(in, out).handle();
            } else {
                out.println("UNAUTHORIZED");
            }

        } catch (final IOException e) {
            System.err.println("[Dispatcher] Connection error: " + e.getMessage());
        } finally {
            AuthenticationContext.clear();
        }
    }
}
