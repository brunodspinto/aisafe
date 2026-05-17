package aisafe.usermanagement.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Email} value object.
 * Verifies format validation, lowercase normalisation and equality semantics.
 */
class EmailTest {

    @Test
    void ensureValidEmailIsAccepted() {
        final Email email = new Email("john.doe@example.com");
        assertEquals("john.doe@example.com", email.address());
    }

    @Test
    void ensureEmailCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> new Email(null));
    }

    @Test
    void ensureEmailCannotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> new Email("   "));
    }

    @Test
    void ensureEmailRejectsInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> new Email("not-an-email"));
    }

    @Test
    void ensureEmailRejectsMissingDomain() {
        assertThrows(IllegalArgumentException.class, () -> new Email("user@"));
    }

    @Test
    void ensureEmailRejectsMissingAt() {
        assertThrows(IllegalArgumentException.class, () -> new Email("user.example.com"));
    }

    @Test
    void ensureEmailRejectsMissingTld() {
        assertThrows(IllegalArgumentException.class, () -> new Email("user@example"));
    }

    @Test
    void ensureEmailIsNormalisedToLowerCase() {
        final Email email = new Email("John.Doe@Example.COM");
        assertEquals("john.doe@example.com", email.address());
    }

    @Test
    void ensureEmailAcceptsDotsAndHyphensInLocal() {
        final Email email = new Email("john.doe-admin@example.com");
        assertEquals("john.doe-admin@example.com", email.address());
    }

    @Test
    void ensureTwoEmailsWithSameAddressAreEqual() {
        final Email a = new Email("john@example.com");
        final Email b = new Email("john@example.com");
        assertEquals(a, b);
    }

    @Test
    void ensureTwoEmailsWithDifferentAddressesAreNotEqual() {
        final Email a = new Email("john@example.com");
        final Email b = new Email("jane@example.com");
        assertNotEquals(a, b);
    }

    @Test
    void ensureEqualityIsCaseInsensitive() {
        final Email a = new Email("John@Example.com");
        final Email b = new Email("john@example.com");
        assertEquals(a, b);
    }

    @Test
    void ensureEqualsReturnsTrueForSameInstance() {
        final Email email = new Email("john@example.com");
        assertEquals(email, email);
    }

    @Test
    void ensureEqualsReturnsFalseForNull() {
        final Email email = new Email("john@example.com");
        assertNotEquals(null, email);
    }

    @Test
    void ensureEqualsReturnsFalseForDifferentType() {
        final Email email = new Email("john@example.com");
        assertNotEquals("john@example.com", email);
    }

    @Test
    void ensureHashCodeIsConsistentWithEquals() {
        final Email a = new Email("john@example.com");
        final Email b = new Email("john@example.com");
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void ensureToStringReturnsAddress() {
        final Email email = new Email("john@example.com");
        assertEquals("john@example.com", email.toString());
    }
}
