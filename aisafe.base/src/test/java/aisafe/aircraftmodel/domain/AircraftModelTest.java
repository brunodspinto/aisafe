package aisafe.aircraftmodel.domain;

import aisafe.enginemodel.domain.EngineModel;
import aisafe.enginemodel.domain.EngineType;
import aisafe.maker.domain.Maker;
import aisafe.maker.domain.MakerName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link AircraftModel} aggregate root.
 * Verifies construction validation, engine management, and capacity constraints.
 */
class AircraftModelTest {

    private static Maker validMaker() {
        return new Maker(MakerName.valueOf("Boeing"), "USA");
    }

    private static EngineModel validEngine() {
        return new EngineModel("CFM56", "CFM International", EngineType.TURBOFAN, 120.0, 115.0, 0.35);
    }

    private static AircraftModel validAircraftModel() {
        return new AircraftModel(
                "737-800", validMaker(), AircraftType.PASSENGER,
                41140, 79016, 62732, 20894,
                12500, 230, 34.3, 125.0,
                0.026, 1.5, 5765.0, validEngine()
        );
    }

    @Test
    void ensureValidAircraftModelCanBeCreated() {
        final AircraftModel model = validAircraftModel();
        assertEquals("737-800", model.modelName());
        assertEquals("Boeing", model.maker().name());
        assertEquals(AircraftType.PASSENGER, model.aircraftType());
        assertEquals(1, model.certifiedEngines().size());
    }

