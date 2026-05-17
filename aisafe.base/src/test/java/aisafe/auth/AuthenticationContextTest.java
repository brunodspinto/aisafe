package aisafe.auth;

import eapli.framework.infrastructure.authz.domain.model.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AuthenticationContext 
 * 
 * Note: Tests focus on local validation and error handling.
 * Full authentication flow should be tested via integration tests.
 */
class AuthenticationContextTest {

    // --- Role Validation Tests ---

    /**
     * Verifies that a null role never grants access.
     */
    @Test
    void ensureHasRoleReturnsFalseForNullRole() {
        assertFalse(AuthenticationContext.hasRole(null), "hasRole should return false for null role");
    }

    /**
     * Verifies that an empty role list never grants access.
     */
    @Test
    void ensureHasAnyRoleReturnsFalseForEmptyRoleArray() {
        assertFalse(AuthenticationContext.hasAnyRole(), "hasAnyRole should return false for empty array");
    }

    /**
     * Verifies that a null role array never grants access.
     */
    @Test
    void ensureHasAnyRoleReturnsFalseForNullRoleArray() {
        assertFalse(AuthenticationContext.hasAnyRole((Role[]) null), "hasAnyRole should return false for null array");
    }

}
