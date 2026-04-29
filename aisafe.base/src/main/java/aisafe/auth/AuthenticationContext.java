package aisafe.auth;

import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import java.util.Optional;

/**
 * Manages the currently authenticated user in a thread-local context.
 *
 * Usage:
 *   AuthenticationContext.setCurrentUser(systemUser);  // After login
 *   AuthenticationContext.isAuthenticated();            // Check if logged in
 *   AuthenticationContext.clear();                      // On logout
 */
public final class AuthenticationContext {

	private static final ThreadLocal<SystemUser> CURRENT_USER = new ThreadLocal<>();

	/**
	 * Store the currently authenticated user for this thread.
	 * Call this after successful login.
	 * Pass null to clear the user.
	 */
	public static void setCurrentUser(final SystemUser user) {
		if (user == null) {
			CURRENT_USER.remove();
		} else {
			CURRENT_USER.set(user);
		}
	}

	/**
	 * Get the currently authenticated user, if any.
	 */
	public static Optional<SystemUser> currentUser() {
		return Optional.ofNullable(CURRENT_USER.get());
	}

	/**
	 * Check if a user is currently authenticated.
	 */
	public static boolean isAuthenticated() {
		return CURRENT_USER.get() != null;
	}

	/**
	 * Clear the current user. Call this on logout or session end.
	 */
	public static void clear() {
		CURRENT_USER.remove();
	}

	/**
	 * Check if current user has a specific role.
	 */
	public static boolean hasRole(final Role role) {
		return currentUser()
				.map(user -> user.hasAny(role))
				.orElse(false);
	}

	/**
	 * Check if current user has ANY of the given roles.
	 */
	public static boolean hasAnyRole(final Role... roles) {
		if (roles == null || roles.length == 0) {
			return false;
		}
		return currentUser()
				.map(user -> user.hasAny(roles))
				.orElse(false);
	}

	private AuthenticationContext() {}
}



