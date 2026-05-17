package aisafe.collaborator.domain;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.airtransportcompany.domain.AirTransportCompany;
import aisafe.usermanagement.domain.User;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.Objects;

/**
 * Entity and Aggregate Root representing a customer's collaborator.
 * A collaborator is a system user associated with either an AirTransportCompany
 * or an AirControlArea, but not both.
 */
@Entity
@Table(name = "T_COLLABORATOR")
public class Collaborator implements AggregateRoot<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @OneToOne(fetch = FetchType.LAZY, cascade = {})
    @JoinColumn(name = "user_mecanographic_number")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {})
    @JoinColumn(name = "air_transport_company_iata_code")
    private AirTransportCompany airTransportCompany;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {})
    @JoinColumn(name = "air_control_area_code")
    private AirControlArea airControlArea;

    protected Collaborator() {}

    /**
     * Creates a collaborator associated with an air transport company.
     *
     * @param user    the system user (non-null)
     * @param company the company this collaborator belongs to (non-null)
     * @throws IllegalArgumentException if either argument is null
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
     * Creates a collaborator associated with an air control area.
     *
     * @param user the system user (non-null)
     * @param area the air control area this collaborator belongs to (non-null)
     * @throws IllegalArgumentException if either argument is null
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

    /** @return the system user associated with this collaborator */
    public User user() { return user; }

    /** @return the air transport company this collaborator works for, or {@code null} if area-based */
    public AirTransportCompany airTransportCompany() { return airTransportCompany; }

    /** @return the air control area this collaborator works for, or {@code null} if company-based */
    public AirControlArea airControlArea() { return airControlArea; }

    /** @return {@code true} if this collaborator belongs to an air transport company */
    public boolean isCompanyCollaborator() { return airTransportCompany != null; }

    /** @return {@code true} if this collaborator belongs to an air control area */
    public boolean isAreaCollaborator() { return airControlArea != null; }

    /** @return {@code true} if the associated system user account is active */
    public boolean isActive() { return user.systemUser().isActive(); }

    /**
     * @return the name of the customer (company or area) this collaborator belongs to,
     *         or {@code "Unknown"} if neither is set
     */
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
