package aisafe.app.logging;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Client-side emitter of remote-access events to the US090 Remote Accesses Logging Server,
 * using UDP datagrams (US044 / US078 / US086 → US090 dependency).
 *
 * <p>Shared by every remote-access client application; each client supplies its own
 * {@code serviceId} (e.g. {@code "US44"}, {@code "US78"}, {@code "US86"}).
 *
 * <p>The payload is ASCII, pipe-delimited:
 * <pre>{@code <timestamp> | <username> | <clientIP> | <clientPort> | <serviceId> | <EVENT>}</pre>
 *
 * <p>Sending is fire-and-forget: a fresh {@link DatagramSocket} is opened, the datagram is
 * sent, and the socket is closed. Any error is swallowed so that logging never blocks or
 * breaks the client.
 */
public final class RemoteAccessLogger {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String host;
    private final int port;
    private final String serviceId;

    /**
     * @param host      the US090 logging server host
     * @param port      the US090 logging server UDP port
     * @param serviceId the service identifier for this client app (e.g. {@code "US78"})
     */
    public RemoteAccessLogger(final String host, final int port, final String serviceId) {
        this.host = host;
        this.port = port;
        this.serviceId = serviceId;
    }

    /**
     * Builds the pipe-delimited payload and sends it as a UDP datagram (fire-and-forget).
     *
     * @param username   the username involved in the event
     * @param clientIp   the client's local IP address
     * @param clientPort the client's local TCP port
     * @param event      the event name (LOGIN_SUCCESS, LOGIN_FAILED, LOGOUT, CONNECTION_LOST)
     */
    public void log(final String username, final String clientIp, final int clientPort,
                    final String event) {
        final String payload = String.join(" | ",
                LocalDateTime.now().format(TIMESTAMP),
                username,
                clientIp,
                String.valueOf(clientPort),
                serviceId,
                event);
        try (DatagramSocket socket = new DatagramSocket()) {
            final byte[] data = payload.getBytes(StandardCharsets.US_ASCII);
            final InetAddress address = InetAddress.getByName(host);
            socket.send(new DatagramPacket(data, data.length, address, port));
        } catch (final Exception ignored) {
            // best-effort: never break the client because logging failed
        }
    }
}
