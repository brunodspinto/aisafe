package aisafe.auth;

import eapli.framework.infrastructure.authz.domain.model.Role;

/**
 * Re-exports roles from AiSafeRoles for convenient access in flight plan operations.
 * These roles are shared across the entire AISafe application.
 *
 * Roles defined in eapli.base/exemplo.core/usermanagement/domain/AiSafeRoles:
 * - ADMIN: System administrator with full system access
 * - BACKOFFICE_OPERATOR: Backoffice staff who manage administrative operations
 * - ATCC: Air Traffic Control Center personnel
 * - PILOT: Pilots who operate aircraft
 * - FLIGHT_CONTROL_OPERATOR: Flight control operators
 * - WEATHER_PERSON: Meteorology/weather personnel
 */
public final class FlightPlanRoles {

	/**
	 * ADMIN: Full system access to all flight plan operations.
	 */
	public static final Role ADMIN = Role.valueOf("ADMIN");

	/**
	 * BACKOFFICE_OPERATOR: Can manage administrative flight plan operations.
	 */
	public static final Role BACKOFFICE_OPERATOR = Role.valueOf("BACKOFFICE_OPERATOR");

	/**
	 * ATCC: Air Traffic Control Center - can approve and coordinate flight plans.
	 */
	public static final Role ATCC = Role.valueOf("ATCC");

	/**
	 * PILOT: Can view and interact with their own flight plans.
	 */
	public static final Role PILOT = Role.valueOf("PILOT");

	/**
	 * FLIGHT_CONTROL_OPERATOR: Flight control operator - can manage and approve flight plans.
	 */
	public static final Role FLIGHT_CONTROL_OPERATOR = Role.valueOf("FLIGHT_CONTROL_OPERATOR");

	/**
	 * WEATHER_PERSON: Meteorology personnel - can view flight plans for weather considerations.
	 */
	public static final Role WEATHER_PERSON = Role.valueOf("WEATHER_PERSON");

	/**
	 * Returns all available roles for the AISafe system.
	 *
	 * @return Array of all roles
	 */
	public static Role[] allRoles() {
		return new Role[] {
				ADMIN,
				BACKOFFICE_OPERATOR,
				ATCC,
				PILOT,
				FLIGHT_CONTROL_OPERATOR,
				WEATHER_PERSON
		};
	}

	private FlightPlanRoles() {
		// Prevent instantiation
	}
}


