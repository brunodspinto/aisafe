package aisafe.tcpserver;

import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafePasswordPolicy;
import aisafe.usermanagement.domain.AiSafeRoles;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;
import eapli.framework.infrastructure.authz.domain.model.Username;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link TcpClientDispatcher} (US086 / AC086.4).
 * Exercises the LOGIN phase over a real loopback TCP socket with EAPLI auth.
 */
class TcpClientDispatcherTest {

    private static final String PILOT_USERNAME   = "pilot-disp-test";
    private static final String ATCC_USERNAME    = "atcc-disp-test";
    private static final String WEATHER_USERNAME = "weather-disp-test";
    private static final String PASSWORD         = "Password1";

    private ServerSocket serverSocket;
    private Thread serverThread;
    private Socket clientSocket;
    private BufferedReader clientIn;
    private PrintWriter clientOut;

    @BeforeAll
    static void bootstrapAuth() {
        AuthzRegistry.configure(
                PersistenceContext.repositories().systemUsers(),
                new AiSafePasswordPolicy(),
                new PlainTextEncoder());

        ensureSystemUserExists(PILOT_USERNAME,   PASSWORD, AiSafeRoles.PILOT);
        ensureSystemUserExists(ATCC_USERNAME,    PASSWORD, AiSafeRoles.ATCC);
        ensureSystemUserExists(WEATHER_USERNAME, PASSWORD, AiSafeRoles.WEATHER_PERSON);
    }

    @BeforeEach
    void setUp() throws Exception {
        serverSocket = new ServerSocket(0);
        final int port = serverSocket.getLocalPort();

        serverThread = new Thread(() -> {
            try (final Socket conn = serverSocket.accept()) {
                new TcpClientDispatcher(conn).run();
            } catch (final Exception e) {
                // expected when test closes the socket
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        clientSocket = new Socket("localhost", port);
        clientIn  = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        clientOut = new PrintWriter(clientSocket.getOutputStream(), true);
    }

    @AfterEach
    void tearDown() throws Exception {
        clientSocket.close();
        serverSocket.close();
        serverThread.join(1000);
    }

    @Test
    void ensureInvalidCredentialsReturnFail() throws Exception {
        clientOut.println("LOGIN " + PILOT_USERNAME + " wrongpassword");
        assertTrue(clientIn.readLine().startsWith("FAIL"));
    }

    @Test
    void ensureNonPilotRoleReturnsUnauthorized() throws Exception {
        // ATCC user logs in without a service token — dispatcher treats as Pilot path → UNAUTHORIZED
        clientOut.println("LOGIN " + ATCC_USERNAME + " " + PASSWORD);
        assertEquals("UNAUTHORIZED", clientIn.readLine());
    }

    @Test
    void ensurePilotLoginReturnsOK() throws Exception {
        clientOut.println("LOGIN " + PILOT_USERNAME + " " + PASSWORD);
        assertEquals("OK", clientIn.readLine());
        clientOut.println("EXIT");
        assertEquals("BYE", clientIn.readLine());
    }

    @Test
    void ensureWeatherPersonLoginWithServiceTokenReturnsOK() throws Exception {
        clientOut.println("LOGIN " + WEATHER_USERNAME + " " + PASSWORD + " WEATHER");
        assertEquals("OK", clientIn.readLine());
        clientOut.println("EXIT");
        assertEquals("BYE", clientIn.readLine());
    }

    @Test
    void ensureWeatherPersonLoginWithoutServiceTokenReturnsUnauthorized() throws Exception {
        // WEATHER_PERSON user without WEATHER token — dispatcher treats as Pilot path → UNAUTHORIZED
        clientOut.println("LOGIN " + WEATHER_USERNAME + " " + PASSWORD);
        assertEquals("UNAUTHORIZED", clientIn.readLine());
    }

    @Test
    void ensureConcurrentSessionsGetCorrectResponses() throws Exception {
        // Two simultaneous connections: valid pilot + invalid credentials.
        // Without AUTH_LOCK the valid client's session can be clobbered; with it each
        // connection queues under the lock and always sees the correct identity.
        final CountDownLatch clientsConnected = new CountDownLatch(2);
        final CountDownLatch loginGate = new CountDownLatch(1);
        final AtomicReference<String> responseA = new AtomicReference<>();
        final AtomicReference<String> responseB = new AtomicReference<>();

        try (final ServerSocket concServer = new ServerSocket(0)) {
            final int port = concServer.getLocalPort();

            // accept 2 connections concurrently, each dispatched to its own thread
            final Thread acceptThread = new Thread(() -> {
                try {
                    for (int i = 0; i < 2; i++) {
                        final Socket conn = concServer.accept();
                        new Thread(new TcpClientDispatcher(conn)).start();
                    }
                } catch (final Exception ignored) {}
            });
            acceptThread.setDaemon(true);
            acceptThread.start();

            // Client A — valid pilot
            final Thread clientA = new Thread(() -> {
                try (final Socket s = new Socket("localhost", port);
                     final BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                     final PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {
                    clientsConnected.countDown();
                    loginGate.await();
                    out.println("LOGIN " + PILOT_USERNAME + " " + PASSWORD);
                    responseA.set(in.readLine());
                    out.println("EXIT");
                } catch (final Exception ignored) {}
            });

            // Client B — wrong password
            final Thread clientB = new Thread(() -> {
                try (final Socket s = new Socket("localhost", port);
                     final BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                     final PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {
                    clientsConnected.countDown();
                    loginGate.await();
                    out.println("LOGIN " + PILOT_USERNAME + " wrongpassword");
                    responseB.set(in.readLine());
                } catch (final Exception ignored) {}
            });

            clientA.start();
            clientB.start();
            clientsConnected.await(3, TimeUnit.SECONDS);  // both TCP connections established
            loginGate.countDown();                         // fire both LOGINs simultaneously
            clientA.join(5000);
            clientB.join(5000);
        }

        assertEquals("OK", responseA.get());
        assertTrue(responseB.get() != null && responseB.get().startsWith("FAIL"));
    }

    private static void ensureSystemUserExists(final String username, final String password,
                                               final Role role) {
        final var repo = PersistenceContext.repositories().systemUsers();
        if (repo.ofIdentity(Username.valueOf(username)).isPresent()) return;
        final var builder = new SystemUserBuilder(new AiSafePasswordPolicy(), new PlainTextEncoder());
        builder.withUsername(username).withPassword(password)
                .withName("Test", "User")
                .withEmail(username + "@aisafe.com")
                .withRoles(role);
        repo.save(builder.build());
    }
}
