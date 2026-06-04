package aisafe.tcpserver.collaborator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Implementation tests for {@link CollaboratorSessionHandler} (US078).
 * Exercises the command protocol over a real loopback TCP socket.
 *
 * <p>Only protocol-level behaviour that does not require an authenticated EAPLI session is
 * exercised here (EXIT, UNKNOWN_COMMAND, argument validation). Controller-backed happy paths
 * are validated by manual integration tests.
 */
class CollaboratorSessionHandlerIT {

    private ServerSocket serverSocket;
    private Thread serverThread;
    private Socket clientSocket;
    private BufferedReader clientIn;
    private PrintWriter clientOut;

    @BeforeEach
    void setUp() throws Exception {
        serverSocket = new ServerSocket(0);
        final int port = serverSocket.getLocalPort();

        serverThread = new Thread(() -> {
            try (final Socket conn = serverSocket.accept();
                 final BufferedReader in = new BufferedReader(
                         new InputStreamReader(conn.getInputStream()));
                 final PrintWriter out = new PrintWriter(conn.getOutputStream(), true)) {
                new CollaboratorSessionHandler(in, out).handle();
            } catch (final Exception e) {
                // connection closed by test — expected
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        clientSocket = new Socket("localhost", port);
        clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        clientOut = new PrintWriter(clientSocket.getOutputStream(), true);
    }

    @AfterEach
    void tearDown() throws Exception {
        clientSocket.close();
        serverSocket.close();
        serverThread.join(1000);
    }

    @Test
    void ensureExitOverRealSocketReturnsBye() throws Exception {
        clientOut.println("EXIT");
        assertEquals("BYE", clientIn.readLine());
    }

    @Test
    void ensureUnknownCommandOverRealSocketReturnsUnknownCommand() throws Exception {
        clientOut.println("HELLO");
        assertEquals("UNKNOWN_COMMAND", clientIn.readLine());
        clientOut.println("EXIT");
        assertEquals("BYE", clientIn.readLine());
    }

    @Test
    void ensureDeactivateRouteWithInvalidDateOverRealSocketReturnsError() throws Exception {
        clientOut.println("DEACTIVATE_ROUTE TP100 not-a-date");
        assertTrue(clientIn.readLine().startsWith("ERROR"));
        clientOut.println("EXIT");
        assertEquals("BYE", clientIn.readLine());
    }

    @Test
    void ensureCreateRouteWithMissingFieldsOverRealSocketReturnsError() throws Exception {
        clientOut.println("CREATE_ROUTE TP500;LIS");
        assertTrue(clientIn.readLine().startsWith("ERROR"));
        clientOut.println("EXIT");
        assertEquals("BYE", clientIn.readLine());
    }

    @Test
    void ensureMultipleCommandsOverRealSocketAreHandledInSequence() throws Exception {
        clientOut.println("FOO");
        assertEquals("UNKNOWN_COMMAND", clientIn.readLine());
        clientOut.println("BAR");
        assertEquals("UNKNOWN_COMMAND", clientIn.readLine());
        clientOut.println("EXIT");
        assertEquals("BYE", clientIn.readLine());
    }
}
