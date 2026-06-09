package aisafe.usermanagement.application;

import aisafe.usermanagement.domain.AiSafePasswordPolicy;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthenticationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.validations.Preconditions;

/**
 * Application-layer controller for the "Change My Password" use case.
 * Lets the currently authenticated user change their own password. The change is delegated to
 * EAPLI's {@link AuthenticationService}, which verifies the current password, validates the new
 * password against the password policy, and persists the change.
 */
@UseCaseController
public class ChangePasswordController {

    private final AuthenticationService authenticationService = AuthzRegistry.authenticationService();
    private final AiSafePasswordPolicy passwordPolicy = new AiSafePasswordPolicy();

    /**
     * Checks whether the given password satisfies the system password policy
     * (minimum 6 characters, at least one digit and one uppercase letter).
     *
     * @param password the candidate password
     * @return {@code true} if the password is acceptable
     */
    public boolean isPasswordAcceptable(final String password) {
        return password != null && passwordPolicy.isSatisfiedBy(password);
    }

    /**
     * Changes the authenticated user's password.
     *
     * @param currentPassword the user's current password (must match)
     * @param newPassword      the new password (must satisfy the password policy)
     * @return {@code true} if the password was changed; {@code false} if there is no active session,
     *         the current password is incorrect, or the new password is not acceptable
     */
    public boolean changePassword(final String currentPassword, final String newPassword) {
        Preconditions.nonEmpty(currentPassword, "Current password must be provided.");
        Preconditions.nonEmpty(newPassword, "New password must be provided.");
        return authenticationService.changePassword(currentPassword, newPassword);
    }
}
