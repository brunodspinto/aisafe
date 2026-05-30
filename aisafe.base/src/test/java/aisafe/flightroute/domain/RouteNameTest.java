package aisafe.flightroute.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link RouteName} value object.
 * Verifies format validation, equality and immutability.
 */
class RouteNameTest {

    // -----------------------------------------------------------------------
    // Format validation
    // -----------------------------------------------------------------------

    @Test
    void ensureValidRouteNameIsAccepted() {
        final RouteName name = new RouteName("TP123");
        assertEquals("TP123", name.toString());
    }

    @Test
    void ensureMinimumValidRouteNameIsAccepted() {
        final RouteName name = new RouteName("TP1");
        assertEquals("TP1", name.toString());
    }

    @Test
    void ensureMaximumValidRouteNameIsAccepted() {
        final RouteName name = new RouteName("TP1234");
        assertEquals("TP1234", name.toString());
    }

    @Test
    void ensureRouteNameCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> new RouteName(null));
    }

    @Test
    void ensureRouteNameCannotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> new RouteName("   "));
    }

    @Test
    void ensureRouteNameWithOnlyOneLetterIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RouteName("T123"));
    }

    @Test
    void ensureRouteNameWithMoreThanTwoLettersIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RouteName("TAP123"));
    }

    @Test
    void ensureRouteNameWithNoDigitsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RouteName("TP"));
    }

    @Test
    void ensureRouteNameWithMoreThanFourDigitsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RouteName("TP12345"));
    }

    @Test
    void ensureRouteNameWithLowercaseLettersIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RouteName("tp123"));
    }

    @Test
    void ensureRouteNameWithSpecialCharactersIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RouteName("TP-123"));
    }

    @Test
    void ensureRouteNameWithOnlyLettersIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RouteName("TPAB"));
    }

    // -----------------------------------------------------------------------
    // Equality and value semantics
    // -----------------------------------------------------------------------

    @Test
    void ensureTwoRouteNamesWithSameValueAreEqual() {
        final RouteName a = new RouteName("TP123");
        final RouteName b = new RouteName("TP123");
        assertEquals(a, b);
    }

    @Test
    void ensureTwoRouteNamesWithDifferentValuesAreNotEqual() {
        final RouteName a = new RouteName("TP123");
        final RouteName b = new RouteName("TP456");
        assertNotEquals(a, b);
    }

    @Test
    void ensureEqualsReturnsTrueForSameInstance() {
        final RouteName name = new RouteName("TP123");
        assertEquals(name, name);
    }

    @Test
    void ensureEqualsReturnsFalseForNull() {
        assertNotEquals(null, new RouteName("TP123"));
    }

    @Test
    void ensureEqualsReturnsFalseForDifferentType() {
        assertNotEquals("TP123", new RouteName("TP123"));
    }

    @Test
    void ensureHashCodeIsConsistentWithEquals() {
        final RouteName a = new RouteName("TP123");
        final RouteName b = new RouteName("TP123");
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void ensureToStringReturnsName() {
        assertEquals("TP123", new RouteName("TP123").toString());
    }
}