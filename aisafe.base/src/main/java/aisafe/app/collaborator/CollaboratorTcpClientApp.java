package aisafe.app.collaborator;

import aisafe.app.logging.RemoteAccessLogger;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * Standalone TCP client application for Air Transport Company Collaborators (US078).
 * Connects to the AISafe TCP server and exposes ATCC commands interactively.
 *
 * <p>Supported commands (Option A): List Fleet, List Routes, Deactivate Route, Create Route.
 * Remote-access events (login/logout/disconnect) are emitted to the US090 logging server
 * via UDP using {@link RemoteAccessLogger}.
 */
public final class CollaboratorTcpClientApp {

    /** US090 Remote Accesses Logging Server (UDP) — change to point at the cloud node. */
    private static final String LOG_HOST = "localhost";
    private static final int LOG_PORT = 9090;
    private static final String SERVICE_ID = "US78";

    private CollaboratorTcpClientApp() {}

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

        try (final CollaboratorTcpClient client = new CollaboratorTcpClient(host, port)) {

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

    private static void showMenu(final Scanner scanner, final CollaboratorTcpClient client) throws IOException {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("=== Air Transport Company Remote Menu ===");
            System.out.println("1. List Fleet");
            System.out.println("2. List Flight Routes");
            System.out.println("3. Deactivate Flight Route");
            System.out.println("4. Create Flight Route");
            System.out.println("0. Exit");
            System.out.print("Option: ");

            final String option = scanner.nextLine().trim();
            switch (option) {
                case "1" -> printList("Fleet", client.listFleet());
                case "2" -> printList("Routes", client.listRoutes());
                case "3" -> deactivateRoute(scanner, client);
                case "4" -> createRoute(scanner, client);
                case "0" -> {
                    client.exit();
                    running = false;
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private static void printList(final String title, final List<String> items) {
        if (items.isEmpty()) {
            System.out.println("No " + title.toLowerCase() + " found.");
        } else {
            System.out.println("--- " + title + " (" + items.size() + ") ---");
            items.forEach(System.out::println);
        }
    }

    private static void deactivateRoute(final Scanner scanner, final CollaboratorTcpClient client)
            throws IOException {
        System.out.print("Route name: ");
        final String route = scanner.nextLine().trim();
        System.out.print("Deactivation date (yyyy-MM-dd): ");
        final String date = scanner.nextLine().trim();
        System.out.println(client.deactivateRoute(route, date));
    }

    private static void createRoute(final Scanner scanner, final CollaboratorTcpClient client)
            throws IOException {
        System.out.print("Route name: ");
        final String route = scanner.nextLine().trim();
        System.out.print("Origin airport (IATA): ");
        final String origin = scanner.nextLine().trim();
        System.out.print("Destination airport (IATA): ");
        final String destination = scanner.nextLine().trim();
        System.out.println(client.createRoute(route, origin, destination));
    }
}
