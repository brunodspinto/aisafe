package aisafe.tcpserver.pilot;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PilotSessionHandler} (US086).
 * Uses in-memory streams to simulate TCP input/output without a real socket.
 */
class PilotSessionHandlerTest {

    private String runSession(final String input) throws IOException {
        final StringWriter sw = new StringWriter();
        final PrintWriter out = new PrintWriter(sw, true);
        final BufferedReader in = new BufferedReader(new StringReader(input));
        new PilotSessionHandler(in, out).handle();
        return sw.toString().trim();
    }

    @Test
    void ensureExitCommandReturnsBye() throws IOException {
        final String response = runSession("EXIT");
        assertEquals("BYE", response);
    }

    @Test
    void ensureUnknownCommandReturnsUnknownCommand() throws IOException {
        final String response = runSession("HELLO\nEXIT");
        assertTrue(response.contains("UNKNOWN_COMMAND"));
    }

    @Test
    void ensureMultipleUnknownCommandsAreEachRejected() throws IOException {
        final String response = runSession("FOO\nBAR\nEXIT");
        assertEquals(2, response.lines().filter(l -> l.equals("UNKNOWN_COMMAND")).count());
    }

    @Test
    void ensureCreateFlightPlanWithoutByteLengthReturnsError() throws IOException {
        final String response = runSession("CREATE_FLIGHT_PLAN\nEXIT");
        assertTrue(response.contains("ERROR"));
    }

    @Test
    void ensureCreateFlightPlanWithInvalidByteLengthReturnsError() throws IOException {
        final String response = runSession("CREATE_FLIGHT_PLAN abc\nEXIT");
        assertTrue(response.contains("ERROR"));
    }

    @Test
    void ensureCreateFlightPlanWithNegativeByteLengthReturnsError() throws IOException {
        final String response = runSession("CREATE_FLIGHT_PLAN -10\nEXIT");
        assertTrue(response.contains("ERROR"));
    }

    @Test
    void ensureSessionHandlesUnknownCommandBeforeExit() throws IOException {
        final String response = runSession("UNKNOWN\nEXIT");
        assertTrue(response.contains("UNKNOWN_COMMAND"));
        assertTrue(response.contains("BYE"));
    }

    @Test
    void ensureInsertWeatherDataWithoutArgsReturnsError() throws IOException {
        final String response = runSession("INSERT_WEATHER_DATA\nEXIT");
        assertTrue(response.contains("ERROR"));
    }

    @Test
    void ensureInsertWeatherDataWithMissingIdReturnsError() throws IOException {
        final String response = runSession("INSERT_WEATHER_DATA TP123\nEXIT");
        assertTrue(response.contains("ERROR"));
    }

    @Test
    void ensureInsertWeatherDataWithNonNumericIdReturnsError() throws IOException {
        final String response = runSession("INSERT_WEATHER_DATA TP123 abc\nEXIT");
        assertTrue(response.contains("ERROR"));
    }

    @Test
    void ensureInsertWeatherDataWithNegativeIdReturnsError() throws IOException {
        final String response = runSession("INSERT_WEATHER_DATA TP123 -5\nEXIT");
        assertTrue(response.contains("ERROR"));
    }

    @Test
    void ensureTestFlightPlanWithoutArgsReturnsError() throws IOException {
        final String response = runSession("TEST_FLIGHT_PLAN\nEXIT");
        assertTrue(response.contains("ERROR"));
    }
}
