package aisafe.app.loggingserver;

import aisafe.app.loggingserver.store.LogFileWriter;
import aisafe.app.loggingserver.store.RemoteAccessLogStore;
import aisafe.app.loggingserver.udp.LogEventParser;
import aisafe.app.loggingserver.model.RemoteAccessEvent;
import aisafe.app.loggingserver.udp.UdpLogReceiver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Entry point for the Remote Accesses Logging Server (US90 + US91).
 * Starts the UDP receiver (US90) and the HTTP visualization server (US91),
 * both sharing the same in-memory store.
 *
 * <p>Usage: {@code java ... aisafe.app.loggingserver.RemoteAccessLoggingServerApp [--port <port>] [--file <path>]}
 * (defaults: UDP port 9090, log file {@code logs.txt}).
 */
public final class RemoteAccessLoggingServerApp {

    private static final int DEFAULT_PORT = 9090;
    private static final String DEFAULT_FILE = "logs.txt";

    private RemoteAccessLoggingServerApp() {}

    public static void main(final String[] args) throws IOException {
        int port = DEFAULT_PORT;
        String filePath = DEFAULT_FILE;
        for (int i = 0; i + 1 < args.length; i += 2) {
            switch (args[i]) {
                case "--port" -> port = Integer.parseInt(args[i + 1]);
                case "--file" -> filePath = args[i + 1];
                default -> System.err.println("[Logging Server] Unknown argument: " + args[i]);
            }
        }

        final Path file = Path.of(filePath);
        final RemoteAccessLogStore store = new RemoteAccessLogStore();

        final int reloaded = reload(file, store);
        if (reloaded > 0) {
            System.out.println("[Logging Server] Reloaded " + reloaded + " event(s) from " + file.toAbsolutePath());
        }

        final LogFileWriter writer = new LogFileWriter(file);
        final UdpLogReceiver receiver = new UdpLogReceiver(port, store, writer);

        // US91 — HTTP server (shares the same store as the UDP receiver)
        final LoggingHttpServer httpServer = new LoggingHttpServer(store);
        try {
            httpServer.start();
            System.out.println("[US91] HTTP visualization server running on port " + LoggingHttpServer.HTTP_PORT);
        } catch (final IOException e) {
            System.err.println("[US91] Failed to start HTTP server: " + e.getMessage());
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            receiver.stop();
            try {
                writer.close();
            } catch (final IOException ignored) {
                // shutting down anyway
            }
            System.out.println("\n[Logging Server] Shutting down. " + store.size() + " event(s) recorded.");
        }));

        // US90 — UDP receiver runs in this thread (blocks until stopped)
        receiver.run();
    }

    private static int reload(final Path file, final RemoteAccessLogStore store) {
        if (!Files.exists(file)) {
            return 0;
        }
        int count = 0;
        try {
            for (final String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.isBlank()) continue;
                final Optional<RemoteAccessEvent> ev = LogEventParser.parse(line, null);
                if (ev.isPresent()) {
                    store.add(ev.get());
                    count++;
                }
            }
        } catch (final IOException ex) {
            System.err.println("[Logging Server] Could not reload " + file + ": " + ex.getMessage());
        }
        return count;
    }
}
