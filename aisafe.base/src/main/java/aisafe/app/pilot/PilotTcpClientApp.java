package aisafe.app.pilot;

import java.io.IOException;
import java.util.Scanner;

/**
 * Standalone TCP client application for Pilots (US086).
 * Connects to the AISafe TCP server and exposes Pilot commands interactively.
 */
public final class PilotTcpClientApp {

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

        try (final PilotTcpClient client = new PilotTcpClient(host, port)) {

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
