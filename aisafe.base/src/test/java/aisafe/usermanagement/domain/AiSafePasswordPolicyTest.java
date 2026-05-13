package aisafe.usermanagement.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link AiSafePasswordPolicy}.
 * Verifies password strength validation rules enforced on user registration.
 */
class AiSafePasswordPolicyTest {

    private final AiSafePasswordPolicy policy = new AiSafePasswordPolicy();

    // --- Valid passwords (AC031.3) ---

    @Test
    void ensureValidPasswordIsAccepted() {
        assertTrue(policy.isSatisfiedBy("Password1"));
    }

    @Test
    void ensurePasswordWithExactlyMinimumLengthIsAccepted() {
        assertTrue(policy.isSatisfiedBy("Pass1a"));
    }

    @Test
    void ensureLongPasswordWithDigitAndCapitalIsAccepted() {
        assertTrue(policy.isSatisfiedBy("SuperSecurePassword123"));
    }

    // --- Invalid passwords (AC031.3) ---

    @Test
    void ensurePasswordShorterThanSixCharsIsRejected() {
        assertFalse(policy.isSatisfiedBy("Pa1"));
    }

    @Test
    void ensurePasswordWithoutDigitIsRejected() {
        assertFalse(policy.isSatisfiedBy("Password"));
    }

    @Test
    void ensurePasswordWithoutCapitalLetterIsRejected() {
        assertFalse(policy.isSatisfiedBy("password1"));
    }

    @Test
    void ensureNullPasswordIsRejected() {
        assertFalse(policy.isSatisfiedBy(null));
    }

    @Test
    void ensureEmptyPasswordIsRejected() {
        assertFalse(policy.isSatisfiedBy(""));
    }
}
