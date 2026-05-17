package aisafe.usermanagement.application;

import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafeRoles;
import aisafe.usermanagement.domain.Email;
import aisafe.usermanagement.domain.MecanographicNumber;
import aisafe.usermanagement.domain.SecurityClearance;
import aisafe.usermanagement.domain.User;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserManagementService;
import eapli.framework.infrastructure.authz.domain.model.Role;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.time.util.CurrentTimeCalendars;

import java.time.LocalDate;
import java.util.Set;

/**
 * Application-layer controller for the "Add User" use case (US031).
 * Delegates SystemUser creation to EAPLI's {@link UserManagementService} and persists
 * the AISafe {@link User} aggregate in the same transaction.
 * Requires the authenticated user to have the {@code ADMIN} role.
 */
@UseCaseController
public class AddUserController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final UserManagementService userSvc = AuthzRegistry.userService();

    /**
     * Returns the set of assignable roles that can be selected during user creation.
     *
     * @return array of available AISafe roles
     */
    public Role[] getRoleTypes() {
        return AiSafeRoles.nonUserValues();
    }

    /**
     * Registers a new system user via EAPLI's {@link UserManagementService} and persists
     * the associated {@link User} aggregate.
     *
     * @param username             login username
     * @param password             plaintext password (must satisfy the password policy)
     * @param firstName            user's first name
     * @param lastName             user's last name
     * @param emailStr             e-mail address string passed to the EAPLI service
     * @param roles                set of roles to assign (must not be empty)
     * @param phoneNumber          contact phone number
     * @param position             job position or title
     * @param email                {@link Email} value object stored on the AISafe user
     * @param securityClearance    security clearance level and expiration
     * @param skillsAssessmentDate date of the most recent skills assessment
     * @return the persisted {@link User} aggregate
     * @throws IllegalArgumentException if {@code roles} is null or empty
     */
    public User addUser(final String username, final String password,
                        final String firstName, final String lastName,
                        final String emailStr, final Set<Role> roles,
                        final String phoneNumber, final String position,
                        final Email email,
                        final SecurityClearance securityClearance,
                        final LocalDate skillsAssessmentDate) {

        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ADMIN);

        if (roles == null || roles.isEmpty())
            throw new IllegalArgumentException("At least one role must be assigned");

        final var tx = PersistenceContext.repositories().newTransactionalContext();
        final var userRepo = PersistenceContext.repositories().users(tx);

        tx.beginTransaction();
        try {
            final SystemUser systemUser = userSvc.registerNewUser(
                    username, password, firstName, lastName, emailStr, roles,
                    CurrentTimeCalendars.now());

            final long nextId = java.util.stream.StreamSupport
                    .stream(userRepo.findAll().spliterator(), false).count() + 1;
            final MecanographicNumber mecNumber =
                    MecanographicNumber.valueOf(String.format("EMP%05d", nextId));

            final User user = new User(systemUser, mecNumber, phoneNumber, email,
                    position, securityClearance, skillsAssessmentDate);

            final var savedUser = userRepo.save(user);
            tx.commit();
            return savedUser;
        } catch (final Exception e) {
            tx.rollback();
            throw e;
        }
    }
}
