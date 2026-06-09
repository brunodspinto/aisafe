package aisafe.app.console.presentation.flightplan;

import aisafe.flightplan.application.InsertWeatherDataController;
import aisafe.flightplan.domain.FlightPlan;
import aisafe.weatherdata.domain.WeatherData;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.HashSet;
import java.util.Set;

/**
 * Console UI for the "Insert Weather Data in a Flight" use case (US082).
 * Lets the authenticated pilot attach an existing weather data record to one of their flight plans.
 */
public class InsertWeatherDataUI extends AbstractUI {

    private final InsertWeatherDataController controller = new InsertWeatherDataController();

    @Override
    protected boolean doShow() {
        try {
            final String designator = selectFlightPlan();
            if (designator == null) return false;

            final Long weatherDataId = selectWeatherData();
            if (weatherDataId == null) return false;

            final FlightPlan plan = controller.insertWeatherData(designator, weatherDataId);

            System.out.println("\n Weather data added to flight plan!");
            System.out.println("  Designator   : " + plan.designator());
            System.out.println("  Weather data : " + plan.weatherDataIds().size() + " record(s) attached");
            System.out.println("  Status       : " + plan.status());

        } catch (final IllegalArgumentException e) {
            System.out.println("\n Validation Error: " + e.getMessage());
        } catch (final IllegalStateException e) {
            System.out.println("\n Error: " + e.getMessage());
        } catch (final Exception e) {
            System.out.println("\n An error occurred: " + e.getMessage());
        }
        return false;
    }

    private String selectFlightPlan() {
        System.out.println("\n--- Your Flight Plans ---");
        final Set<String> designators = new HashSet<>();
        for (final FlightPlan plan : controller.myFlightPlans()) {
            System.out.printf("  [%s] %s (%s)%n", plan.designator(), plan.routeName(), plan.status());
            designators.add(plan.designator());
        }
        if (designators.isEmpty()) {
            System.out.println("  You have no flight plans.");
            return null;
        }
        while (true) {
            final String input = Console.readLine("Flight plan designator (or 0 to cancel): ").trim().toUpperCase();
            if (input.equals("0")) {
                System.out.println("  Operation cancelled.");
                return null;
            }
            if (designators.contains(input)) {
                return input;
            }
            System.out.println("  Unknown flight plan. Please enter one of the listed designators (or 0 to cancel).");
        }
    }

    private Long selectWeatherData() {
        System.out.println("\n--- Available Weather Data ---");
        final Set<Long> ids = new HashSet<>();
        for (final WeatherData wd : controller.availableWeatherData()) {
            System.out.printf("  [%s] area=%s date=%s temp=%.1f wind=%.1f %s%n",
                    wd.identity(), wd.areaCode(), wd.date(), wd.temperature(), wd.windSpeed(), wd.windDirection());
            ids.add(wd.identity());
        }
        if (ids.isEmpty()) {
            System.out.println("  No weather data registered.");
            return null;
        }
        while (true) {
            final String input = Console.readLine("Weather data id (or 0 to cancel): ").trim();
            if (input.equals("0")) {
                System.out.println("  Operation cancelled.");
                return null;
            }
            try {
                final Long id = Long.valueOf(input);
                if (ids.contains(id)) {
                    return id;
                }
                System.out.println("  Unknown weather data id. Please enter one of the listed ids (or 0 to cancel).");
            } catch (final NumberFormatException e) {
                System.out.println("  Invalid id. Please enter a number (or 0 to cancel).");
            }
        }
    }

    @Override
    public String headline() {
        return "Insert Weather Data in a Flight";
    }
}
