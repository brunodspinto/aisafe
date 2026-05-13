package aisafe.auth;

import aisafe.usermanagement.domain.AiSafeRoles;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

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

	private static final eapli.framework.infrastructure.authz.application.AuthorizationService AUTHZ =
			AuthzRegistry.authorizationService();

	/**
	 * Asserts that the current user may create flight plans (ADMIN or BACKOFFICE_OPERATOR).
	 *
	 * @throws UnauthorizedException if the user is not authenticated or lacks the required role
	 */
	public static void requireCanCreateFlightPlan() {
		if (!AuthenticationContext.isAuthenticated()) {
			throw new UnauthorizedException("User not authenticated. Cannot create flight plan.");
		}
		if (!AuthenticationContext.hasAnyRole(AiSafeRoles.ADMIN, AiSafeRoles.BACKOFFICE_OPERATOR)) {
			throw new UnauthorizedException("User does not have permission to create flight plans. " +
					"Required roles: ADMIN, BACKOFFICE_OPERATOR");
		}
		AUTHZ.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ADMIN, AiSafeRoles.BACKOFFICE_OPERATOR);
	}

	/**
	 * Asserts that the current user may read flight plans (any authenticated user).
	 *
	 * @throws UnauthorizedException if no session is active
	 */
	public static void requireCanReadFlightPlan() {
		if (!AuthenticationContext.isAuthenticated()) {
			throw new UnauthorizedException("User not authenticated. Cannot read flight plan.");
		}
	}

	/**
	 * Asserts that the current user may update flight plans (ADMIN or BACKOFFICE_OPERATOR).
	 *
	 * @throws UnauthorizedException if the user is not authenticated or lacks the required role
	 */
	public static void requireCanUpdateFlightPlan() {
		if (!AuthenticationContext.isAuthenticated()) {
			throw new UnauthorizedException("User not authenticated. Cannot update flight plan.");
		}
		if (!AuthenticationContext.hasAnyRole(AiSafeRoles.ADMIN, AiSafeRoles.BACKOFFICE_OPERATOR)) {
			throw new UnauthorizedException("User does not have permission to update flight plans. " +
					"Required roles: ADMIN, BACKOFFICE_OPERATOR");
		}
		AUTHZ.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ADMIN, AiSafeRoles.BACKOFFICE_OPERATOR);
	}

	/**
	 * Asserts that the current user may delete flight plans (ADMIN only).
	 *
	 * @throws UnauthorizedException if the user is not authenticated or lacks the ADMIN role
	 */
	public static void requireCanDeleteFlightPlan() {
		if (!AuthenticationContext.isAuthenticated()) {
			throw new UnauthorizedException("User not authenticated. Cannot delete flight plan.");
		}
		if (!AuthenticationContext.hasRole(AiSafeRoles.ADMIN)) {
			throw new UnauthorizedException("Only administrators can delete flight plans. " +
					"Required role: ADMIN");
		}
		AUTHZ.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ADMIN);
	}

	/**
	 * Asserts that the current user may approve flight plans (ADMIN, ATCC, or FLIGHT_CONTROL_OPERATOR).
	 *
	 * @throws UnauthorizedException if the user is not authenticated or lacks the required role
	 */
	public static void requireCanApproveFlightPlan() {
		if (!AuthenticationContext.isAuthenticated()) {
			throw new UnauthorizedException("User not authenticated. Cannot approve flight plan.");
		}
		if (!AuthenticationContext.hasAnyRole(AiSafeRoles.ADMIN, AiSafeRoles.ATCC,
				AiSafeRoles.FLIGHT_CONTROL_OPERATOR)) {
			throw new UnauthorizedException("User does not have permission to approve flight plans. " +
					"Required roles: ADMIN, ATCC, FLIGHT_CONTROL_OPERATOR");
		}
		AUTHZ.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ADMIN, AiSafeRoles.ATCC,
				AiSafeRoles.FLIGHT_CONTROL_OPERATOR);
	}

	private AuthorizationService() {}
}




