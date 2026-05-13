package aisafe.app.console.presentation.airtransportcompany;

import aisafe.airtransportcompany.application.RegisterAirTransportCompanyController;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

/**
 * Console UI for the "Register Air Transport Company" use case (US058).
 * Validates IATA/ICAO format in the UI layer before delegating to the controller.
 */
public class RegisterAirTransportCompanyUI extends AbstractUI {

    private final RegisterAirTransportCompanyController controller =
            new RegisterAirTransportCompanyController();

    @Override
    protected boolean doShow() {
        final String name = readName();
        final String iata = readIataCode();
        final String icao = readIcaoCode();

        try {
            final var company = controller.registerCompany(name, iata, icao);
            System.out.println("Company '" + company + "' registered successfully.");
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            System.out.println("A company with that IATA code, ICAO code, or name already exists.");
        } catch (final IllegalArgumentException e) {
            System.out.println("Invalid data: " + e.getMessage());
        }

        return false;
    }

    /** Reads and validates the company name (at least 2 characters). */
    private String readName() {
        while (true) {
            final String value = Console.readLine("Company Name").trim();
            if (value.length() < 2) {
                System.out.println("Name must be at least 2 characters.");
            } else {
                return value;
            }
        }
    }

    /** Reads and validates the 2-letter IATA code. */
    private String readIataCode() {
        while (true) {
            final String value = Console.readLine("IATA Code (2 uppercase letters, e.g. TP)").trim();
            if (!value.matches("[A-Z]{2}")) {
                System.out.println("IATA code must be exactly 2 uppercase letters.");
            } else {
                return value;
            }
        }
    }

    /** Reads and validates the 2–3-letter ICAO code. */
    private String readIcaoCode() {
        while (true) {
            final String value = Console.readLine("ICAO Code (2-3 uppercase letters, e.g. TAP)").trim();
            if (!value.matches("[A-Z]{2,3}")) {
                System.out.println("ICAO code must be 2 or 3 uppercase letters.");
            } else {
                return value;
            }
        }
    }

    @Override
    public String headline() {
        return "Register Air Transport Company";
    }
}
