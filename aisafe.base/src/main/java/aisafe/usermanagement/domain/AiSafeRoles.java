package aisafe.usermanagement.domain;

import eapli.framework.infrastructure.authz.domain.model.Role;

/**
 * Constants for all AISafe system roles used in authorization checks.
 * This is a utility class — not to be instantiated.
 */
public final class AiSafeRoles {

    /** System administrator role — full access. */
    public static final Role ADMIN = Role.valueOf("ADMIN");
    /** Back-office operator role — user and data management. */
    public static final Role BACKOFFICE_OPERATOR = Role.valueOf("BACKOFFICE_OPERATOR");
    /** Air Traffic Control Centre operator role. */
    public static final Role ATCC = Role.valueOf("ATCC");
    /** Pilot role — flight plan submission and viewing. */
    public static final Role PILOT = Role.valueOf("PILOT");
    /** Flight control operator role — flight plan approval and monitoring. */
    public static final Role FLIGHT_CONTROL_OPERATOR = Role.valueOf("FLIGHT_CONTROL_OPERATOR");
    /** Weather information provider role. */
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
