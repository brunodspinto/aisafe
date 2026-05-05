package aisafe.aircontrolarea.application;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.aircontrolarea.domain.GeoBoundary;
import aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;


/**
 * Controller responsible for the use case
 * "Register an Air Control Area" (US050).
 * This is where the UI interacts to create a new area.
 */

@UseCaseController
public class RegisterAirControlAreaController {

    // Get the authorization service (to ensure only authorized users can create)
    private final AuthorizationService authz = AuthzRegistry.authorizationService();

    // Get the repository through the factory
    private final AirControlAreaRepository repository =
            PersistenceContext.repositories().airControlAreas();

    /**
     * Registers a new Air Control Area in the system.
     * Note that the UI only passes primitive data types (Strings and doubles).
     * The Controller is responsible for building complex domain objects.
     */
    public AirControlArea registerAirControlArea(final String areaCode, final String name,
                                                 final double minimumFuelRequired,
                                                 final double northLat, final double southLat,
                                                 final double eastLong, final double westLong) {

        // Check permissions
                authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.BACKOFFICE_OPERATOR, AiSafeRoles.ADMIN);

                final String normalizedAreaCode = normalizeAreaCode(areaCode);
                if (repository.ofIdentity(normalizedAreaCode).isPresent()) {
                        throw new IllegalArgumentException("Air Control Area code already exists: " + normalizedAreaCode);
                }

        // Instantiate domain objects
        // First the Value Object
        final GeoBoundary boundaries =
                new GeoBoundary(northLat, southLat, eastLong, westLong);

        // Then the Aggregate Root entity
        final AirControlArea newArea =
                                new AirControlArea(normalizedAreaCode, name == null ? null : name.trim(), minimumFuelRequired, boundaries);

        // Save in the repository (persist to database)
        return repository.save(newArea);
    }

        private String normalizeAreaCode(final String areaCode) {
                if (areaCode == null) {
                        return null;
                }
                return areaCode.trim().toUpperCase();
        }
}
