package aisafe.app.logging.server;

import aisafe.app.logging.server.model.RemoteAccessEvent;
import aisafe.app.logging.server.store.LogFileWriter;
import aisafe.app.logging.server.store.RemoteAccessLogStore;
import aisafe.app.logging.server.udp.LogEventParser;
import aisafe.app.logging.server.udp.UdpLogReceiver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Entry point for the US090 Remote Accesses Logging Server.
 *
 * <p>Although it lives inside {@code aisafe.base} for project consistency, this server is
 * self-contained: it imports nothing from the AISafe domain or the EAPLI framework (pure JDK),
 * so it can be run on its own dedicated cloud node by launching only this {@code main}.
 *
 * <p>Usage: {@code java -cp target/classes aisafe.app.logging.server.LoggingServerApp [--port <port>] [--file <path>]}
 * (defaults: port 9090, file {@code logs.txt}).
 *
 * <p>On startup it reloads any previously persisted events, then runs the UDP receiver.
 * The shared {@link RemoteAccessLogStore} is the integration seam for the US091 HTTP/AJAX
 * visualization, which will be started in a separate thread sharing the same instance.
 */
public final class LoggingServerApp {

    private static final int DEFAULT_PORT = 9090;
    private static final String DEFAULT_FILE = "logs.txt";

    private LoggingServerApp() {}

    public static void main(final String[] args) throws IOException {
        int port = DEFAULT_PORT;
        String filePath = DEFAULT_FILE;
        for (int i = 0; i + 1 < args.length; i += 2) {
            switch (args[i]) {
                case "--port" -> port = Integer.parseInt(args[i + 1]);
                case "--file" -> filePath = args[i + 1];
                default -> System.err.println("[US90] Unknown argument: " + args[i]);
            }
        }

        final Path file = Path.of(filePath);
        final RemoteAccessLogStore store = new RemoteAccessLogStore();

        final int reloaded = reload(file, store);
        if (reloaded > 0) {
            System.out.println("[US90] Reloaded " + reloaded + " event(s) from " + file.toAbsolutePath());
        }

        final LogFileWriter writer = new LogFileWriter(file);
        final UdpLogReceiver receiver = new UdpLogReceiver(port, store, writer);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            receiver.stop();
            try {
                writer.close();
            } catch (final IOException ignored) {
                // shutting down anyway
            }
            System.out.println("\n[US90] Shutting down. " + store.size() + " event(s) recorded.");
        }));

        // The UDP receiver runs in this thread. US091 will start its HTTP server in a
        // separate thread, sharing this same `store` instance (the integration seam).
        receiver.run();
    }

    /**
     * Replays a previously written log file into the store so recent events and active
     * sessions survive a restart. Lines are read oldest-first so the newest ends up at the
     * front of the store.
     *
     * @return the number of events successfully reloaded
     */
    private static int reload(final Path file, final RemoteAccessLogStore store) {
        if (!Files.exists(file)) {
            return 0;
        }
        int count = 0;
        try {
            for (final String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                final Optional<RemoteAccessEvent> ev = LogEventParser.parse(line, null);
                if (ev.isPresent()) {
                    store.add(ev.get());
                    count++;
                }
            }
        } catch (final IOException ex) {
            System.err.println("[US90] Could not reload " + file + ": " + ex.getMessage());
        }
        return count;
    }
}
