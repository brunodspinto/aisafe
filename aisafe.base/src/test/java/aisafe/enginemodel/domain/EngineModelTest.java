package aisafe.enginemodel.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@link EngineModel} domain object.
 * Verifies construction validation and accessor correctness.
 */
class EngineModelTest {

    private static final String VALID_NAME = "CFM56";
    private static final String VALID_MAKER = "CFM International";
    private static final EngineType VALID_TYPE = EngineType.TURBOFAN;
    private static final double VALID_THRUST = 120.0;
    private static final double VALID_TSFC = 0.372;

    @Test
    void ensureValidEngineModelCanBeCreated() {
        final EngineModel model = new EngineModel(VALID_NAME, VALID_MAKER, VALID_TYPE, VALID_THRUST, VALID_TSFC);

        assertEquals(VALID_NAME, model.name());
        assertEquals(VALID_MAKER, model.makerName());
        assertEquals(VALID_TYPE, model.engineType());
        assertEquals(VALID_THRUST, model.thrust());
        assertEquals(VALID_TSFC, model.tsfc());
    }

    @Test
    void ensureNameCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new EngineModel(null, VALID_MAKER, VALID_TYPE, VALID_THRUST, VALID_TSFC));
    }

    @Test
    void ensureNameCannotBeBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> new EngineModel("   ", VALID_MAKER, VALID_TYPE, VALID_THRUST, VALID_TSFC));
    }

    @Test
    void ensureMakerNameCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new EngineModel(VALID_NAME, null, VALID_TYPE, VALID_THRUST, VALID_TSFC));
    }

    @Test
    void ensureMakerNameCannotBeBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> new EngineModel(VALID_NAME, "   ", VALID_TYPE, VALID_THRUST, VALID_TSFC));
    }

    @Test
    void ensureEngineTypeCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new EngineModel(VALID_NAME, VALID_MAKER, null, VALID_THRUST, VALID_TSFC));
    }

    @Test
    void ensureThrustMustBePositive() {
        assertThrows(IllegalArgumentException.class,
                () -> new EngineModel(VALID_NAME, VALID_MAKER, VALID_TYPE, 0.0, VALID_TSFC));
    }

    @Test
    void ensureThrustCannotBeNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> new EngineModel(VALID_NAME, VALID_MAKER, VALID_TYPE, -10.0, VALID_TSFC));
    }

    @Test
    void ensureTsfcMustBePositive() {
        assertThrows(IllegalArgumentException.class,
                () -> new EngineModel(VALID_NAME, VALID_MAKER, VALID_TYPE, VALID_THRUST, 0.0));
    }

    @Test
    void ensureTsfcCannotBeNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> new EngineModel(VALID_NAME, VALID_MAKER, VALID_TYPE, VALID_THRUST, -0.1));
    }

    @Test
    void ensureNameIsTrimmedOnConstruction() {
        final EngineModel model = new EngineModel("  CFM56  ", VALID_MAKER, VALID_TYPE, VALID_THRUST, VALID_TSFC);
        assertEquals("CFM56", model.name());
    }

    @Test
    void ensureMakerNameIsTrimmedOnConstruction() {
        final EngineModel model = new EngineModel(VALID_NAME, "  CFM International  ", VALID_TYPE, VALID_THRUST, VALID_TSFC);
        assertEquals("CFM International", model.makerName());
    }
}
