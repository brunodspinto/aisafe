package aisafe.usermanagement.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDate;

@E mbeddable
public class SecurityClearance implements ValueObject {

    private static final long serialVersionUID = 1L;

    @Enumerated(EnumType.STRING)
    private SecurityLevel level;
    private LocalDate expirationDate;

    public SecurityClearance(final SecurityLevel level, final LocalDate expirationDate) {
        if (level == null)
            throw new IllegalArgumentException("Security clearance level cannot be null");
        if (expirationDate == null || expirationDate.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Expiration date must be in the future");
        this.level = level;
        this.expirationDate = expirationDate;
    }

    protected SecurityClearance() {
        // for ORM
    }

    public boolean isActive() {
        return !LocalDate.now().isAfter(expirationDate);
    }

    public SecurityLevel level() {
        return level;
    }

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
