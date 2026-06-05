package aisafe.app.loggingserver;

import java.io.IOException;

/**
 * Entry point for the Remote Accesses Logging Server (US90 + US91).
 * Starts the HTTP visualization server (US91) and prepares the UDP receiver stub (US90).
 */
public final class RemoteAccessLoggingServerApp {

    private RemoteAccessLoggingServerApp() {}

    public static void main(final String[] args) throws InterruptedException {
        final AccessEventStore store = AccessEventStore.getInstance();

        // US91 — HTTP server for log visualization
        final LoggingHttpServer httpServer = new LoggingHttpServer(store);
        try {
            httpServer.start();
            System.out.println("[Logging Server] HTTP running on port " + LoggingHttpServer.HTTP_PORT);
        } catch (final IOException e) {
            System.err.println("[Logging Server] Failed to start HTTP server: " + e.getMessage());
            return;
        }

        // US90 — UDP receiver (stub; teammate fills in the implementation)
        new UdpLogReceiver(store);

        Thread.currentThread().join();
    }
}
