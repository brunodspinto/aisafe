package aisafe.app.loggingserver.udp;

import aisafe.app.loggingserver.model.RemoteAccessEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogEventParserTest {

    @Test
    void ensureValidPayloadIsParsedIntoAllSixFields() {
        final String payload = "2026-06-04 17:00:00 | atcc1 | 10.0.0.5 | 50231 | US78 | LOGIN_SUCCESS";

        final Optional<RemoteAccessEvent> parsed = LogEventParser.parse(payload, "127.0.0.1");

        assertTrue(parsed.isPresent());
        final RemoteAccessEvent e = parsed.get();
        assertEquals(LocalDateTime.of(2026, 6, 4, 17, 0, 0), e.timestamp());
        assertEquals("atcc1", e.username());
        assertEquals("10.0.0.5", e.clientIp());
        assertEquals(50231, e.clientPort());
        assertEquals("US78", e.service());
        assertEquals("LOGIN_SUCCESS", e.event());
        assertEquals("127.0.0.1", e.sourceUdpIp());
    }

    @Test
    void ensureNullOrBlankPayloadIsRejected() {
        assertTrue(LogEventParser.parse(null, "1.2.3.4").isEmpty());
        assertTrue(LogEventParser.parse("   ", "1.2.3.4").isEmpty());
    }

    @Test
    void ensureWrongNumberOfFieldsIsRejected() {
        assertTrue(LogEventParser.parse("only | three | fields", null).isEmpty());
        assertTrue(LogEventParser.parse("a | b | c | d | e | f | g", null).isEmpty());
    }

    @Test
    void ensureEmptyMandatoryFieldsAreRejected() {
        assertTrue(LogEventParser.parse(
                "2026-06-04 17:00:00 |   | 10.0.0.5 | 50231 | US78 | LOGIN_SUCCESS", null).isEmpty());
        assertTrue(LogEventParser.parse(
                "2026-06-04 17:00:00 | atcc1 | 10.0.0.5 | 50231 |   | LOGIN_SUCCESS", null).isEmpty());
        assertTrue(LogEventParser.parse(
                "2026-06-04 17:00:00 | atcc1 | 10.0.0.5 | 50231 | US78 |   ", null).isEmpty());
    }

    @Test
    void ensureBadTimestampFallsBackToNowButStillParses() {
        final String payload = "not-a-timestamp | atcc1 | 10.0.0.5 | 50231 | US78 | LOGIN_SUCCESS";

        final Optional<RemoteAccessEvent> parsed = LogEventParser.parse(payload, null);

        assertTrue(parsed.isPresent());
        assertNotNull(parsed.get().timestamp());
    }

    @Test
    void ensureBadPortFallsBackToMinusOne() {
        final String payload = "2026-06-04 17:00:00 | atcc1 | 10.0.0.5 | NOT_A_PORT | US78 | LOGIN_SUCCESS";

        final Optional<RemoteAccessEvent> parsed = LogEventParser.parse(payload, null);

        assertTrue(parsed.isPresent());
        assertEquals(-1, parsed.get().clientPort());
    }

    @Test
    void ensureServiceIdentifierIsCarriedThroughForEachClient() {
        assertEquals("US44", LogEventParser.parse(
                "2026-06-04 17:00:00 | wp1 | 10.0.0.1 | 100 | US44 | LOGIN_SUCCESS", null).get().service());
        assertEquals("US86", LogEventParser.parse(
                "2026-06-04 17:00:00 | pilot1 | 10.0.0.2 | 200 | US86 | LOGIN_SUCCESS", null).get().service());
        assertFalse(LogEventParser.parse("", null).isPresent());
    }
}
