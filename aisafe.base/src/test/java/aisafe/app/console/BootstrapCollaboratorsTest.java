package aisafe.app.console;

import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafePasswordPolicy;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.Username;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BootstrapCollaboratorsTest {

    @BeforeAll
    static void configureAuthz() {
        AuthzRegistry.configure(
                PersistenceContext.repositories().systemUsers(),
                new AiSafePasswordPolicy(),
                new PlainTextEncoder());
    }

    @Test
    void bootstrapCreatesFcoAndAtccCollaborators() throws Exception {
        final Class<?> cls = Class.forName("aisafe.app.console.AiSafeBootstrap");
        final java.lang.reflect.Method coll = cls.getDeclaredMethod("bootstrapCollaborators");
        final java.lang.reflect.Method atcc = cls.getDeclaredMethod("bootstrapAtccUser");
        coll.setAccessible(true);
        atcc.setAccessible(true);

        // ensure required reference data exists
        final java.lang.reflect.Method areas = cls.getDeclaredMethod("bootstrapAirControlAreas");
        final java.lang.reflect.Method companies = cls.getDeclaredMethod("bootstrapAirTransportCompanies");
        areas.setAccessible(true);
        companies.setAccessible(true);
        areas.invoke(null);
        companies.invoke(null);

        // invoke collaborator bootstraps
        coll.invoke(null);
        atcc.invoke(null);

        final var systemUserRepo = PersistenceContext.repositories().systemUsers();
        assertTrue(systemUserRepo.ofIdentity(Username.valueOf("fco1")).isPresent(), "fco1 system user should exist");
        assertTrue(systemUserRepo.ofIdentity(Username.valueOf("atcc1")).isPresent(), "atcc1 system user should exist");

        final var collaboratorRepo = PersistenceContext.repositories().collaborators();
        final var fcoSystemUser = systemUserRepo.ofIdentity(Username.valueOf("fco1")).orElseThrow();
        final var atccSystemUser = systemUserRepo.ofIdentity(Username.valueOf("atcc1")).orElseThrow();

        assertTrue(collaboratorRepo.findBySystemUser(fcoSystemUser).isPresent(), "fco1 collaborator should exist");
        assertTrue(collaboratorRepo.findBySystemUser(atccSystemUser).isPresent(), "atcc1 collaborator should exist");
    }
}
