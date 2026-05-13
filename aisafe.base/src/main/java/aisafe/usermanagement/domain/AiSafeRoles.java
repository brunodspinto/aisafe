package aisafe.usermanagement.domain;

import eapli.framework.infrastructure.authz.domain.model.Role;

/**
 * Constants for all AISafe system roles used in authorization checks.
 * This is a utility class — not to be instantiated.
 */
public final class AiSafeRoles {

    public static final Role ADMIN = Role.valueOf("ADMIN");
    public static final Role BACKOFFICE_OPERATOR = Role.valueOf("BACKOFFICE_OPERATOR");
    public static final Role ATCC = Role.valueOf("ATCC");
    public static final Role PILOT = Role.valueOf("PILOT");
    public static final Role FLIGHT_CONTROL_OPERATOR = Role.valueOf("FLIGHT_CONTROL_OPERATOR");
    public static final Role WEATHER_PERSON = Role.valueOf("WEATHER_PERSON");

    /**
     * Returns all application roles (excluding any generic end-user role).
     *
     * @return array containing every defined AISafe role
     */
    public static Role[] nonUserValues() {
        return new Role[] {
                ADMIN,
                BACKOFFICE_OPERATOR,
                ATCC,
                PILOT,
                FLIGHT_CONTROL_OPERATOR,
                WEATHER_PERSON
        };
    }

    private AiSafeRoles() {}
}
