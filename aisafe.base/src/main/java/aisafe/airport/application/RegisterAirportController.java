package aisafe.airport.application;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.aircontrolarea.domain.AirControlAreaCode;
import aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import aisafe.airport.domain.Airport;
import aisafe.airport.domain.AirportIATACode;
import aisafe.airport.domain.AirportICAOCode;
import aisafe.airport.domain.GeoCoordinate;
import aisafe.airport.repositories.AirportRepository;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * Application-layer controller for the "Register Airport" use case (US052).
 * Requires a Back-Office Operator or Admin role.
 */
@UseCaseController
public class RegisterAirportController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AirportRepository airportRepository =
            PersistenceContext.repositories().airports();
    private final AirControlAreaRepository airControlAreaRepository =
            PersistenceContext.repositories().airControlAreas();

    /**
     * Returns all registered air control areas for selection in the UI.
     *
     * @return all {@link AirControlArea} instances
     */
    public Iterable<AirControlArea> allAirControlAreas() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.BACKOFFICE_OPERATOR, AiSafeRoles.ADMIN);
        return airControlAreaRepository.findAll();
    }

    /**
     * Validates and persists a new airport.
     * Enforces uniqueness of both IATA and ICAO codes.
     *
     * @param iataCodeStr 3-letter IATA code (will be trimmed and uppercased)
     * @param icaoCodeStr 4-letter ICAO code (will be trimmed and uppercased)
     * @param name        official airport name
     * @param town        town or city served
     * @param country     country of the airport
     * @param latitude    decimal latitude (-90 to 90)
     * @param longitude   decimal longitude (-180 to 180)
     * @param altitude    elevation in metres
     * @param areaCode    code of an existing {@link AirControlArea}
     * @return the saved {@link Airport}
     * @throws IllegalArgumentException if any code is duplicate or invalid, or the area does not exist
     */
    public Airport registerAirport(final String iataCodeStr,
                                   final String icaoCodeStr,
                                   final String name,
                                   final String town,
                                   final String country,
                                   final double latitude,
                                   final double longitude,
                                   final double altitude,
                                   final String areaCode) {

        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.BACKOFFICE_OPERATOR, AiSafeRoles.ADMIN);

        final AirportIATACode iataCode = AirportIATACode.valueOf(iataCodeStr.trim().toUpperCase());
        final AirportICAOCode icaoCode = AirportICAOCode.valueOf(icaoCodeStr.trim().toUpperCase());

        if (airportRepository.ofIdentity(iataCode).isPresent()) {
            throw new IllegalArgumentException(
                    "An airport with IATA code '" + iataCode + "' already exists.");
        }

        if (airportRepository.findByIcaoCode(icaoCode).isPresent()) {
            throw new IllegalArgumentException(
                    "An airport with ICAO code '" + icaoCode + "' already exists.");
        }

        final AirControlArea area = airControlAreaRepository.ofIdentity(AirControlAreaCode.valueOf(areaCode))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Air Control Area '" + areaCode + "' not found."));

        final GeoCoordinate location = new GeoCoordinate(latitude, longitude);

        final Airport airport = new Airport(
                iataCode, icaoCode, name.trim(), town.trim(), country.trim(),
                location, altitude, area
        );

        return airportRepository.save(airport);
    }
}
