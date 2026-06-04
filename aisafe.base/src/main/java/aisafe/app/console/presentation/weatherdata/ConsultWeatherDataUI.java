package aisafe.app.console.presentation.weatherdata;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.weatherdata.application.ConsultWeatherDataController;
import aisafe.weatherdata.domain.WeatherData;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ConsultWeatherDataUI extends AbstractUI {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ConsultWeatherDataController controller =
            new ConsultWeatherDataController();

    @Override
    protected boolean doShow() {
        try {
            System.out.println("\n--- Available Air Control Areas ---");
            final Iterable<AirControlArea> areas = controller.activeAirControlAreas();
            boolean hasAreas = false;
            for (final AirControlArea area : areas) {
                System.out.printf("  [%s] %s%n", area.areaCode(), area.name());
                hasAreas = true;
            }

            if (!hasAreas) {
                System.out.println("No Air Control Areas registered.");
                return false;
            }

            final String areaCode = Console.readLine("\nArea Code: ");
            final LocalDate date = readDate();

            final Iterable<WeatherData> records =
                    controller.consultWeatherData(date, areaCode);

            printWeatherData(records);
        } catch (final IllegalArgumentException e) {
            System.out.println("\nValidation Error: " + e.getMessage());
        } catch (final Exception e) {
            System.out.println("\nUnable to consult weather data: " + e.getMessage());
        }

        return false;
    }

    private LocalDate readDate() {
        while (true) {
            try {
                return LocalDate.parse(
                        Console.readLine("Date (yyyy-MM-dd): ").trim(), DATE_FORMATTER);
            } catch (final DateTimeParseException e) {
                System.out.println("Invalid date format. Use yyyy-MM-dd.");
            }
        }
    }

    private void printWeatherData(final Iterable<WeatherData> records) {
        boolean hasRecords = false;
        System.out.println("\n--- Weather Data ---");
        System.out.printf("%-17s %-14s %10s %12s %-6s %12s %12s%n",
                "Date/Time", "Source", "Temp (C)", "Wind (km/h)", "Dir", "Pressure", "Visibility");
        System.out.println("-------------------------------------------------------------------------------------");

        for (final WeatherData record : records) {
            hasRecords = true;
            System.out.printf("%-17s %-14s %10.1f %12.1f %-6s %12.1f %12.1f%n",
                    record.date().format(DATE_TIME_FORMATTER),
                    record.source(),
                    record.temperature(),
                    record.windSpeed(),
                    record.windDirection(),
                    record.pressure(),
                    record.visibility());
        }

        if (!hasRecords) {
            System.out.println("No weather data found for the selected date and Air Control Area.");
        }
    }

    @Override
    public String headline() {
        return "Consult Weather Data";
    }
}
