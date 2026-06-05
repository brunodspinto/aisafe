package aisafe.app.pilot;

import aisafe.app.logging.RemoteAccessLogger;

import java.io.IOException;
import java.util.Scanner;

/**
 * Standalone TCP client application for Pilots (US086).
 * Connects to the AISafe TCP server and exposes Pilot commands interactively.
 *
 * <p>Remote-access events (login/logout/disconnect) are emitted to the US090 logging server
 * via UDP using {@link RemoteAccessLogger}, identifying this service as {@code US86}.
 */
public final class PilotTcpClientApp {

    /** US090 Remote Accesses Logging Server (UDP) — change to point at the cloud node. */
    private static final String LOG_HOST = "localhost";
    private static final int LOG_PORT = 9090;
    private static final String SERVICE_ID = "US86";

    private PilotTcpClientApp() {}

    public static void main(final String[] args) {
        final Scanner scanner = new Scanner(System.in);

        System.out.print("Server host [localhost]: ");
        final String hostInput = scanner.nextLine().trim();
        final String host = hostInput.isEmpty() ? "localhost" : hostInput;

        System.out.print("Server port [9999]: ");
        final String portInput = scanner.nextLine().trim();
        final int port;
        try {
            port = portInput.isEmpty() ? 9999 : Integer.parseInt(portInput);
        } catch (final NumberFormatException e) {
            System.out.println("Invalid port.");
            return;
        }

        System.out.print("Username: ");
        final String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        final String password = scanner.nextLine().trim();

        final RemoteAccessLogger logger = new RemoteAccessLogger(LOG_HOST, LOG_PORT, SERVICE_ID);

        try (final PilotTcpClient client = new PilotTcpClient(host, port)) {

            final String clientIp = client.getLocalAddress().getHostAddress();
            final int clientPort = client.getLocalPort();

            if (!client.login(username, password)) {
                logger.log(username, clientIp, clientPort, "LOGIN_FAILED");
                System.out.println("Authentication failed.");
                return;
            }
            logger.log(username, clientIp, clientPort, "LOGIN_SUCCESS");

            System.out.println("Authenticated. Welcome, " + username + ".");

            try {
                showMenu(scanner, client);
                logger.log(username, clientIp, clientPort, "LOGOUT");
            } catch (final IOException e) {
                logger.log(username, clientIp, clientPort, "CONNECTION_LOST");
                System.out.println("Connection lost: " + e.getMessage());
            }

        } catch (final IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }

    private static void showMenu(final Scanner scanner, final PilotTcpClient client) throws IOException {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("=== Pilot Remote Menu ===");
            System.out.println("1. Create Flight Plan from DSL File");
            System.out.println("0. Exit");
            System.out.print("Option: ");

            final String option = scanner.nextLine().trim();
            switch (option) {
                case "1" -> {
                    System.out.print("DSL file path: ");
                    final String filePath = scanner.nextLine().trim();
                    try {
                        final String response = client.createFlightPlanFromFile(filePath);
                        if (response != null && response.startsWith("OK")) {
                            System.out.println("Flight plan created: " + response.substring(3).trim());
                        } else {
                            System.out.println("Failed: " + response);
                        }
                    } catch (final IOException e) {
                        System.out.println("Error reading file: " + e.getMessage());
                    }
                }
                case "0" -> {
                    client.exit();
                    running = false;
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }
}
