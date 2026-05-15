package aisafe.airtransportcompany.application;

import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.airtransportcompany.domain.IATACode;
import aisafe.airtransportcompany.domain.ICAOCode;
import aisafe.airtransportcompany.repositories.AirTransportCompanyRepository;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

/**
 * Application-layer controller for the "Register Air Transport Company" use case (US060).
 * Only a Back-Office Operator may register companies.
 */
@UseCaseController
public class RegisterAirTransportCompanyController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final AirTransportCompanyRepository repo = PersistenceContext.repositories().airTransportCompanies();

    /**
     * Creates and persists a new air transport company.
     *
     * @param name     company name (non-blank, unique)
     * @param iataCode 2-letter IATA designator (unique)
     * @param icaoCode 2–3-letter ICAO designator (unique)
     * @return the saved {@link AirTransportCompany}
     * @throws IllegalArgumentException if any code format is invalid
     */
    public AirTransportCompany registerCompany(final String name, final String iataCode, final String icaoCode) {

        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.BACKOFFICE_OPERATOR);

        final IATACode iata = IATACode.valueOf(iataCode);
        final ICAOCode icao = ICAOCode.valueOf(icaoCode);
        final String trimmedName = name == null ? null : name.trim();

        if (repo.ofIdentity(iata).isPresent()) {
            throw new IllegalArgumentException(
                    "An air transport company with IATA code '" + iata + "' already exists.");
        }
        if (repo.findByIcaoCode(icao).isPresent()) {
            throw new IllegalArgumentException(
                    "An air transport company with ICAO code '" + icao + "' already exists.");
        }
        if (trimmedName != null && repo.findByName(trimmedName).isPresent()) {
            throw new IllegalArgumentException(
                    "An air transport company with name '" + trimmedName + "' already exists.");
        }

        final AirTransportCompany company = new AirTransportCompany(trimmedName, iata, icao);
        return repo.save(company);
    }
}
