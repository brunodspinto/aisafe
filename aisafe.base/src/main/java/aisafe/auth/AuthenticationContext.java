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
	 * Authenticates a user against the EAPLI infrastructure.
	 *
	 * @param username the login username
	 * @param password the plaintext password
	 * @return {@code true} if authentication succeeded; {@code false} otherwise
	 */
	public static boolean authenticate(final String username, final String password) {
		return AuthzRegistry.authenticationService().authenticate(username, password, (Role) null).isPresent();
	}

	/**
	 * Not supported — session creation must go through {@link #authenticate(String, String)}.
	 *
	 * @param user the user to set (ignored; pass {@code null} to clear the session)
	 * @throws UnsupportedOperationException always, unless {@code user} is null or equals the current user
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
	 * Returns the currently authenticated user, if any.
	 *
	 * @return an {@link Optional} containing the authenticated {@link SystemUser}, or empty if no session is active
	 */
	public static Optional<SystemUser> currentUser() {
		return AuthzRegistry.authorizationService().session().map(s -> s.authenticatedUser());
	}

	/**
	 * Checks whether a user is currently authenticated.
	 *
	 * @return {@code true} if an active session exists; {@code false} otherwise
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
	 * Checks whether the current user has the given role.
	 *
	 * @param role the role to check; returns {@code false} if {@code null}
	 * @return {@code true} if the authenticated user has the role; {@code false} otherwise
	 */
	public static boolean hasRole(final Role role) {
		if (role == null) {
			return false;
		}
		return AuthzRegistry.authorizationService().isAuthenticatedUserAuthorizedTo(role);
	}

	/**
	 * Checks whether the current user has at least one of the given roles.
	 *
	 * @param roles the roles to check; returns {@code false} if null or empty
	 * @return {@code true} if the authenticated user holds any of the specified roles; {@code false} otherwise
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



