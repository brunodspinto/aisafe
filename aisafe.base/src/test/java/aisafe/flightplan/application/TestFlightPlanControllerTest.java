package aisafe.flightplan.application;

import aisafe.aircraft.domain.RegistrationNumber;
import aisafe.dsl.ast.FlightType;
import aisafe.flightplan.domain.FlightPlan;
import aisafe.flightplan.domain.FlightPlanDesignator;
import aisafe.flightplan.domain.FuelQuantity;
import aisafe.flightroute.domain.RouteName;
import aisafe.infrastructure.persistence.inmemory.InMemoryFlightPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link TestFlightPlanController} (US085).
 *
 * <p>Uses the package-private constructor to inject an in-memory repository, avoiding any
 * JPA or authentication context. Tests verify the plan-listing and status-filtering logic only;
 * live C-binary invocation is an integration concern outside unit-test scope.</p>
 */
class TestFlightPlanControllerTest {

    private InMemoryFlightPlanRepository repo;
    private TestFlightPlanController controller;

    @BeforeEach
    void setUp() throws Exception {
        final var reset = Class.forName(
                "eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryRepository")
                .getDeclaredMethod("reset");
        reset.setAccessible(true);
        reset.invoke(null);

        repo = new InMemoryFlightPlanRepository();
        controller = new TestFlightPlanController(repo);
    }

    // ---- helpers ----

    private static FlightPlan dslDraftPlan(final String designator) {
        return new FlightPlan(
                FlightPlanDesignator.valueOf(designator),
                FlightType.REGULAR,
                "flight_plan " + designator + " { }");
    }

    private static FlightPlan validatedDslPlan(final String designator) {
        final FlightPlan fp = dslDraftPlan(designator);
        fp.markValidated();
        return fp;
    }

    private static FlightPlan formBasedValidatedPlan(final String designator) {
        final FlightPlan fp = new FlightPlan(
                FlightPlanDesignator.valueOf(designator),
                FlightType.REGULAR,
                new RouteName("TP99"),
                RegistrationNumber.valueOf("CS-TUA"),
                1L,
                LocalDateTime.now().plusDays(1),
                FuelQuantity.valueOf(5000.0));
        fp.markValidated();
        return fp;
    }

    // ---- tests ----

    @Test
    void listValidatedDslPlans_returnsOnlyValidatedDslPlans() {
        repo.save(validatedDslPlan("TP1001"));
        repo.save(validatedDslPlan("TP1002"));

        final List<FlightPlan> result = controller.listValidatedDslPlans();

        assertEquals(2, result.size());
    }

    @Test
    void listValidatedDslPlans_excludesDraftPlans() {
        repo.save(dslDraftPlan("TP1001")); // DRAFT status

        final List<FlightPlan> result = controller.listValidatedDslPlans();

        assertTrue(result.isEmpty());
    }

    @Test
    void listValidatedDslPlans_excludesFormBasedPlans() {
        repo.save(formBasedValidatedPlan("TP1001")); // VALIDATED but dslContent == null

        final List<FlightPlan> result = controller.listValidatedDslPlans();

        assertTrue(result.isEmpty());
    }

    @Test
    void listValidatedDslPlans_excludesTestedPlans() {
        final FlightPlan fp = validatedDslPlan("TP1001");
        fp.markTested(); // TESTED — not eligible
        repo.save(fp);

        final List<FlightPlan> result = controller.listValidatedDslPlans();

        assertTrue(result.isEmpty());
    }

    @Test
    void listValidatedDslPlans_emptyWhenNoPlanExists() {
        final List<FlightPlan> result = controller.listValidatedDslPlans();

        assertTrue(result.isEmpty());
    }

    // ---- testFlightPlan guard conditions (via package-private executeTestFlightPlan) ----

    @Test
    void testFlightPlan_throwsIllegalArgumentWhenPlanNotFound() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.executeTestFlightPlan("NOTEXIST"));
    }

    @Test
    void testFlightPlan_throwsIllegalStateWhenPlanNotValidated() {
        repo.save(dslDraftPlan("TP2001")); // DRAFT status — cannot be tested
        assertThrows(IllegalStateException.class,
                () -> controller.executeTestFlightPlan("TP2001"));
    }

    @Test
    void testFlightPlan_throwsIllegalStateWhenDslContentIsNull() {
        repo.save(formBasedValidatedPlan("TP2001")); // VALIDATED but dslContent == null
        assertThrows(IllegalStateException.class,
                () -> controller.executeTestFlightPlan("TP2001"));
    }
}
