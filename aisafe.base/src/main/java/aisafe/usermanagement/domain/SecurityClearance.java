package aisafe.usermanagement.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;

@Embeddable
public class SecurityClearance implements ValueObject {

    private static final long serialVersionUID = 1L;

    private String level;
    private LocalDate expirationDate;

    public SecurityClearance(final String level, final LocalDate expirationDate) {
        if (level == null || level.isBlank())
            throw new IllegalArgumentException("Security clearance level cannot be empty");
        if (expirationDate == null || expirationDate.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Expiration date must be in the future");
        this.level = level.trim();
        this.expirationDate = expirationDate;
    }

    protected SecurityClearance() {
        // for ORM
    }

    public boolean isActive() {
        return LocalDate.now().isBefore(expirationDate);
    }

    public String level() {
        return level;
    }

    public LocalDate expirationDate() {
        return expirationDate;
    }

    @Override
    public String toString() {
        return level + " (expires: " + expirationDate + ")";
    }
}
