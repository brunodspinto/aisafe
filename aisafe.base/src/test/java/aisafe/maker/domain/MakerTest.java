package aisafe.maker.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Maker} domain object.
 * Verifies construction validation and equality semantics.
 */
class MakerTest {

    private static Maker validMaker() {
        return new Maker(MakerName.valueOf("Boeing"), "USA");
    }

    @Test
    void ensureValidMakerCanBeCreated() {
        final Maker maker = validMaker();
        assertEquals("Boeing", maker.name());
        assertEquals("USA", maker.country());
    }

    @Test
    void ensureNameCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> new Maker((MakerName) null, "USA"));
    }

    @Test
    void ensureNameCannotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> new Maker(MakerName.valueOf("   "), "USA"));
    }

    @Test
    void ensureCountryCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> new Maker(MakerName.valueOf("Boeing"), null));
    }

    @Test
    void ensureCountryCannotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> new Maker(MakerName.valueOf("Boeing"), "   "));
    }

    @Test
    void ensureTwoMakersWithSameNameAreEqual() {
        final Maker a = new Maker(MakerName.valueOf("Boeing"), "USA");
        final Maker b = new Maker(MakerName.valueOf("Boeing"), "United States");
        assertEquals(a, b);
    }

    @Test
    void ensureTwoMakersWithDifferentNamesAreNotEqual() {
        final Maker a = new Maker(MakerName.valueOf("Boeing"), "USA");
        final Maker b = new Maker(MakerName.valueOf("Airbus"), "France");
        assertNotEquals(a, b);
    }

    @Test
    void ensureIdentityReturnsName() {
        final Maker maker = validMaker();
        assertEquals(MakerName.valueOf("Boeing"), maker.identity());
    }

    @Test
    void ensureHashCodeIsConsistentWithEquals() {
        final Maker a = new Maker(MakerName.valueOf("Boeing"), "USA");
        final Maker b = new Maker(MakerName.valueOf("Boeing"), "USA");
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void ensureToStringContainsName() {
        final Maker maker = validMaker();
        assertTrue(maker.toString().contains("Boeing"));
    }

    @Test
    void ensureEqualsReturnsTrueForSameInstance() {
        final Maker maker = validMaker();
        assertEquals(maker, maker);
    }

    @Test
    void ensureEqualsReturnsFalseForNull() {
        assertNotEquals(null, validMaker());
    }

    @Test
    void ensureEqualsReturnsFalseForDifferentType() {
        assertNotEquals("Boeing", validMaker());
    }

    @Test
    void ensureSameAsReturnsTrueForEqualMakers() {
        final Maker a = new Maker(MakerName.valueOf("Boeing"), "USA");
        final Maker b = new Maker(MakerName.valueOf("Boeing"), "USA");
        assertTrue(a.sameAs(b));
    }

    @Test
    void ensureCountryIsStored() {
        final Maker maker = validMaker();
        assertEquals("USA", maker.country());
    }
}
