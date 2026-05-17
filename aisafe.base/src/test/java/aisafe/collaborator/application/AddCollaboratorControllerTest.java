package aisafe.collaborator.application;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.aircontrolarea.domain.AirControlAreaCode;
import aisafe.aircontrolarea.domain.GeoBoundary;
import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.airtransportcompany.domain.ICAOCode;
import aisafe.airtransportcompany.domain.IATACode;
import aisafe.auth.AuthenticationContext;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafePasswordPolicy;
import aisafe.usermanagement.domain.AiSafeRoles;
import aisafe.usermanagement.domain.Email;
import aisafe.usermanagement.domain.SecurityClearance;
import aisafe.usermanagement.domain.SecurityLevel;
import aisafe.usermanagement.domain.User;
import aisafe.usermanagement.repositories.UserRepository;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;
import eapli.framework.infrastructure.authz.domain.model.Username;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddCollaboratorControllerTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static final String OPERATOR_USERNAME = "bo-op-us061";
    private static final String OPERATOR_PASSWORD = "Password1";

    private final AddCollaboratorController controller = new AddCollaboratorController();

    @BeforeAll
    static void configureAuthz() {
        AuthzRegistry.configure(
                PersistenceContext.repositories().systemUsers(),
                new AiSafePasswordPolicy(),
                new PlainTextEncoder());

        ensureBackofficeOperatorExists();
    }

    @AfterEach
    void tearDown() {
        AuthenticationContext.clear();
    }

    @Test
    void ensureCompanyCollaboratorCreatesDistinctSystemUser() {
        AuthenticationContext.authenticate(OPERATOR_USERNAME, OPERATOR_PASSWORD);

        final String companyCode = nextCompanyCode();
        final AirTransportCompany company = new AirTransportCompany(
                "Company " + companyCode,
                IATACode.valueOf(companyCode),
                ICAOCode.valueOf(companyCode + "C"));
        PersistenceContext.repositories().airTransportCompanies().save(company);

        final String username = "collab-company-" + SEQUENCE.incrementAndGet();
        final var collaborator = controller.addCompanyCollaborator(
                username,
                "Password1",
                "Ana",
                "Silva",
                username + "@aisafe.com",
                Set.of(AiSafeRoles.ATCC),
                "912345678",
                "Operations",
                new Email(username + "@aisafe.com"),
                new SecurityClearance(SecurityLevel.HIGH, LocalDate.now().plusYears(1)),
                LocalDate.now(),
                companyCode);

        assertNotNull(collaborator);
        assertEquals(company.identity(), collaborator.companyIataCode());
        assertTrue(collaborator.isCompanyCollaborator());

        final User savedUser = userRepo().findByUsername(Username.valueOf(username)).orElseThrow();
        assertEquals(username, savedUser.systemUser().username().toString());
        assertEquals(savedUser, collaborator.user());
    }

    @Test
    void ensureAreaCollaboratorCreatesDistinctSystemUser() {
        AuthenticationContext.authenticate(OPERATOR_USERNAME, OPERATOR_PASSWORD);

        final String areaCode = nextAreaCode();
        final AirControlArea area = new AirControlArea(
                AirControlAreaCode.valueOf(areaCode),
                "Area " + areaCode,
                1200.0,
                new GeoBoundary(42.5, 36.5, -6.0, -10.0));
        PersistenceContext.repositories().airControlAreas().save(area);

        final String username = "collab-area-" + SEQUENCE.incrementAndGet();
        final var collaborator = controller.addAreaCollaborator(
                username,
                "Password1",
                "Beatriz",
                "Costa",
                username + "@aisafe.com",
                Set.of(AiSafeRoles.FLIGHT_CONTROL_OPERATOR),
                "923456789",
                "Controller",
                new Email(username + "@aisafe.com"),
                new SecurityClearance(SecurityLevel.LOW, LocalDate.now().plusYears(1)),
                LocalDate.now(),
                areaCode);

        assertNotNull(collaborator);
        assertEquals(area.identity(), collaborator.areaCode());
        assertTrue(collaborator.isAreaCollaborator());

        final User savedUser = userRepo().findByUsername(Username.valueOf(username)).orElseThrow();
        assertEquals(username, savedUser.systemUser().username().toString());
        assertEquals(savedUser, collaborator.user());
    }

    private static void ensureBackofficeOperatorExists() {
        final var systemUserRepo = PersistenceContext.repositories().systemUsers();
        if (systemUserRepo.ofIdentity(Username.valueOf(OPERATOR_USERNAME)).isPresent()) {
            return;
        }

        final var builder = new SystemUserBuilder(new AiSafePasswordPolicy(), new PlainTextEncoder());
        builder.withUsername(OPERATOR_USERNAME)
                .withPassword(OPERATOR_PASSWORD)
                .withName("Back", "Office")
                .withEmail("bo-op-us061@aisafe.com")
                .withRoles(AiSafeRoles.BACKOFFICE_OPERATOR);
        systemUserRepo.save(builder.build());
    }

    private static String nextCompanyCode() {
        final int index = SEQUENCE.incrementAndGet();
        final char first = (char) ('A' + (index % 26));
        final char second = (char) ('A' + ((index / 26) % 26));
        return "" + first + second;
    }

    private static String nextAreaCode() {
        return "AREA-" + SEQUENCE.incrementAndGet();
    }

    private static UserRepository userRepo() {
        return PersistenceContext.repositories().users();
    }
}