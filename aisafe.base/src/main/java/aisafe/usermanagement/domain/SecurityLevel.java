package aisafe.usermanagement.domain;

/**
 * Enumeration of AISafe personnel security clearance levels, ordered by increasing sensitivity.
 */
public enum SecurityLevel {

    /** Lowest clearance level — general access. */
    LOW(1),
    /** Guarded clearance — restricted areas. */
    GUARDED(2),
    /** Elevated clearance — requires body scan. */
    ELEVATED(3),
    /** High clearance — sensitive operations. */
    HIGH(4),
    /** Highest clearance — critical infrastructure. */
    CRITICAL(5);

    private final int code;

    SecurityLevel(final int code) {
        this.code = code;
    }

    /** @return the numeric code associated with this level */
    public int getCode() {
        return code;
    }

    /**
     * Looks up a {@code SecurityLevel} by its numeric code.
     *
     * @param code the numeric code (1–5)
     * @return the matching level
     * @throws IllegalArgumentException if no level has the given code
     */
    public static SecurityLevel fromCode(final int code) {
        for (final SecurityLevel level : values()) {
            if (level.code == code) return level;
        }
        throw new IllegalArgumentException("Invalid security level code: " + code);
    }

    /** @return {@code true} if this level is {@link #ELEVATED} or above */
    public boolean requiresBodyScan() {
        return isAtLeast(ELEVATED);
    }

    /**
     * @param other the minimum required level
     * @return {@code true} if this level's code is greater than or equal to {@code other}'s code
     */
    public boolean isAtLeast(final SecurityLevel other) {
        return this.code >= other.code;
    }
}
