package aisafe.enginemodel.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EngineModelTest {

    private static final String VALID_NAME = "CFM56";
    private static final String VALID_MAKER = "CFM International";
    private static final EngineType VALID_TYPE = EngineType.TURBOFAN;
    private static final double VALID_THRUST_STANDSTILL = 120.0;
    private static final double VALID_THRUST_CRUISE = 115.0;
    private static final double VALID_TSFC = 0.372;

    @Test
    void ensureValidEngineModelCanBeCreated() {
        final EngineModel model = new EngineModel(VALID_NAME, VALID_MAKER, VALID_TYPE,
                VALID_THRUST_STANDSTILL, VALID_THRUST_CRUISE, VALID_TSFC);
        assertEquals(VALID_NAME, model.name());
        assertEquals(VALID_MAKER, model.makerName());
        assertEquals(VALID_TYPE, model.engineType());
        assertEquals(VALID_THRUST_STANDSTILL, model.thrustAtStandstill());
        assertEquals(VALID_THRUST_CRUISE, model.thrustAtCruiseSpeed());
        assertEquals(VALID_TSFC, model.tsfc());
    }

    @Test
    void ensureNameCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new EngineModel(null, VALID_MAKER, VALID_TYPE,
                        VALID_THRUST_STANDSTILL, VALID_THRUST_CRUISE, VALID_TSFC));
    }

    @Test
    void ensureNameCannotBeBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> new EngineModel("   ", VALID_MAKER, VALID_TYPE,
                        VALID_THRUST_STANDSTILL, VALID_THRUST_CRUISE, VALID_TSFC));
    }

    @Test
    void ensureMakerNameCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new EngineModel(VALID_NAME, null, VALID_TYPE,
                        VALID_THRUST_STANDSTILL, VALID_THRUST_CRUISE, VALID_TSFC));
    }

    @Test
    void ensureMakerNameCannotBeBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> new EngineModel(VALID_NAME, "   ", VALID_TYPE,
                        VALID_THRUST_STANDSTILL, VALID_THRUST_CRUISE, VALID_TSFC));
    }

    @Test
    void ensureEngineTypeCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new EngineModel(VALID_NAME, VALID_MAKER, null,
                        VALID_THRUST_STANDSTILL, VALID_THRUST_CRUISE, VALID_TSFC));
    }

    @Test
    void ensureThrustAtStandstillMustBePositive() {
        assertThrows(IllegalArgumentException.class,
                () -> new EngineModel(VALID_NAME, VALID_MAKER, VALID_TYPE,
                        0.0, VALID_THRUST_CRUISE, VALID_TSFC));
    }

    @Test
    void ensureThrustAtStandstillCannotBeNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> new EngineModel(VALID_NAME, VALID_MAKER, VALID_TYPE,
                        -10.0, VALID_THRUST_CRUISE, VALID_TSFC));
    }

    @Test
    void ensureThrustAtCruiseSpeedMustBePositive() {
        assertThrows(IllegalArgumentException.class,
                () -> new EngineModel(VALID_NAME, VALID_MAKER, VALID_TYPE,
                        VALID_THRUST_STANDSTILL, 0.0, VALID_TSFC));
    }

    @Test
    void ensureThrustAtCruiseSpeedCannotBeNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> new EngineModel(VALID_NAME, VALID_MAKER, VALID_TYPE,
                        VALID_THRUST_STANDSTILL, -10.0, VALID_TSFC));
    }

    @Test
    void ensureTsfcMustBePositive() {
        assertThrows(IllegalArgumentException.class,
                () -> new EngineModel(VALID_NAME, VALID_MAKER, VALID_TYPE,
                        VALID_THRUST_STANDSTILL, VALID_THRUST_CRUISE, 0.0));
    }

    @Test
    void ensureTsfcCannotBeNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> new EngineModel(VALID_NAME, VALID_MAKER, VALID_TYPE,
                        VALID_THRUST_STANDSTILL, VALID_THRUST_CRUISE, -0.1));
    }

    @Test
    void ensureNameIsTrimmedOnConstruction() {
        final EngineModel model = new EngineModel("  CFM56  ", VALID_MAKER, VALID_TYPE,
                VALID_THRUST_STANDSTILL, VALID_THRUST_CRUISE, VALID_TSFC);
        assertEquals("CFM56", model.name());
    }

    @Test
    void ensureMakerNameIsTrimmedOnConstruction() {
        final EngineModel model = new EngineModel(VALID_NAME, "  CFM International  ", VALID_TYPE,
                VALID_THRUST_STANDSTILL, VALID_THRUST_CRUISE, VALID_TSFC);
        assertEquals("CFM International", model.makerName());
    }

    @Test
    void ensureToStringContainsRelevantInfo() {
        final EngineModel model = new EngineModel(VALID_NAME, VALID_MAKER, VALID_TYPE, VALID_THRUST_STANDSTILL, VALID_THRUST_CRUISE, VALID_TSFC);
        final String result = model.toString();
        assertTrue(result.contains(VALID_NAME));
        assertTrue(result.contains(VALID_MAKER));
    }

    @Test
    void ensureEqualsReturnsTrueForSameInstance() {
        final EngineModel model = new EngineModel(VALID_NAME, VALID_MAKER, VALID_TYPE,
                VALID_THRUST_STANDSTILL, VALID_THRUST_CRUISE, VALID_TSFC);
        assertEquals(model, model);
    }

    @Test
    void ensureEqualsReturnsFalseForNull() {
        final EngineModel model = new EngineModel(VALID_NAME, VALID_MAKER, VALID_TYPE,
                VALID_THRUST_STANDSTILL, VALID_THRUST_CRUISE, VALID_TSFC);
        assertNotEquals(null, model);
    }

    @Test
    void ensureSameAsReturnsTrueForSameInstance() {
        final EngineModel model = new EngineModel(VALID_NAME, VALID_MAKER, VALID_TYPE,
                VALID_THRUST_STANDSTILL, VALID_THRUST_CRUISE, VALID_TSFC);
        assertTrue(model.sameAs(model));
    }

    @Test
    void ensureHashCodeIsConsistent() {
        final EngineModel model = new EngineModel(VALID_NAME, VALID_MAKER, VALID_TYPE,
                VALID_THRUST_STANDSTILL, VALID_THRUST_CRUISE, VALID_TSFC);
        assertEquals(model.hashCode(), model.hashCode());
    }
}
