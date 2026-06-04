package aisafe.app.collaborator;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * Standalone TCP client application for Air Transport Company Collaborators (US078).
 * Connects to the AISafe TCP server and exposes ATCC commands interactively.
 *
 * <p>Supported commands (Option A): List Fleet, List Routes, Deactivate Route, Create Route.
 * The UDP remote-access logging is added in a later phase.
 */
public final class CollaboratorTcpClientApp {

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

        try (final CollaboratorTcpClient client = new CollaboratorTcpClient(host, port)) {

            if (!client.login(username, password)) {
                System.out.println("Authentication failed.");
                return;
            }

            System.out.println("Authenticated. Welcome, " + username + ".");
            showMenu(scanner, client);

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
