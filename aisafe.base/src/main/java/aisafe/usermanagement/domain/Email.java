package aisafe.usermanagement.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Embeddable;

/**
 * Value object representing a validated e-mail address.
 * The address is normalised to lower-case on construction.
 */
@Embeddable
public class Email implements ValueObject {

    private static final long serialVersionUID = 1L;

    private String address;

    /**
     * Creates a new e-mail address value object.
     *
     * @param address the raw e-mail string (must be non-blank and match standard format)
     * @throws IllegalArgumentException if the address is blank or does not match the expected format
     */
    public Email(final String address) {
        if (address == null || address.isBlank())
            throw new IllegalArgumentException("Email cannot be empty");
        if (!address.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$"))
            throw new IllegalArgumentException("Invalid email format: " + address);
        this.address = address.toLowerCase();
    }

    /** For JPA. */
    protected Email() {
        // for ORM
    }

    /** @return the lower-cased e-mail address string */
    public String address() {
        return address;
    }

    @Override
    public String toString() {
        return address;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Email)) return false;
        final Email other = (Email) o;
        return address.equals(other.address);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }
}