    @Test
    void ensureModelNameCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () ->
                new AircraftModel(null, validMaker(), AircraftType.PASSENGER,
                        41140, 79016, 62732, 20894,
                        12500, 230, 34.3, 125.0,
                        0.026, 1.5, 5765.0, validEngine()));
    }

    @Test
    void ensureMakerCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () ->
                new AircraftModel("737-800", null, AircraftType.PASSENGER,
                        41140, 79016, 62732, 20894,
                        12500, 230, 34.3, 125.0,
                        0.026, 1.5, 5765.0, validEngine()));
    }

    @Test
    void ensureAircraftTypeCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () ->
                new AircraftModel("737-800", validMaker(), null,
                        41140, 79016, 62732, 20894,
                        12500, 230, 34.3, 125.0,
                        0.026, 1.5, 5765.0, validEngine()));
    }

    @Test
    void ensureFirstEngineCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () ->
                new AircraftModel("737-800", validMaker(), AircraftType.PASSENGER,
                        41140, 79016, 62732, 20894,
                        12500, 230, 34.3, 125.0,
                        0.026, 1.5, 5765.0, null));
    }

    @Test
    void ensureEmptyWeightMustBePositive() {
        assertThrows(IllegalArgumentException.class, () ->
                new AircraftModel("737-800", validMaker(), AircraftType.PASSENGER,
                        0, 79016, 62732, 20894,
                        12500, 230, 34.3, 125.0,
                        0.026, 1.5, 5765.0, validEngine()));
    }

    @Test
    void ensureMTOWMustBeGreaterThanEmptyWeight() {
        assertThrows(IllegalArgumentException.class, () ->
                new AircraftModel("737-800", validMaker(), AircraftType.PASSENGER,
                        79016, 41140, 62732, 20894,
                        12500, 230, 34.3, 125.0,
                        0.026, 1.5, 5765.0, validEngine()));
    }

    @Test
    void ensureCannotAddNullEngineModel() {
        final AircraftModel model = validAircraftModel();
        assertThrows(IllegalArgumentException.class, () -> model.addEngine(null));
    }

    @Test
    void ensureTwoModelsWithSameNameAndMakerAreEqual() {
        final AircraftModel a = validAircraftModel();
        final AircraftModel b = new AircraftModel(
                "737-800", validMaker(), AircraftType.CARGO,
                41140, 79016, 62732, 20894,
                12500, 230, 34.3, 125.0,
                0.026, 1.5, 5765.0, validEngine()
        );
        assertEquals(a, b);
    }

    @Test
    void ensureTwoModelsWithDifferentNamesAreNotEqual() {
        final AircraftModel a = validAircraftModel();
        final AircraftModel b = new AircraftModel(
                "737-900", validMaker(), AircraftType.PASSENGER,
                41140, 79016, 62732, 20894,
                12500, 230, 34.3, 125.0,
                0.026, 1.5, 5765.0, validEngine()
        );
        assertNotEquals(a, b);
    }

    @Test
    void ensureToStringContainsModelName() {
        final AircraftModel model = validAircraftModel();
        assertTrue(model.toString().contains("737-800"));
    }

    @Test
    void ensureEqualsReturnsTrueForSameInstance() {
        final AircraftModel model = validAircraftModel();
        assertEquals(model, model);
    }

    @Test
    void ensureEqualsReturnsFalseForNull() {
        assertNotEquals(null, validAircraftModel());
    }

    @Test
    void ensureHashCodeIsConsistentWithEquals() {
        final AircraftModel a = validAircraftModel();
        final AircraftModel b = validAircraftModel();
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void ensureGettersReturnCorrectValues() {
        final AircraftModel model = validAircraftModel();
        assertEquals(41140, model.emptyWeight());
        assertEquals(79016, model.mtow());
        assertEquals(62732, model.mzfw());
        assertEquals(20894, model.maxFuelCapacity());
        assertEquals(12500, model.serviceCeiling());
        assertEquals(230, model.cruiseSpeed());
        assertEquals(34.3, model.wingSpan());
        assertEquals(125.0, model.wingArea());
        assertEquals(0.026, model.dragCoefficient());
        assertEquals(1.5, model.liftCoefficient());
    }

    @Test
    void ensureSameAsReturnsTrueForEqualModels() {
        final AircraftModel a = validAircraftModel();
        final AircraftModel b = validAircraftModel();
        assertTrue(a.sameAs(b));
    }

    @Test
    void ensureCertifiedEnginesIsUnmodifiable() {
        final AircraftModel model = validAircraftModel();
        assertThrows(UnsupportedOperationException.class,
                () -> model.certifiedEngines().add(validEngine()));
    }

    @Test
    void ensureMZFWMustBePositive() {
        assertThrows(IllegalArgumentException.class, () ->
                new AircraftModel("737-800", validMaker(), AircraftType.PASSENGER,
                        41140, 79016, 0, 20894,
                        12500, 230, 34.3, 125.0,
                        0.026, 1.5, 5765.0, validEngine()));
    }

    @Test
    void ensureMZFWCannotExceedMTOW() {
        assertThrows(IllegalArgumentException.class, () ->
                new AircraftModel("737-800", validMaker(), AircraftType.PASSENGER,
                        41140, 79016, 80000, 20894,
                        12500, 230, 34.3, 125.0,
                        0.026, 1.5, 5765.0, validEngine()));
    }

    @Test
    void ensureMaxFuelCapacityMustBePositive() {
        assertThrows(IllegalArgumentException.class, () ->
                new AircraftModel("737-800", validMaker(), AircraftType.PASSENGER,
                        41140, 79016, 62732, 0,
                        12500, 230, 34.3, 125.0,
                        0.026, 1.5, 5765.0, validEngine()));
    }

    @Test
    void ensureServiceCeilingMustBePositive() {
        assertThrows(IllegalArgumentException.class, () ->
                new AircraftModel("737-800", validMaker(), AircraftType.PASSENGER,
                        41140, 79016, 62732, 20894,
                        0, 230, 34.3, 125.0,
                        0.026, 1.5, 5765.0, validEngine()));
    }

    @Test
    void ensureCruiseSpeedMustBePositive() {
        assertThrows(IllegalArgumentException.class, () ->
                new AircraftModel("737-800", validMaker(), AircraftType.PASSENGER,
                        41140, 79016, 62732, 20894,
                        12500, 0, 34.3, 125.0,
                        0.026, 1.5, 5765.0, validEngine()));
    }

    @Test
    void ensureWingSpanMustBePositive() {
        assertThrows(IllegalArgumentException.class, () ->
                new AircraftModel("737-800", validMaker(), AircraftType.PASSENGER,
                        41140, 79016, 62732, 20894,
                        12500, 230, 0, 125.0,
                        0.026, 1.5, 5765.0, validEngine()));
    }

    @Test
    void ensureWingAreaMustBePositive() {
        assertThrows(IllegalArgumentException.class, () ->
                new AircraftModel("737-800", validMaker(), AircraftType.PASSENGER,
                        41140, 79016, 62732, 20894,
                        12500, 230, 34.3, 0,
                        0.026, 1.5, 5765.0, validEngine()));
    }

    @Test
    void ensureDragCoefficientMustBePositive() {
        assertThrows(IllegalArgumentException.class, () ->
                new AircraftModel("737-800", validMaker(), AircraftType.PASSENGER,
                        41140, 79016, 62732, 20894,
                        12500, 230, 34.3, 125.0,
                        0, 1.5, 5765.0, validEngine()));
    }

    @Test
    void ensureLiftCoefficientMustBePositive() {
        assertThrows(IllegalArgumentException.class, () ->
                new AircraftModel("737-800", validMaker(), AircraftType.PASSENGER,
                        41140, 79016, 62732, 20894,
                        12500, 230, 34.3, 125.0,
                        0.026, 0, 5765.0, validEngine()));
    }

    @Test
    void ensureCannotAddIncompatibleEngineType() {
        final AircraftModel model = validAircraftModel();
        final EngineModel turboprop = new EngineModel("PT6A", "Pratt & Whitney Canada",
                EngineType.TURBOPROP, 17.0, 14.0, 0.29);
        assertThrows(IllegalArgumentException.class, () -> model.addEngine(turboprop));
    }

    @Test
    void ensureCanAddCompatibleEngineType() {
        final AircraftModel model = validAircraftModel();
        final EngineModel anotherTurbofan = new EngineModel("GE90", "GE Aviation",
                EngineType.TURBOFAN, 330.0, 310.0, 0.31);
        model.addEngine(anotherTurbofan);
        assertEquals(2, model.certifiedEngines().size());
    }

    @Test
    void ensureCanRemoveEngineWhenMoreThanOneExists() {
        final AircraftModel model = validAircraftModel();
        final EngineModel second = new EngineModel("GE90", "GE Aviation", EngineType.TURBOFAN, 330.0, 310.0, 0.31);
        model.addEngine(second);
        model.removeEngine(second);
        assertEquals(1, model.certifiedEngines().size());
    }

    @Test
    void ensureCannotRemoveLastEngine() {
        final AircraftModel model = validAircraftModel();
        assertThrows(IllegalArgumentException.class, () -> model.removeEngine(validEngine()));
    }

    @Test
    void ensureCannotRemoveNullEngine() {
        final AircraftModel model = validAircraftModel();
        final EngineModel second = new EngineModel("GE90", "GE Aviation", EngineType.TURBOFAN, 330.0, 310.0, 0.31);
        model.addEngine(second);
        assertThrows(IllegalArgumentException.class, () -> model.removeEngine(null));
    }

    @Test
    void ensureCannotRemoveEngineThatIsNotCertified() {
        final AircraftModel model = validAircraftModel();
        final EngineModel second = new EngineModel("GE90", "GE Aviation", EngineType.TURBOFAN, 330.0, 310.0, 0.31);
        model.addEngine(second);
        final EngineModel notCertified = new EngineModel("V2500", "IAE", EngineType.TURBOFAN, 111.0, 105.0, 0.33);
        assertThrows(IllegalArgumentException.class, () -> model.removeEngine(notCertified));
    }

    @Test
    void ensureCannotAddDuplicateEngineModel() {
        final AircraftModel model = validAircraftModel();
        final EngineModel anotherTurbofan = new EngineModel("GE90", "GE Aviation", EngineType.TURBOFAN, 330.0, 310.0, 0.31);
        model.addEngine(anotherTurbofan); // Adiciona a primeira vez
        assertThrows(IllegalArgumentException.class, () -> model.addEngine(anotherTurbofan));
    }

    @Test
    void ensureMaxRangeMustBePositive() {
        assertThrows(IllegalArgumentException.class, () ->
                new AircraftModel("737-800", validMaker(), AircraftType.PASSENGER,
                        41140, 79016, 62732, 20894,
                        12500, 230, 34.3, 125.0,
                        0.026, 1.5, 0, validEngine()));
    }

    @Test
    void ensureMaxRangeGetterWorks() {
        final AircraftModel model = validAircraftModel();
        assertEquals(5765.0, model.maxRange());
    }

    @Test
    void ensureHashCodeDiffersForUnequalModels() {
        final AircraftModel a = validAircraftModel();
        final AircraftModel b = new AircraftModel(
                "737-900", validMaker(), AircraftType.PASSENGER,
                41140, 79016, 62732, 20894,
                12500, 230, 34.3, 125.0,
                0.026, 1.5, 5765.0, validEngine()
        );
        assertNotEquals(a.hashCode(), b.hashCode());
    }

}
