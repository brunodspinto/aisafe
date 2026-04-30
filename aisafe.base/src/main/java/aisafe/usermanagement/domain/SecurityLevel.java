package aisafe.usermanagement.domain;

public enum SecurityLevel {

    LOW(1), GUARDED(2), ELEVATED(3), HIGH(4), CRITICAL(5);

    private final int code;

    SecurityLevel(final int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static SecurityLevel fromCode(final int code) {
        for (final SecurityLevel level : values()) {
            if (level.code == code) return level;
        }
        throw new IllegalArgumentException("Invalid security level code: " + code);
    }

    public boolean requiresBodyScan() {
        return isAtLeast(ELEVATED);
    }

    public boolean isAtLeast(final SecurityLevel other) {
        return this.code >= other.code;
    }
}
