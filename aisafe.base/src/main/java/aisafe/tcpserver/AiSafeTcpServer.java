package aisafe.tcpserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * TCP server for remote access (US086 / US044 / US078).
 * Accepts one connection per thread via {@link TcpClientDispatcher}.
 */
public final class AiSafeTcpServer {

    private AiSafeTcpServer() {}

    /**
     * Starts the TCP server on the given port in the current thread.
     * Blocks indefinitely, accepting connections.
     *
     * @param port the port to listen on
     */
    public static void start(final int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[TCP Server] Listening on port " + port);
            while (!Thread.currentThread().isInterrupted()) {
                final Socket clientSocket = serverSocket.accept();
                final Thread dispatcher = new Thread(new TcpClientDispatcher(clientSocket));
                dispatcher.setDaemon(true);
                dispatcher.start();
            }
        } catch (final IOException e) {
            System.err.println("[TCP Server] Error: " + e.getMessage());
        }
    }
}
