package aisafe.usermanagement.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Embeddable;

@Embeddable
public class Email implements ValueObject {

    private static final long serialVersionUID = 1L;

    private String address;

    public Email(final String address) {
        if (address == null || address.isBlank())
            throw new IllegalArgumentException("Email cannot be empty");
        if (!address.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$"))
            throw new IllegalArgumentException("Invalid email format: " + address);
        this.address = address.toLowerCase();
    }

    protected Email() {
        // for ORM
    }

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
