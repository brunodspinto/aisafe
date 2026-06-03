package aisafe.app.collaborator;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * Standalone TCP client application for Air Transport Company Collaborators (US078).
 * Connects to the AISafe TCP server and exposes ATCC commands interactively.
 *
 * <p>Phase 1 (walking skeleton): authentication + {@code List Fleet}. The UDP remote-access
 * logging and the remaining commands are added in later phases.
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
            System.out.println("0. Exit");
            System.out.print("Option: ");

            final String option = scanner.nextLine().trim();
            switch (option) {
                case "1" -> {
                    try {
                        final List<String> fleet = client.listFleet();
                        if (fleet.isEmpty()) {
                            System.out.println("No aircraft in your fleet.");
                        } else {
                            System.out.println("--- Fleet (" + fleet.size() + ") ---");
                            fleet.forEach(System.out::println);
                        }
                    } catch (final IOException e) {
                        System.out.println("Failed: " + e.getMessage());
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
