package aisafe.tcpserver.weatherperson;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link WeatherPersonSessionHandler} (US044).
 * Uses in-memory streams to simulate TCP input/output without a real socket.
 */
class WeatherPersonSessionHandlerTest {

    private String runSession(final String input) throws IOException {
        final StringWriter sw = new StringWriter();
        final PrintWriter out = new PrintWriter(sw, true);
        final BufferedReader in = new BufferedReader(new StringReader(input));
        new WeatherPersonSessionHandler(in, out).handle();
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
    void ensureRegisterWeatherWithMissingParamsReturnsError() throws IOException {
        final String response = runSession("REGISTER_WEATHER\nEXIT");
        assertTrue(response.contains("ERROR"));
    }

    @Test
    void ensureImportBulkWithMissingLengthReturnsError() throws IOException {
        final String response = runSession("IMPORT_BULK\nEXIT");
        assertTrue(response.contains("ERROR"));
    }

    @Test
    void ensureImportBulkWithInvalidLengthReturnsError() throws IOException {
        final String response = runSession("IMPORT_BULK abc\nEXIT");
        assertTrue(response.contains("ERROR"));
    }

    @Test
    void ensureImportBulkWithNegativeLengthReturnsError() throws IOException {
        final String response = runSession("IMPORT_BULK -5\nEXIT");
        assertTrue(response.contains("ERROR"));
    }

    @Test
    void ensureConsultWeatherWithMissingParamsReturnsError() throws IOException {
        final String response = runSession("CONSULT_WEATHER\nEXIT");
        assertTrue(response.contains("ERROR"));
    }

    @Test
    void ensureSessionHandlesUnknownCommandBeforeExit() throws IOException {
        final String response = runSession("UNKNOWN\nEXIT");
        assertTrue(response.contains("UNKNOWN_COMMAND"));
        assertTrue(response.contains("BYE"));
    }
}
