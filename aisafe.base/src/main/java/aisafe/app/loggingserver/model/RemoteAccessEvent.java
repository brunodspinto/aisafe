package aisafe.app.loggingserver.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * One remote-access event received from a client application (US044 / US078 / US086).
 *
 * <p>Mirrors the pipe-delimited UDP payload:
 * <pre>{@code <timestamp> | <username> | <clientIp> | <clientPort> | <service> | <event>}</pre>
 *
 * @param timestamp   the moment the event occurred (as reported by the client)
 * @param username    the username involved in the access
 * @param clientIp    the client's TCP IP address (taken from the payload, not the UDP packet)
 * @param clientPort  the client's TCP port (taken from the payload, not the UDP packet)
 * @param service     the originating service identifier ({@code US44}, {@code US78}, {@code US86})
 * @param event       the event name ({@code LOGIN_SUCCESS}, {@code LOGIN_FAILED}, {@code LOGOUT}, {@code CONNECTION_LOST})
 * @param sourceUdpIp the source IP of the UDP datagram (for verification only; may be {@code null})
 */
public record RemoteAccessEvent(
        LocalDateTime timestamp,
        String username,
        String clientIp,
        int clientPort,
        String service,
        String event,
        String sourceUdpIp) {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Identity of the remote session this event belongs to (one TCP connection). */
    public String sessionKey() {
        return username + "@" + clientIp + ":" + clientPort + "/" + service;
    }

    /** Canonical pipe-delimited line, identical to the wire format (used for the file and console). */
    public String toLogLine() {
        return String.join(" | ",
                timestamp.format(TS),
                username,
                clientIp,
                String.valueOf(clientPort),
                service,
                event);
    }
}
