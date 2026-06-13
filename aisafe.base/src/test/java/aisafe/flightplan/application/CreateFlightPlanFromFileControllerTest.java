package aisafe.flightplan.application;

import aisafe.dsl.parser.FlightPlanParserFacade;
import aisafe.flightplan.domain.FlightPlan;
import aisafe.flightplan.domain.FlightPlanDesignator;
import aisafe.flightplan.domain.FlightPlanStatus;
import aisafe.infrastructure.persistence.inmemory.InMemoryFlightPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link CreateFlightPlanFromFileController} (US121).
 *
 * <p>The controller is exercised with the real US120 parser and in-memory persistence,
 * while authorization is injected locally to avoid bootstrapping EAPLI/JPA.</p>
 */
class CreateFlightPlanFromFileControllerTest {

    private static final Path VALID_DSL =
            Path.of("src/test/resources/dsl/valid/01_single_leg_regular.dsl");
    private static final Path INVALID_LEXICAL_DSL =
            Path.of("src/test/resources/dsl/invalid/lexical/01_unknown_character.dsl");
    private static final Path INVALID_SYNTACTIC_DSL =
            Path.of("src/test/resources/dsl/invalid/syntactic/01_missing_semicolon.dsl");
    private static final Path INVALID_SEMANTIC_DSL =
            Path.of("src/test/resources/dsl/invalid/semantic/01_negative_fuel.dsl");

    private InMemoryFlightPlanRepository repository;
    private AtomicInteger authorizationCalls;
    private CreateFlightPlanFromFileController controller;

    @BeforeEach
    void setUp() throws Exception {
        final var reset = Class.forName(
                        "eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryRepository")
                .getDeclaredMethod("reset");
        reset.setAccessible(true);
        reset.invoke(null);

        repository = new InMemoryFlightPlanRepository();
        authorizationCalls = new AtomicInteger();
        controller = new CreateFlightPlanFromFileController(
                repository,
                new FlightPlanParserFacade(),
                authorizationCalls::incrementAndGet);
    }

    @Test
    void ensureValidDslFileCreatesDraftFlightPlan() throws Exception {
        final FlightPlan flightPlan = controller.createFromFile(VALID_DSL.toString());

        assertEquals("TP123", flightPlan.designator());
        assertEquals(FlightPlanStatus.DRAFT, flightPlan.status());
        assertTrue(flightPlan.dslContent().contains("FLIGHT TP123"));
        assertTrue(repository.ofIdentity(FlightPlanDesignator.valueOf("TP123")).isPresent());
        assertEquals(1, authorizationCalls.get());
    }

    @Test
    void ensureInvalidLexicalFileIsRejectedAndNotPersisted() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> controller.createFromFile(INVALID_LEXICAL_DSL.toString()));

        assertTrue(exception.getMessage().contains("Flight plan file is invalid"));
        assertTrue(exception.getMessage().contains("line"));
        assertEquals(0, countFlightPlans());
        assertEquals(1, authorizationCalls.get());
    }

    @Test
    void ensureInvalidSyntacticFileIsRejectedAndNotPersisted() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> controller.createFromFile(INVALID_SYNTACTIC_DSL.toString()));

        assertTrue(exception.getMessage().contains("Flight plan file is invalid"));
        assertTrue(exception.getMessage().contains("line"));
        assertEquals(0, countFlightPlans());
    }

    @Test
    void ensureInvalidSemanticFileIsRejectedAndNotPersisted() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> controller.createFromFile(INVALID_SEMANTIC_DSL.toString()));

        assertTrue(exception.getMessage().contains("Flight plan file is invalid"));
        assertFalse(exception.getMessage().isBlank());
        assertEquals(0, countFlightPlans());
    }

    @Test
    void ensureDuplicateDesignatorIsRejected() throws Exception {
        controller.createFromFile(VALID_DSL.toString());

        assertThrows(IllegalStateException.class,
                () -> controller.createFromFile(VALID_DSL.toString()));
        assertEquals(1, countFlightPlans());
    }

    @Test
    void ensureUnsupportedFileExtensionIsRejectedBeforeImport() throws Exception {
        final Path txtFile = Files.createTempFile("flight-plan-", ".txt");
        try {
            Files.writeString(txtFile, Files.readString(VALID_DSL));

            final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> controller.createFromFile(txtFile.toString()));

            assertTrue(exception.getMessage().contains(".dsl"));
            assertEquals(0, countFlightPlans());
        } finally {
            Files.deleteIfExists(txtFile);
        }
    }

    @Test
    void ensureAuthorizationFailureStopsImportFlow() {
        controller = new CreateFlightPlanFromFileController(
                repository,
                new FlightPlanParserFacade(),
                () -> {
                    throw new IllegalStateException("not authorized");
                });

        assertThrows(IllegalStateException.class,
                () -> controller.createFromFile(VALID_DSL.toString()));
        assertEquals(0, countFlightPlans());
    }

    private long countFlightPlans() {
        return StreamSupport.stream(repository.findAll().spliterator(), false).count();
    }
}
