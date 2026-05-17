package aisafe.usermanagement.domain;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link SecurityClearance} value object.
 * Verifies construction validation, expiry semantics and equality.
 */
class SecurityClearanceTest {

    private static final LocalDate FUTURE_DATE = LocalDate.now().plusYears(1);

    @Test
    void ensureValidSecurityClearanceIsAccepted() {
        final SecurityClearance sc = new SecurityClearance(SecurityLevel.HIGH, FUTURE_DATE);
        assertEquals(SecurityLevel.HIGH, sc.level());
        assertEquals(FUTURE_DATE, sc.expirationDate());
    }

    @Test
    void ensureLevelCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new SecurityClearance(null, FUTURE_DATE));
    }

    @Test
    void ensureExpirationDateCannotBeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new SecurityClearance(SecurityLevel.HIGH, null));
    }

    @Test
    void ensureExpirationDateCannotBeInThePast() {
        final LocalDate pastDate = LocalDate.now().minusDays(1);
        assertThrows(IllegalArgumentException.class,
                () -> new SecurityClearance(SecurityLevel.HIGH, pastDate));
    }

    @Test
    void ensureIsActiveReturnsTrueForFutureDate() {
        final SecurityClearance sc = new SecurityClearance(SecurityLevel.LOW, FUTURE_DATE);
        assertTrue(sc.isActive());
    }

    @Test
    void ensureIsActiveReturnsTrueForToday() {
        final SecurityClearance sc = new SecurityClearance(SecurityLevel.LOW, LocalDate.now().plusDays(1));
        assertTrue(sc.isActive());
    }

    @Test
    void ensureTwoClearancesWithSameValuesAreEqual() {
        final SecurityClearance a = new SecurityClearance(SecurityLevel.HIGH, FUTURE_DATE);
        final SecurityClearance b = new SecurityClearance(SecurityLevel.HIGH, FUTURE_DATE);
        assertEquals(a, b);
    }

    @Test
    void ensureTwoClearancesWithDifferentLevelsAreNotEqual() {
        final SecurityClearance a = new SecurityClearance(SecurityLevel.HIGH, FUTURE_DATE);
        final SecurityClearance b = new SecurityClearance(SecurityLevel.LOW, FUTURE_DATE);
        assertNotEquals(a, b);
    }

    @Test
    void ensureTwoClearancesWithDifferentDatesAreNotEqual() {
        final SecurityClearance a = new SecurityClearance(SecurityLevel.HIGH, FUTURE_DATE);
        final SecurityClearance b = new SecurityClearance(SecurityLevel.HIGH, FUTURE_DATE.plusDays(1));
        assertNotEquals(a, b);
    }

    @Test
    void ensureEqualsReturnsTrueForSameInstance() {
        final SecurityClearance sc = new SecurityClearance(SecurityLevel.HIGH, FUTURE_DATE);
        assertEquals(sc, sc);
    }

    @Test
    void ensureEqualsReturnsFalseForNull() {
        final SecurityClearance sc = new SecurityClearance(SecurityLevel.HIGH, FUTURE_DATE);
        assertNotEquals(null, sc);
    }

    @Test
    void ensureEqualsReturnsFalseForDifferentType() {
        final SecurityClearance sc = new SecurityClearance(SecurityLevel.HIGH, FUTURE_DATE);
        assertNotEquals("HIGH", sc);
    }

    @Test
    void ensureHashCodeIsConsistentWithEquals() {
        final SecurityClearance a = new SecurityClearance(SecurityLevel.HIGH, FUTURE_DATE);
        final SecurityClearance b = new SecurityClearance(SecurityLevel.HIGH, FUTURE_DATE);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void ensureToStringContainsLevelAndDate() {
        final SecurityClearance sc = new SecurityClearance(SecurityLevel.HIGH, FUTURE_DATE);
        final String str = sc.toString();
        assertTrue(str.contains("HIGH"));
        assertTrue(str.contains(FUTURE_DATE.toString()));
    }
}
