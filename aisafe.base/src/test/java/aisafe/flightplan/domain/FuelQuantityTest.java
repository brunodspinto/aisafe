package aisafe.flightplan.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link FuelQuantity} value object (US080).
 */
class FuelQuantityTest {

    @Test
    void ensureValidFuelQuantityCanBeCreated() {
        final FuelQuantity fuel = FuelQuantity.valueOf(1500.0);
        assertEquals(1500.0, fuel.amount());
    }

    @Test
    void ensureZeroFuelIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> FuelQuantity.valueOf(0.0));
    }

    @Test
    void ensureNegativeFuelIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> FuelQuantity.valueOf(-1.0));
    }

    @Test
    void ensureEqualAmountsAreEqual() {
        assertEquals(FuelQuantity.valueOf(2000.0), FuelQuantity.valueOf(2000.0));
    }

    @Test
    void ensureDifferentAmountsAreNotEqual() {
        assertNotEquals(FuelQuantity.valueOf(2000.0), FuelQuantity.valueOf(2500.0));
    }

    @Test
    void ensureHashCodeIsConsistentWithEquals() {
        assertEquals(FuelQuantity.valueOf(1800.0).hashCode(), FuelQuantity.valueOf(1800.0).hashCode());
    }

    @Test
    void ensureCompareToOrdersByAmount() {
        assertTrue(FuelQuantity.valueOf(1000.0).compareTo(FuelQuantity.valueOf(2000.0)) < 0);
    }

    @Test
    void ensureToStringContainsAmount() {
        assertTrue(FuelQuantity.valueOf(1234.0).toString().contains("1234"));
    }

    @Test
    void ensureEqualsReturnsFalseForNull() {
        assertNotEquals(null, FuelQuantity.valueOf(1000.0));
    }
}
