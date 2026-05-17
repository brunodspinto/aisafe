package aisafe.usermanagement.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDate;

/**
 * Value object representing the security clearance assigned to a {@link User}.
 * Encapsulates a {@link SecurityLevel} and an expiration date.
 */
@Embeddable
public class SecurityClearance implements ValueObject {

    @Enumerated(EnumType.STRING)
    private SecurityLevel level;
    private LocalDate expirationDate;

    /**
     * Creates a new security clearance.
     *
     * @param level          the clearance level (must not be null)
     * @param expirationDate the date on which the clearance expires (must be today or a future date)
     * @throws IllegalArgumentException if {@code level} is null or {@code expirationDate} is null or in the past
     */
    public SecurityClearance(final SecurityLevel level, final LocalDate expirationDate) {
        if (level == null)
            throw new IllegalArgumentException("Security clearance level cannot be null");
        if (expirationDate == null || expirationDate.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Expiration date must be today or in the future");
        this.level = level;
        this.expirationDate = expirationDate;
    }

    /** For JPA. */
    protected SecurityClearance() {
        // for ORM
    }

    /** @return {@code true} if the clearance has not yet expired */
    public boolean isActive() {
        return !LocalDate.now().isAfter(expirationDate);
    }

    /** @return the security clearance level */
    public SecurityLevel level() {
        return level;
    }

    /** @return the date on which this clearance expires */
    public LocalDate expirationDate() {
        return expirationDate;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof SecurityClearance)) return false;
        final SecurityClearance other = (SecurityClearance) o;
        return level == other.level && expirationDate.equals(other.expirationDate);
    }

    @Override
    public int hashCode() {
        return 31 * level.hashCode() + expirationDate.hashCode();
    }

    @Override
    public String toString() {
        return level.name() + " (expires: " + expirationDate + ")";
    }
}
