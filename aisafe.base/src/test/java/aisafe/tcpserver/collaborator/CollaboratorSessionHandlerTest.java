package aisafe.tcpserver.collaborator;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link CollaboratorSessionHandler} (US078).
 * Uses in-memory streams to simulate TCP input/output without a real socket or EAPLI context.
 * Controller-backed commands (e.g. LIST_FLEET) are exercised by integration and manual tests.
 */
class CollaboratorSessionHandlerTest {

    private String runSession(final String input) throws IOException {
        final StringWriter sw = new StringWriter();
        final PrintWriter out = new PrintWriter(sw, true);
        final BufferedReader in = new BufferedReader(new StringReader(input));
        new CollaboratorSessionHandler(in, out).handle();
        return sw.toString().trim();
    }

    @Test
    void ensureExitCommandReturnsBye() throws IOException {
        assertEquals("BYE", runSession("EXIT"));
    }

    @Test
    void ensureUnknownCommandReturnsUnknownCommand() throws IOException {
        assertTrue(runSession("HELLO\nEXIT").contains("UNKNOWN_COMMAND"));
    }

    @Test
    void ensureMultipleUnknownCommandsAreEachRejected() throws IOException {
        final String response = runSession("FOO\nBAR\nEXIT");
        assertEquals(2, response.lines().filter(l -> l.equals("UNKNOWN_COMMAND")).count());
    }

    @Test
    void ensureSessionHandlesUnknownCommandBeforeExit() throws IOException {
        final String response = runSession("UNKNOWN\nEXIT");
        assertTrue(response.contains("UNKNOWN_COMMAND"));
        assertTrue(response.contains("BYE"));
    }

    @Test
    void ensureDeactivateRouteWithoutArgumentsReturnsError() throws IOException {
        assertTrue(runSession("DEACTIVATE_ROUTE\nEXIT").contains("ERROR"));
    }

    @Test
    void ensureDeactivateRouteWithInvalidDateReturnsError() throws IOException {
        assertTrue(runSession("DEACTIVATE_ROUTE TP100 not-a-date\nEXIT").contains("ERROR"));
    }

    @Test
    void ensureCreateRouteWithMissingFieldsReturnsError() throws IOException {
        assertTrue(runSession("CREATE_ROUTE TP500;LIS\nEXIT").contains("ERROR"));
    }

    @Test
    void ensureFleetByModelWithoutArgumentReturnsError() throws IOException {
        assertTrue(runSession("LIST_FLEET_BY_MODEL\nEXIT").contains("ERROR"));
    }

    @Test
    void ensureFleetByCapacityWithInvalidNumberReturnsError() throws IOException {
        assertTrue(runSession("LIST_FLEET_BY_CAPACITY xyz\nEXIT").contains("invalid number"));
    }

    @Test
    void ensureFleetByAgeWithInvalidNumberReturnsError() throws IOException {
        assertTrue(runSession("LIST_FLEET_BY_AGE notayear\nEXIT").contains("invalid number"));
    }

    @Test
    void ensureDecommissionAircraftWithoutRegistrationReturnsError() throws IOException {
        assertTrue(runSession("DECOMMISSION_AIRCRAFT\nEXIT").contains("ERROR"));
    }

    @Test
    void ensureRemovePilotWithoutIdReturnsError() throws IOException {
        assertTrue(runSession("REMOVE_PILOT\nEXIT").contains("ERROR"));
    }

    @Test
    void ensureRemovePilotWithInvalidIdReturnsError() throws IOException {
        assertTrue(runSession("REMOVE_PILOT abc\nEXIT").contains("invalid pilot id"));
    }
}
