package aisafe.app.loggingserver.udp;

import aisafe.app.loggingserver.store.LogFileWriter;
import aisafe.app.loggingserver.store.RemoteAccessLogStore;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;

/**
 * US90: Receives remote-access UDP datagrams and feeds them into the shared store and log file.
 *
 * <p>The receive loop is robust: each datagram is processed inside its own try/catch, so a
 * single malformed or oversized packet can never terminate the server.
 */
public final class UdpLogReceiver implements Runnable {

    private static final int BUFFER_SIZE = 2048;

    private final int port;
    private final RemoteAccessLogStore store;
    private final LogFileWriter fileWriter;

    private volatile boolean running = true;
    private volatile DatagramSocket socket;

    /**
     * @param port       the UDP port to listen on (default 9090)
     * @param store      the shared in-memory store
     * @param fileWriter the durable file writer (may be {@code null} to disable persistence)
     */
    public UdpLogReceiver(final int port, final RemoteAccessLogStore store, final LogFileWriter fileWriter) {
        this.port = port;
        this.store = store;
        this.fileWriter = fileWriter;
    }

    @Override
    public void run() {
        try (DatagramSocket ds = new DatagramSocket(port)) {
            this.socket = ds;
            System.out.println("[US90] Remote Accesses Logging Server listening on UDP " + port);
            final byte[] buffer = new byte[BUFFER_SIZE];
            while (running) {
                final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try {
                    ds.receive(packet);
                    handle(packet);
                } catch (final Exception e) {
                    if (running) {
                        System.err.println("[US90] Receive error: " + e.getMessage());
                    }
                }
            }
        } catch (final Exception e) {
            System.err.println("[US90] Could not bind UDP " + port + ": " + e.getMessage());
        }
    }

    private void handle(final DatagramPacket packet) {
        final String payload = new String(
                packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.US_ASCII);
        final String sourceUdpIp = packet.getAddress().getHostAddress();

        LogEventParser.parse(payload, sourceUdpIp).ifPresentOrElse(
                event -> {
                    store.add(event);
                    if (fileWriter != null) {
                        fileWriter.append(event);
                    }
                    System.out.println("[US90] " + event.toLogLine());
                },
                () -> System.err.println("[US90] Ignored malformed datagram from "
                        + sourceUdpIp + ": " + payload));
    }

    public void stop() {
        running = false;
        final DatagramSocket s = socket;
        if (s != null) {
            s.close();
        }
    }
}
