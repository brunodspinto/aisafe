package aisafe.app.console.presentation.weatherdata;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.weatherdata.application.RegisterWeatherDataController;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class RegisterWeatherDataUI extends AbstractUI {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final RegisterWeatherDataController controller =
            new RegisterWeatherDataController();

    @Override
    protected boolean doShow() {
        try {
            // List available Air Control Areas for selection
            System.out.println("\n--- Available Air Control Areas ---");
            final Iterable<AirControlArea> areas = controller.activeAirControlAreas();
            boolean hasAreas = false;
            for (final AirControlArea area : areas) {
                System.out.printf("  [%s] %s%n", area.areaCode(), area.name());
                hasAreas = true;
            }

            if (!hasAreas) {
                System.out.println("No Air Control Areas registered. Please register one first.");
                return false;
            }

            final String areaCode = Console.readLine("\nArea Code: ");

            // Weather source
            System.out.println("\n--- Weather Source ---");
            final String provider = Console.readLine("Provider (e.g., IPMA): ");
            final String format = Console.readLine("Format (e.g., JSON): ");

            // Date and time
            final LocalDateTime date = readDateTime();

            // Meteorological readings
            System.out.println("\n--- Meteorological Data ---");
            final double temperature = Console.readDouble("Temperature (°C): ");
            final double windSpeed = Console.readDouble("Wind Speed (km/h): ");
            final String windDirection = Console.readLine("Wind Direction (e.g., N, NE, SW): ");
            final double pressure = Console.readDouble("Pressure (hPa): ");
            final double visibility = Console.readDouble("Visibility (km): ");

            controller.registerWeatherData(
                    areaCode, provider, format, date,
                    temperature, windSpeed, windDirection,
                    pressure, visibility);

            System.out.println("\nWeather data successfully registered!");

        } catch (final IllegalArgumentException e) {
            System.out.println("\nValidation Error: " + e.getMessage());
        } catch (final Exception e) {
            System.out.println("\nAn error occurred while registering weather data: " + e.getMessage());
        }

        return false;
    }

    private LocalDateTime readDateTime() {
        while (true) {
            try {
                return LocalDateTime.parse(
                        Console.readLine("\nDate and Time (yyyy-MM-dd HH:mm): ").trim(), FORMATTER);
            } catch (final DateTimeParseException e) {
                System.out.println("Invalid date format. Use yyyy-MM-dd HH:mm.");
            }
        }
    }

    @Override
    public String headline() {
        return "Register Weather Data";
    }
}
