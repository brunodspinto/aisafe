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
