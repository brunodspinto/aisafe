package aisafe.app.weatherperson;

import aisafe.app.logging.RemoteAccessLogger;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * Standalone TCP client application for Weather Persons (US044).
 * Connects to the AISafe TCP server and exposes Weather Person commands interactively.
 *
 * <p>Remote-access events (login/logout/disconnect) are emitted to the US090 logging server
 * via UDP using {@link RemoteAccessLogger}, identifying this service as {@code US44}.
 */
public final class WeatherPersonTcpClientApp {

    /** US090 Remote Accesses Logging Server (UDP) — change to point at the cloud node. */
    private static final String LOG_HOST = "localhost";
    private static final int LOG_PORT = 9090;
    private static final String SERVICE_ID = "US44";

    private WeatherPersonTcpClientApp() {}

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

        try (final WeatherPersonTcpClient client = new WeatherPersonTcpClient(host, port)) {

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

    private static void showMenu(final Scanner scanner, final WeatherPersonTcpClient client)
            throws IOException {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("===== Weather Person Remote Menu =====");
            System.out.println(" 1 - Register weather data     (US041)");
            System.out.println(" 2 - Import bulk weather data  (US042)");
            System.out.println(" 3 - Consult weather data      (US043)");
            System.out.println(" 4 - List air control areas");
            System.out.println(" 0 - Exit");
            System.out.print("Option: ");

            final String option = scanner.nextLine().trim();
            switch (option) {
                case "1" -> doRegisterWeather(scanner, client);
                case "2" -> doImportBulk(scanner, client);
                case "3" -> doConsultWeather(scanner, client);
                case "4" -> doListAreas(client);
                case "0" -> {
                    client.exit();
                    running = false;
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    // -------------------------------------------------------------------------
    // US041 – Register weather data
    // -------------------------------------------------------------------------

    private static void doRegisterWeather(final Scanner scanner,
                                          final WeatherPersonTcpClient client) throws IOException {
        System.out.println("\n-- Register Weather Data (US041) --");

        System.out.print("Air Control Area code: ");
        final String areaCode = scanner.nextLine().trim();
        System.out.print("Provider (e.g. MeteoGroup): ");
        final String provider = scanner.nextLine().trim();
        System.out.print("Format (e.g. CSV): ");
        final String format = scanner.nextLine().trim();
        System.out.print("Date and time (ISO-8601, e.g. 2026-06-01T12:00:00): ");
        final String dateTime = scanner.nextLine().trim();
        System.out.print("Temperature (°C): ");
        final String temperature = scanner.nextLine().trim();
        System.out.print("Wind speed (km/h): ");
        final String windSpeed = scanner.nextLine().trim();
        System.out.print("Wind direction (e.g. NW): ");
        final String windDir = scanner.nextLine().trim();
        System.out.print("Pressure (hPa): ");
        final String pressure = scanner.nextLine().trim();
        System.out.print("Visibility (m): ");
        final String visibility = scanner.nextLine().trim();

        final String response = client.registerWeather(
                areaCode, provider, format, dateTime,
                temperature, windSpeed, windDir, pressure, visibility);

        if (response != null && response.startsWith("OK")) {
            System.out.println("Weather data registered. ID: " + response.substring(3).trim());
        } else {
            System.out.println("Failed: " + response);
        }
    }

    // -------------------------------------------------------------------------
    // US042 – Import bulk weather data
    // -------------------------------------------------------------------------

    private static void doImportBulk(final Scanner scanner,
                                     final WeatherPersonTcpClient client) throws IOException {
        System.out.println("\n-- Import Bulk Weather Data (US042) --");
        System.out.print("Path to CSV file: ");
        final String filePath = scanner.nextLine().trim();

        try {
            final String response = client.importBulk(filePath);
            if (response != null && response.startsWith("OK")) {
                System.out.println("Import complete: " + response.substring(3).trim());
            } else {
                System.out.println("Import failed: " + response);
            }
        } catch (final IOException e) {
            System.out.println("Error reading or sending file: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // US043 – Consult weather data
    // -------------------------------------------------------------------------

    private static void doConsultWeather(final Scanner scanner,
                                         final WeatherPersonTcpClient client) throws IOException {
        System.out.println("\n-- Consult Weather Data (US043) --");
        System.out.print("Date (ISO-8601, e.g. 2026-06-01): ");
        final String date = scanner.nextLine().trim();
        System.out.print("Air Control Area code: ");
        final String areaCode = scanner.nextLine().trim();

        final List<String> results = client.consultWeather(date, areaCode);
        if (results.isEmpty()) {
            System.out.println("No weather data found for the given date and area.");
        } else {
            results.forEach(System.out::println);
        }
    }

    // -------------------------------------------------------------------------
    // List Air Control Areas (helper)
    // -------------------------------------------------------------------------

    private static void doListAreas(final WeatherPersonTcpClient client) throws IOException {
        final List<String> areas = client.listAreas();
        if (areas.isEmpty()) {
            System.out.println("No air control areas registered.");
        } else {
            System.out.println("\nAvailable Air Control Areas:");
            areas.forEach(a -> System.out.println("  " + a));
        }
    }
}
