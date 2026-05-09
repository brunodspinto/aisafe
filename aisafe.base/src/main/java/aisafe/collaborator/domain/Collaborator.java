package aisafe.collaborator.domain;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.usermanagement.domain.User;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

import java.util.Objects;

/**
 * Entity and Aggregate Root representing a customer's collaborator.
 * A collaborator is a system user associated with either an AirTransportCompany
 * or an AirControlArea, but not both.
 */
@Entity
public class Collaborator implements AggregateRoot<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private User user;

    @ManyToOne
    private AirTransportCompany airTransportCompany;

    @ManyToOne
    private AirControlArea airControlArea;

    protected Collaborator() {}

    /**
     * Creates a collaborator associated with an AirTransportCompany.
     */
    public Collaborator(final User user, final AirTransportCompany company) {
        if (user == null)
            throw new IllegalArgumentException("User cannot be null.");
        if (company == null)
            throw new IllegalArgumentException("Air Transport Company cannot be null.");
        this.user = user;
        this.airTransportCompany = company;
        this.airControlArea = null;
    }

    /**
     * Creates a collaborator associated with an AirControlArea.
     */
    public Collaborator(final User user, final AirControlArea area) {
        if (user == null)
            throw new IllegalArgumentException("User cannot be null.");
        if (area == null)
            throw new IllegalArgumentException("Air Control Area cannot be null.");
        this.user = user;
        this.airControlArea = area;
        this.airTransportCompany = null;
    }

    public User user() { return user; }

    public AirTransportCompany airTransportCompany() { return airTransportCompany; }

    public AirControlArea airControlArea() { return airControlArea; }

    public boolean isCompanyCollaborator() { return airTransportCompany != null; }

    public boolean isAreaCollaborator() { return airControlArea != null; }

    public String customerName() {
        if (isCompanyCollaborator()) return airTransportCompany.name();
        if (isAreaCollaborator()) return airControlArea.name();
        return "Unknown";
    }

    @Override
    public Long identity() { return id; }

    @Override
    public boolean sameAs(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(id, ((Collaborator) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return String.format("Collaborator{user='%s', customer='%s'}",
                user.identity(), customerName());
    }
}
