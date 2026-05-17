package aisafe.app.console.presentation.airport;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.airport.application.RegisterAirportController;
import aisafe.airport.domain.Airport;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

/**
 * Console UI for the "Register Airport" use case (US052).
 * Collects airport details including IATA/ICAO codes, location, altitude, and air control area.
 */
public class RegisterAirportUI extends AbstractUI {

    private final RegisterAirportController controller = new RegisterAirportController();

    @Override
    protected boolean doShow() {
        try {
            System.out.println("\n--- Available Air Control Areas ---");
            final Iterable<AirControlArea> areas = controller.allAirControlAreas();
            boolean hasAreas = false;
            for (final AirControlArea area : areas) {
                System.out.printf("  [%s] %s%n", area.areaCode(), area.name());
                hasAreas = true;
            }
            if (!hasAreas) {
                System.out.println("  No air control areas registered. Please register one first.");
                return false;
            }

            System.out.println();
            final String iataCode = Console.readLine("IATA Code (3 uppercase letters, e.g. LIS): ");
            final String icaoCode = Console.readLine("ICAO Code (4 uppercase letters, e.g. LPPT): ");
            final String name = Console.readLine("Airport Name: ");
            final String town = Console.readLine("Town/City: ");
            final String country = Console.readLine("Country: ");
            final double latitude = Console.readDouble("Latitude (e.g. 38.7756): ");
            final double longitude = Console.readDouble("Longitude (e.g. -9.1354): ");
            final double altitude = Console.readDouble("Altitude (e.g 113): ");
            final String areaCode = Console.readLine("Air Control Area Code: ");

            final Airport airport = controller.registerAirport(
                    iataCode, icaoCode, name, town, country,
                    latitude, longitude, altitude, areaCode
            );

            System.out.println("\n Airport successfully registered!");
            System.out.println("  IATA       : " + airport.iataCode());
            System.out.println("  ICAO       : " + airport.icaoCode());
            System.out.println("  Name       : " + airport.name());
            System.out.println("  Town       : " + airport.town());
            System.out.println("  Country    : " + airport.country());
            System.out.println("  Location   : " + airport.location());
            System.out.println("  Altitude   : " + airport.altitude() + " m");
            System.out.println("  Area       : " + airport.airControlArea().areaCode());

        } catch (final IllegalArgumentException e) {
            System.out.println("\n Validation Error: " + e.getMessage());
        } catch (final Exception e) {
            System.out.println("\n An error occurred: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Register Airport";
    }
}
