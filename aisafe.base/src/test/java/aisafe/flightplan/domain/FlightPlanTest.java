package aisafe.flightplan.domain;

import aisafe.aircraft.domain.RegistrationNumber;
import aisafe.dsl.ast.FlightPlanAst;
import aisafe.dsl.ast.FlightType;
import aisafe.flightroute.domain.RouteName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static aisafe.dsl.ast.FlightType.REGULAR;
import static aisafe.dsl.ast.FlightType.CHARTER;

/**
 * Unit tests for the {@link FlightPlan} aggregate root.
 * Verifies DSL-based construction, status transitions, and validation.
 */
class FlightPlanTest {

    @Test
    void ensureValidFlightPlanCanBeCreated() {
        final FlightPlan plan = new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), REGULAR, "flight TP1234 { }");
        assertEquals("TP1234", plan.designator());
        assertEquals(REGULAR, plan.flightType());
        assertEquals(FlightPlanStatus.DRAFT, plan.status());
    }

    @Test
    void ensureDesignatorIsNormalizedToUpperCase() {
        final FlightPlan plan = new FlightPlan(FlightPlanDesignator.valueOf("tp1234"), REGULAR, "content");
        assertEquals("TP1234", plan.designator());
    }

    @Test
    void ensureDesignatorCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightPlan(null, REGULAR, "content"));
    }

    @Test
    void ensureDesignatorCannotBeBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightPlan(FlightPlanDesignator.valueOf("   "), REGULAR, "content"));
    }

    @Test
    void ensureFlightTypeCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), (FlightType) null, "content"));
    }

    @Test
    void ensureDslContentCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), REGULAR, null));
    }

    @Test
    void ensureDslContentCannotBeBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), REGULAR, "   "));
    }

    @Test
    void ensureStatusStartsAsDraft() {
        final FlightPlan plan = new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), REGULAR, "content");
        assertEquals(FlightPlanStatus.DRAFT, plan.status());
    }

    @Test
    void ensureFromDslCreatesFlightPlanWithCorrectData() {
        final FlightPlanAst ast = new FlightPlanAst("TP1234", FlightType.REGULAR, List.of());
        final FlightPlan plan = FlightPlan.fromDsl(ast, "dsl content");
        assertEquals("TP1234", plan.designator());
        assertEquals(REGULAR, plan.flightType());
        assertEquals(FlightPlanStatus.DRAFT, plan.status());
    }

    @Test
    void ensureTwoFlightPlansWithSameDesignatorAreEqual() {
        final FlightPlan p1 = new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), REGULAR, "content");
        final FlightPlan p2 = new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), CHARTER, "other content");
        assertEquals(p1, p2);
    }

    @Test
    void ensureTwoFlightPlansWithDifferentDesignatorsAreNotEqual() {
        final FlightPlan p1 = new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), REGULAR, "content");
        final FlightPlan p2 = new FlightPlan(FlightPlanDesignator.valueOf("TP5678"), REGULAR, "content");
        assertNotEquals(p1, p2);
    }

    @Test
    void ensureIdentityReturnsDesignator() {
        final FlightPlan plan = new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), REGULAR, "content");
        assertEquals(FlightPlanDesignator.valueOf("TP1234"), plan.identity());
    }

    @Test
    void ensureHashCodeIsConsistentWithEquals() {
        final FlightPlan p1 = new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), REGULAR, "content");
        final FlightPlan p2 = new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), CHARTER, "other");
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void ensureToStringContainsDesignator() {
        final FlightPlan plan = new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), REGULAR, "content");
        assertTrue(plan.toString().contains("TP1234"));
    }

    @Test
    void ensureDslContentIsStored() {
        final FlightPlan plan = new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), REGULAR, "my dsl content");
        assertEquals("my dsl content", plan.dslContent());
    }

    @Test
    void ensureSameAsReturnsTrueForEqualPlans() {
        final FlightPlan p1 = new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), REGULAR, "content");
        final FlightPlan p2 = new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), REGULAR, "content");
        assertTrue(p1.sameAs(p2));
    }

    @Test
    void ensureEqualsReturnsFalseForNull() {
        final FlightPlan plan = new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), REGULAR, "content");
        assertNotEquals(null, plan);
    }

    @Test
    void ensureEqualsReturnsFalseForDifferentType() {
        final FlightPlan plan = new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), REGULAR, "content");
        assertNotEquals("TP1234", plan);
    }

    // --- Form-based creation (US080) ---

    private static final RouteName ROUTE = new RouteName("TP123");
    private static final RegistrationNumber AIRCRAFT = RegistrationNumber.valueOf("CS-TUA");
    private static final Long PILOT_ID = 1L;

    private static LocalDateTime futureDeparture() {
        return LocalDateTime.now().plusDays(2);
    }

    private static FlightPlan validFormPlan() {
        return new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), REGULAR,
                ROUTE, AIRCRAFT, PILOT_ID, futureDeparture(), FuelQuantity.valueOf(1500.0));
    }

    @Test
    void ensureFormBasedFlightPlanCanBeCreated() {
        final FlightPlan plan = validFormPlan();
        assertEquals("TP1234", plan.designator());
        assertEquals(REGULAR, plan.flightType());
        assertEquals(FlightPlanStatus.DRAFT, plan.status());
    }

    @Test
    void ensureFormBasedPlanStoresAllFields() {
        final FlightPlan plan = validFormPlan();
        assertEquals(ROUTE, plan.routeName());
        assertEquals(AIRCRAFT, plan.aircraftRegistration());
        assertEquals(PILOT_ID, plan.assignedPilotId());
        assertEquals(FuelQuantity.valueOf(1500.0), plan.fuelQuantity());
        assertNotNull(plan.departureDateTime());
    }

    @Test
    void ensureFormBasedPlanHasNullDslContent() {
        assertNull(validFormPlan().dslContent());
    }

    @Test
    void ensureFormPlanRejectsNullRoute() {
        assertThrows(IllegalArgumentException.class, () ->
                new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), REGULAR,
                        null, AIRCRAFT, PILOT_ID, futureDeparture(), FuelQuantity.valueOf(1500.0)));
    }

    @Test
    void ensureFormPlanRejectsNullAircraft() {
        assertThrows(IllegalArgumentException.class, () ->
                new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), REGULAR,
                        ROUTE, null, PILOT_ID, futureDeparture(), FuelQuantity.valueOf(1500.0)));
    }

    @Test
    void ensureFormPlanRejectsNullPilot() {
        assertThrows(IllegalArgumentException.class, () ->
                new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), REGULAR,
                        ROUTE, AIRCRAFT, null, futureDeparture(), FuelQuantity.valueOf(1500.0)));
    }

    @Test
    void ensureFormPlanRejectsNullDeparture() {
        assertThrows(IllegalArgumentException.class, () ->
                new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), REGULAR,
                        ROUTE, AIRCRAFT, PILOT_ID, null, FuelQuantity.valueOf(1500.0)));
    }

    @Test
    void ensureFormPlanRejectsPastDeparture() {
        assertThrows(IllegalArgumentException.class, () ->
                new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), REGULAR,
                        ROUTE, AIRCRAFT, PILOT_ID, LocalDateTime.now().minusDays(1), FuelQuantity.valueOf(1500.0)));
    }

    @Test
    void ensureFormPlanRejectsNullFuel() {
        assertThrows(IllegalArgumentException.class, () ->
                new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), REGULAR,
                        ROUTE, AIRCRAFT, PILOT_ID, futureDeparture(), null));
    }

    @Test
    void ensureFormPlanRejectsNullFlightType() {
        assertThrows(IllegalArgumentException.class, () ->
                new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), (FlightType) null,
                        ROUTE, AIRCRAFT, PILOT_ID, futureDeparture(), FuelQuantity.valueOf(1500.0)));
    }

    // --- markTested lifecycle (US085) ---

    private static FlightPlan validDslPlan() {
        return new FlightPlan(FlightPlanDesignator.valueOf("TP1234"), REGULAR, "flight TP1234 { }");
    }

    @Test
    void ensureValidatedDslPlanCanBeMarkedTested() {
        final FlightPlan plan = validDslPlan();
        plan.markValidated();
        plan.markTested();
        assertEquals(FlightPlanStatus.TESTED, plan.status());
    }

    @Test
    void ensureDraftPlanCannotBeMarkedTested() {
        final FlightPlan plan = validDslPlan();
        // status is DRAFT — markTested() must reject
        assertThrows(IllegalStateException.class, plan::markTested);
    }

    @Test
    void ensureAlreadyTestedPlanCannotBeMarkedTestedAgain() {
        final FlightPlan plan = validDslPlan();
        plan.markValidated();
        plan.markTested();
        // second call must reject
        assertThrows(IllegalStateException.class, plan::markTested);
    }

    // --- Weather data insertion (US082) ---

    private static FlightPlan testedPlan() {
        final FlightPlan plan = validDslPlan();
        plan.markValidated();
        plan.markTested();
        return plan;
    }

    @Test
    void ensureWeatherDataCanBeAdded() {
        final FlightPlan plan = validDslPlan();
        plan.addWeatherData(10L);
        assertTrue(plan.weatherDataIds().contains(10L));
    }

    @Test
    void ensureAddingWeatherDataRejectsNull() {
        final FlightPlan plan = validDslPlan();
        assertThrows(IllegalArgumentException.class, () -> plan.addWeatherData(null));
    }

    @Test
    void ensureWeatherDataIdsAreUnmodifiable() {
        final FlightPlan plan = validDslPlan();
        plan.addWeatherData(10L);
        assertThrows(UnsupportedOperationException.class, () -> plan.weatherDataIds().add(20L));
    }

    @Test
    void ensureMultipleWeatherDataRecordsCanBeAdded() {
        final FlightPlan plan = validDslPlan();
        plan.addWeatherData(10L);
        plan.addWeatherData(20L);
        assertEquals(2, plan.weatherDataIds().size());
    }

    @Test
    void ensureAddingNewWeatherDataVoidsTestWhenTested() {
        final FlightPlan plan = testedPlan();
        plan.addWeatherData(10L);
        assertEquals(FlightPlanStatus.VALIDATED, plan.status());
    }

    @Test
    void ensureReAddingSameWeatherDataDoesNotVoidTest() {
        final FlightPlan plan = testedPlan();
        plan.addWeatherData(10L);                 // voids: TESTED -> VALIDATED
        plan.markTested();                        // tested again with the weather data in place
        plan.addWeatherData(10L);                 // same id -> no-op, must NOT void
        assertEquals(FlightPlanStatus.TESTED, plan.status());
    }

    @Test
    void ensureAddingWeatherDataDoesNotChangeDraftStatus() {
        final FlightPlan plan = validDslPlan(); // DRAFT
        plan.addWeatherData(10L);
        assertEquals(FlightPlanStatus.DRAFT, plan.status());
    }

    @Test
    void ensureAddingWeatherDataDoesNotChangeValidatedStatus() {
        final FlightPlan plan = validDslPlan();
        plan.markValidated(); // VALIDATED
        plan.addWeatherData(10L);
        assertEquals(FlightPlanStatus.VALIDATED, plan.status());
    }
}