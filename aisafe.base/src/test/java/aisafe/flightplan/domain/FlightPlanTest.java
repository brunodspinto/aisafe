package aisafe.flightplan.domain;

import aisafe.dsl.ast.FlightPlanAst;
import aisafe.dsl.ast.FlightType;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link FlightPlan} aggregate root.
 * Verifies DSL-based construction, status transitions, and validation.
 */
class FlightPlanTest {

    @Test
    void ensureValidFlightPlanCanBeCreated() {
        final FlightPlan plan = new FlightPlan("TP1234", "REGULAR", "flight TP1234 { }");
        assertEquals("TP1234", plan.designator());
        assertEquals("REGULAR", plan.flightType());
        assertEquals(FlightPlanStatus.DRAFT, plan.status());
    }

    @Test
    void ensureDesignatorIsNormalizedToUpperCase() {
        final FlightPlan plan = new FlightPlan("tp1234", "REGULAR", "content");
        assertEquals("TP1234", plan.designator());
    }

    @Test
    void ensureDesignatorCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightPlan(null, "REGULAR", "content"));
    }

    @Test
    void ensureDesignatorCannotBeBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightPlan("   ", "REGULAR", "content"));
    }

    @Test
    void ensureFlightTypeCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightPlan("TP1234", null, "content"));
    }

    @Test
    void ensureFlightTypeCannotBeBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightPlan("TP1234", "   ", "content"));
    }

    @Test
    void ensureDslContentCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightPlan("TP1234", "REGULAR", null));
    }

    @Test
    void ensureDslContentCannotBeBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightPlan("TP1234", "REGULAR", "   "));
    }

    @Test
    void ensureStatusStartsAsDraft() {
        final FlightPlan plan = new FlightPlan("TP1234", "REGULAR", "content");
        assertEquals(FlightPlanStatus.DRAFT, plan.status());
    }

    @Test
    void ensureFromDslCreatesFlightPlanWithCorrectData() {
        final FlightPlanAst ast = new FlightPlanAst("TP1234", FlightType.REGULAR, List.of());
        final FlightPlan plan = FlightPlan.fromDsl(ast, "dsl content");
        assertEquals("TP1234", plan.designator());
        assertEquals("REGULAR", plan.flightType());
        assertEquals(FlightPlanStatus.DRAFT, plan.status());
    }

    @Test
    void ensureTwoFlightPlansWithSameDesignatorAreEqual() {
        final FlightPlan p1 = new FlightPlan("TP1234", "REGULAR", "content");
        final FlightPlan p2 = new FlightPlan("TP1234", "CHARTER", "other content");
        assertEquals(p1, p2);
    }

    @Test
    void ensureTwoFlightPlansWithDifferentDesignatorsAreNotEqual() {
        final FlightPlan p1 = new FlightPlan("TP1234", "REGULAR", "content");
        final FlightPlan p2 = new FlightPlan("TP5678", "REGULAR", "content");
        assertNotEquals(p1, p2);
    }

    @Test
    void ensureIdentityReturnsDesignator() {
        final FlightPlan plan = new FlightPlan("TP1234", "REGULAR", "content");
        assertEquals("TP1234", plan.identity());
    }

    @Test
    void ensureHashCodeIsConsistentWithEquals() {
        final FlightPlan p1 = new FlightPlan("TP1234", "REGULAR", "content");
        final FlightPlan p2 = new FlightPlan("TP1234", "CHARTER", "other");
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void ensureToStringContainsDesignator() {
        final FlightPlan plan = new FlightPlan("TP1234", "REGULAR", "content");
        assertTrue(plan.toString().contains("TP1234"));
    }

    @Test
    void ensureDslContentIsStored() {
        final FlightPlan plan = new FlightPlan("TP1234", "REGULAR", "my dsl content");
        assertEquals("my dsl content", plan.dslContent());
    }

    @Test
    void ensureSameAsReturnsTrueForEqualPlans() {
        final FlightPlan p1 = new FlightPlan("TP1234", "REGULAR", "content");
        final FlightPlan p2 = new FlightPlan("TP1234", "REGULAR", "content");
        assertTrue(p1.sameAs(p2));
    }

    @Test
    void ensureEqualsReturnsFalseForNull() {
        final FlightPlan plan = new FlightPlan("TP1234", "REGULAR", "content");
        assertNotEquals(null, plan);
    }

    @Test
    void ensureEqualsReturnsFalseForDifferentType() {
        final FlightPlan plan = new FlightPlan("TP1234", "REGULAR", "content");
        assertNotEquals("TP1234", plan);
    }
}