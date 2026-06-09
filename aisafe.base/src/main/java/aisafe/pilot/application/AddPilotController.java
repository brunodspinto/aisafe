package aisafe.pilot.application;

import aisafe.aircraftmodel.domain.AircraftModel;
import aisafe.aircraftmodel.repositories.AircraftModelRepository;
import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.airtransportcompany.repositories.AirTransportCompanyRepository;
import aisafe.collaborator.repositories.CollaboratorRepository;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.pilot.domain.Pilot;
import aisafe.pilot.repositories.PilotRepository;
import aisafe.usermanagement.domain.AiSafePasswordPolicy;
import aisafe.usermanagement.domain.AiSafeRoles;
import aisafe.usermanagement.domain.Email;
import aisafe.usermanagement.domain.MecanographicNumber;
import aisafe.usermanagement.domain.SecurityClearance;
import aisafe.usermanagement.domain.User;
import aisafe.usermanagement.repositories.UserRepository;
import eapli.framework.application.UseCaseController;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserManagementService;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.time.util.CurrentTimeCalendars;

import java.time.LocalDate;
import java.util.Set;

/**
 * Application-layer controller for the "Add a Pilot" use case (US075).
 * The authenticated Air Transport Company Collaborator (ATCC) adds a pilot to their own company.
 * The pilot is created as a system user with the PILOT role and must be certified for
 * one or more existing aircraft models.
 */
@UseCaseController
public class AddPilotController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();

    private final TransactionalContext tx =
            PersistenceContext.repositories().newTransactionalContext();
    private final eapli.framework.infrastructure.authz.domain.repositories.UserRepository systemUserRepo =
            PersistenceContext.repositories().systemUsers(tx);
    private final UserManagementService txUserSvc =
            new UserManagementService(systemUserRepo, new AiSafePasswordPolicy(), new PlainTextEncoder());
    private final UserRepository userRepo =
            PersistenceContext.repositories().users(tx);
    private final PilotRepository pilotRepo =
            PersistenceContext.repositories().pilots(tx);
    private final AircraftModelRepository modelRepo =
            PersistenceContext.repositories().aircraftModels();
    private final AirTransportCompanyRepository companyRepo =
            PersistenceContext.repositories().airTransportCompanies();
    private final CollaboratorRepository collaboratorRepo =
            PersistenceContext.repositories().collaborators();

    /**
     * Returns all registered aircraft models so the UI can offer them for certification.
     *
     * @return all {@link AircraftModel} instances
     */
    public Iterable<AircraftModel> allAircraftModels() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ATCC);
        return modelRepo.findAll();
    }

    /**
     * Creates a new system user with the PILOT role and registers them as a pilot of the
     * authenticated collaborator's company, certified for the given aircraft models.
     *
     * @param username                  system username
     * @param password                  system password
     * @param firstName                 first name
     * @param lastName                  last name
     * @param emailStr                  e-mail for the system user
     * @param phoneNumber               phone number
     * @param position                  job position
     * @param email                     e-mail value object for the AISafe user
     * @param securityClearance         security clearance level and expiry
     * @param skillsAssessmentDate      date of the last skills assessment
     * @param certifiedAircraftModelIds identities of the aircraft models the pilot is certified for
     *                                  (at least one, all must exist)
     * @return the saved {@link Pilot}
     * @throws IllegalArgumentException if no/unknown aircraft models are provided
     * @throws IllegalStateException    if there is no active session or the user has no company
     */
    public Pilot addPilot(final String username,
                          final String password,
                          final String firstName,
                          final String lastName,
                          final String emailStr,
                          final String phoneNumber,
                          final String position,
                          final Email email,
                          final SecurityClearance securityClearance,
                          final LocalDate skillsAssessmentDate,
                          final Set<Long> certifiedAircraftModelIds) {

        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ATCC);

        final AirTransportCompany company = authenticatedCollaboratorCompany();

        if (certifiedAircraftModelIds == null || certifiedAircraftModelIds.isEmpty())
            throw new IllegalArgumentException(
                    "A pilot must be certified for at least one aircraft model.");
        for (final Long modelId : certifiedAircraftModelIds) {
            modelRepo.ofIdentity(modelId).orElseThrow(() ->
                    new IllegalArgumentException("Aircraft model not found: " + modelId));
        }

        tx.beginTransaction();
        try {
            final User user = createPilotUser(username, password, firstName, lastName,
                    emailStr, phoneNumber, position, email, securityClearance, skillsAssessmentDate);
            final Pilot pilot = pilotRepo.save(
                    new Pilot(user, company.identity(), certifiedAircraftModelIds));
            tx.commit();
            return pilot;
        } catch (final Exception e) {
            tx.rollback();
            throw e;
        }
    }

    /**
     * Resolves the air transport company of the currently authenticated collaborator.
     *
     * @return the authenticated collaborator's company
     * @throws IllegalStateException if there is no active session or the user is not a company collaborator
     */
    private AirTransportCompany authenticatedCollaboratorCompany() {
        final SystemUser su = authz.session()
                .orElseThrow(() -> new IllegalStateException("No active session."))
                .authenticatedUser();

        return collaboratorRepo.findBySystemUser(su)
                .map(c -> {
                    if (!c.isCompanyCollaborator())
                        throw new IllegalStateException("Authenticated user is not a company collaborator.");
                    return companyRepo.ofIdentity(c.companyIataCode())
                            .orElseThrow(() -> new IllegalStateException(
                                    "Company not found for IATA code: " + c.companyIataCode()));
                })
                .orElseThrow(() -> new IllegalStateException(
                        "No collaborator found for authenticated user."));
    }

    private User createPilotUser(final String username, final String password,
                                 final String firstName, final String lastName,
                                 final String emailStr, final String phoneNumber,
                                 final String position, final Email email,
                                 final SecurityClearance securityClearance,
                                 final LocalDate skillsAssessmentDate) {

        final SystemUser systemUser = txUserSvc.registerNewUser(
                username, password, firstName, lastName, emailStr,
                Set.of(AiSafeRoles.PILOT), CurrentTimeCalendars.now());

        final long nextId = java.util.stream.StreamSupport
                .stream(userRepo.findAll().spliterator(), false).count() + 1;
        final MecanographicNumber mecNumber =
                MecanographicNumber.valueOf(String.format("PIL%05d", nextId));

        final User user = new User(systemUser, mecNumber, phoneNumber, email,
                position, securityClearance, skillsAssessmentDate);

        return userRepo.save(user);
    }
}
