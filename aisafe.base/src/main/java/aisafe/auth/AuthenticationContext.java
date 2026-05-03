package aisafe.auth;

import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;

import java.util.Optional;

/**
 * Adapter over EAPLI authentication context.
 */
public final class AuthenticationContext {

	/**
	 * Authenticate in AISafe using eAPLI infrastructure.
	 */
	public static boolean authenticate(final String username, final String password) {
		return AuthzRegistry.authenticationService().authenticate(username, password, (Role) null).isPresent();
	}

	/**
		 * Session creation must be done through authenticate(username, password).
	 */
	public static void setCurrentUser(final SystemUser user) {
		if (user == null) {
			clear();
			return;
		}
		if (currentUser().isPresent() && currentUser().get().equals(user)) {
			return;
		}
		throw new UnsupportedOperationException(
				"Use authenticate(username, password) to create a session for the intended user");
	}

	/**
	 * Get the currently authenticated user, if any.
	 */
	public static Optional<SystemUser> currentUser() {
		return AuthzRegistry.authorizationService().session().map(s -> s.authenticatedUser());
	}

	/**
	 * Check if a user is currently authenticated.
	 */
	public static boolean isAuthenticated() {
		return AuthzRegistry.authorizationService().session().isPresent();
	}

	/**
	 * Clear the current user. Call this on logout or session end.
	 */
	public static void clear() {
		AuthzRegistry.authorizationService().clearSession();
	}

	/**
	 * Check if current user has a specific role.
	 */
	public static boolean hasRole(final Role role) {
		if (role == null) {
			return false;
		}
		return AuthzRegistry.authorizationService().isAuthenticatedUserAuthorizedTo(role);
	}

	/**
	 * Check if current user has ANY of the given roles.
	 */
	public static boolean hasAnyRole(final Role... roles) {
		if (roles == null || roles.length == 0) {
			return false;
		}
		for (final Role role : roles) {
			if (hasRole(role)) {
				return true;
			}
		}
		return false;
	}

	private AuthenticationContext() {}
}



