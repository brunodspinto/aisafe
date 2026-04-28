package aisafe.auth;

/**
 * Authorization checks for flight plan operations.
 *
 * Every method checks if the current user has the required role(s) and throws
 * UnauthorizedException if they don't have permission.
 *
 * Role Matrix:
 * - CREATE:  ADMIN, BACKOFFICE_OPERATOR
 * - READ:    All authenticated users
 * - UPDATE:  ADMIN, BACKOFFICE_OPERATOR
 * - DELETE:  ADMIN only
 * - APPROVE: ADMIN, ATCC, FLIGHT_CONTROL_OPERATOR
 */
public final class AuthorizationService {

	/**
	 * Check: Can user create flight plans?
	 * Allowed: ADMIN, BACKOFFICE_OPERATOR
	 */
	public static void requireCanCreateFlightPlan() {
		if (!AuthenticationContext.isAuthenticated()) {
			throw new UnauthorizedException("User not authenticated. Cannot create flight plan.");
		}
		if (!AuthenticationContext.hasAnyRole(
				FlightPlanRoles.ADMIN,
				FlightPlanRoles.BACKOFFICE_OPERATOR)) {
			throw new UnauthorizedException("User does not have permission to create flight plans. " +
					"Required roles: ADMIN, BACKOFFICE_OPERATOR");
		}
	}

	/**
	 * Check: Can user read flight plans?
	 * Allowed: All authenticated users
	 */
	public static void requireCanReadFlightPlan() {
		if (!AuthenticationContext.isAuthenticated()) {
			throw new UnauthorizedException("User not authenticated. Cannot read flight plan.");
		}
	}

	/**
	 * Check: Can user update flight plans?
	 * Allowed: ADMIN, BACKOFFICE_OPERATOR
	 */
	public static void requireCanUpdateFlightPlan() {
		if (!AuthenticationContext.isAuthenticated()) {
			throw new UnauthorizedException("User not authenticated. Cannot update flight plan.");
		}
		if (!AuthenticationContext.hasAnyRole(
				FlightPlanRoles.ADMIN,
				FlightPlanRoles.BACKOFFICE_OPERATOR)) {
			throw new UnauthorizedException("User does not have permission to update flight plans. " +
					"Required roles: ADMIN, BACKOFFICE_OPERATOR");
		}
	}

	/**
	 * Check: Can user delete flight plans?
	 * Allowed: ADMIN only
	 */
	public static void requireCanDeleteFlightPlan() {
		if (!AuthenticationContext.isAuthenticated()) {
			throw new UnauthorizedException("User not authenticated. Cannot delete flight plan.");
		}
		if (!AuthenticationContext.hasRole(FlightPlanRoles.ADMIN)) {
			throw new UnauthorizedException("Only administrators can delete flight plans. " +
					"Required role: ADMIN");
		}
	}

	/**
	 * Check: Can user approve flight plans?
	 * Allowed: ADMIN, ATCC, FLIGHT_CONTROL_OPERATOR
	 */
	public static void requireCanApproveFlightPlan() {
		if (!AuthenticationContext.isAuthenticated()) {
			throw new UnauthorizedException("User not authenticated. Cannot approve flight plan.");
		}
		if (!AuthenticationContext.hasAnyRole(
				FlightPlanRoles.ADMIN,
				FlightPlanRoles.ATCC,
				FlightPlanRoles.FLIGHT_CONTROL_OPERATOR)) {
			throw new UnauthorizedException("User does not have permission to approve flight plans. " +
					"Required roles: ADMIN, ATCC, FLIGHT_CONTROL_OPERATOR");
		}
	}

	private AuthorizationService() {}
}




