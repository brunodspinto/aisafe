package aisafe.app.console.presentation.aircontrolarea;

import aisafe.aircontrolarea.application.RegisterAirControlAreaController;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

/**
 * Console UI for the "Register Air Control Area" use case (US064).
 * Collects area code, name, minimum fuel, and geographic boundaries, then delegates to the controller.
 */
public class RegisterAirControlAreaUI extends AbstractUI {

    // Instantiate the controller created earlier
    private final RegisterAirControlAreaController controller = new RegisterAirControlAreaController();

    @Override
    protected boolean doShow() {
        try {
            // Request basic Air Control Area data
            final String areaCode = Console.readLine("Area Code (e.g., PT-N): ");
            final String name = Console.readLine("Area Name: ");
            final double minimumFuel = Console.readDouble("Minimum Required Fuel: ");

            // Request geographic boundaries (coordinates)
            System.out.println("\n--- Geographic Boundaries (Coordinates) ---");
            final double northLat = Console.readDouble("North Latitude (e.g., 42.15): ");
            final double southLat = Console.readDouble("South Latitude (e.g., 36.95): ");
            final double eastLong = Console.readDouble("East Longitude (e.g., -6.18): ");
            final double westLong = Console.readDouble("West Longitude (e.g., -9.50): ");

            // Send data to the controller
            controller.registerAirControlArea(
                    areaCode, name, minimumFuel,
                    northLat, southLat, eastLong, westLong
            );

            // If no exception is thrown, registration was successful
            System.out.println("\n Air Control Area successfully registered!");

        } catch (final IllegalArgumentException e) {
            // Handles business rule validation errors (e.g., invalid coordinates, empty code)
            System.out.println("\n Validation Error: " + e.getMessage());
        } catch (final Exception e) {
            // Handles persistence errors (e.g., area already exists)
            System.out.println("\n An error occurred while registering the area: " + e.getMessage());
        }

        return false; // Return false to go back to the previous menu
    }

    @Override
    public String headline() {
        return "Register Air Control Area";
    }
}