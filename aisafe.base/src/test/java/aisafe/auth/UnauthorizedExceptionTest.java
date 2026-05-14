package aisafe.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Minimal unit tests for UnauthorizedException.
 */
class UnauthorizedExceptionTest {

    /**
     * Verifies that the exception preserves the provided message.
     */
    @Test
    void ensureExceptionCanBeCreatedWithMessage() {
        final String message = "Access denied";
        final UnauthorizedException ex = new UnauthorizedException(message);
        assertEquals(message, ex.getMessage());
    }

    /**
     * Verifies that the exception preserves both message and cause.
     */
    @Test
    void ensureExceptionCanBeCreatedWithCause() {
        final String message = "Authorization failed";
        final Throwable cause = new RuntimeException("Internal error");
        final UnauthorizedException ex = new UnauthorizedException(message, cause);
        assertEquals(message, ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    /**
     * Verifies that the exception can be thrown by authorization checks.
     */
    @Test
    void ensureExceptionCanBeThrown() {
        assertThrows(UnauthorizedException.class, () -> {
            throw new UnauthorizedException("Not authorized");
        });
    }

}
