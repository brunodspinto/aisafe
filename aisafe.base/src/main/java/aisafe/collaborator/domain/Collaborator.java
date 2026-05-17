package aisafe.collaborator.domain;

import aisafe.aircontrolarea.domain.AirControlAreaCode;
import aisafe.airtransportcompany.domain.IATACode;
import aisafe.usermanagement.domain.User;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.Objects;

/**
 * Entity and Aggregate Root representing a customer's collaborator.
 * A collaborator is a system user associated with either an AirTransportCompany
 * or an AirControlArea, but not both.
 * References to the company and area are stored as ID value objects to avoid
 * cross-aggregate object references.
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

    @Embedded
    @AttributeOverride(name = "code", column = @Column(name = "air_transport_company_iata_code"))
    private IATACode companyIataCode;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "air_control_area_code"))
    private AirControlAreaCode areaCode;

    protected Collaborator() {}

    /**
     * Creates a collaborator associated with an air transport company.
     *
     * @param user           the system user (non-null)
     * @param companyIataCode the IATA code of the company this collaborator belongs to (non-null)
     * @throws IllegalArgumentException if either argument is null
     */
    public Collaborator(final User user, final IATACode companyIataCode) {
        if (user == null)
            throw new IllegalArgumentException("User cannot be null.");
        if (companyIataCode == null)
            throw new IllegalArgumentException("Air Transport Company cannot be null.");
        this.user = user;
        this.companyIataCode = companyIataCode;
        this.areaCode = null;
    }

    /**
     * Creates a collaborator associated with an air control area.
     *
     * @param user     the system user (non-null)
     * @param areaCode the code of the air control area this collaborator belongs to (non-null)
     * @throws IllegalArgumentException if either argument is null
     */
    public Collaborator(final User user, final AirControlAreaCode areaCode) {
        if (user == null)
            throw new IllegalArgumentException("User cannot be null.");
        if (areaCode == null)
            throw new IllegalArgumentException("Air Control Area cannot be null.");
        this.user = user;
        this.areaCode = areaCode;
        this.companyIataCode = null;
    }

    /** @return the system user associated with this collaborator */
    public User user() { return user; }

    /** @return the IATA code of the company this collaborator works for, or {@code null} if area-based */
    public IATACode companyIataCode() { return companyIataCode; }

    /** @return the code of the air control area this collaborator works for, or {@code null} if company-based */
    public AirControlAreaCode areaCode() { return areaCode; }

    /** @return {@code true} if this collaborator belongs to an air transport company */
    public boolean isCompanyCollaborator() { return companyIataCode != null; }

    /** @return {@code true} if this collaborator belongs to an air control area */
    public boolean isAreaCollaborator() { return areaCode != null; }

    /** @return {@code true} if the associated system user account is active */
    public boolean isActive() { return user.systemUser().isActive(); }

    /**
     * @return the identity code (IATA or area code) of the customer this collaborator belongs to,
     *         or {@code "Unknown"} if neither is set
     */
    public String customerName() {
        if (isCompanyCollaborator()) return companyIataCode.toString();
        if (isAreaCollaborator()) return areaCode.toString();
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
