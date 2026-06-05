package aisafe.app.loggingserver.udp;

import aisafe.app.loggingserver.model.RemoteAccessEvent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Parses a received UDP payload into a {@link RemoteAccessEvent}.
 *
 * <p>Expected format (pipe-delimited, as produced by the client apps):
 * <pre>{@code <timestamp> | <username> | <clientIp> | <clientPort> | <service> | <event>}</pre>
 *
 * <p>Parsing is defensive: a malformed datagram yields {@link Optional#empty()} instead of
 * throwing, so one bad packet can never break the receive loop.
 */
public final class LogEventParser {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int FIELD_COUNT = 6;

    private LogEventParser() {}

    public static Optional<RemoteAccessEvent> parse(final String payload, final String sourceUdpIp) {
        if (payload == null || payload.isBlank()) {
            return Optional.empty();
        }

        final String[] f = payload.split("\\|");
        if (f.length != FIELD_COUNT) {
            return Optional.empty();
        }

        final String username = f[1].trim();
        final String clientIp = f[2].trim();
        final String service = f[4].trim();
        final String event = f[5].trim();
        if (username.isEmpty() || service.isEmpty() || event.isEmpty()) {
            return Optional.empty();
        }

        LocalDateTime timestamp;
        try {
            timestamp = LocalDateTime.parse(f[0].trim(), TS);
        } catch (final DateTimeParseException ex) {
            timestamp = LocalDateTime.now();
        }

        int clientPort;
        try {
            clientPort = Integer.parseInt(f[3].trim());
        } catch (final NumberFormatException ex) {
            clientPort = -1;
        }

        return Optional.of(new RemoteAccessEvent(
                timestamp, username, clientIp, clientPort, service, event, sourceUdpIp));
    }
}
