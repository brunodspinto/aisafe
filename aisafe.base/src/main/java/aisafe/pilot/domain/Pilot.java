package aisafe.pilot.domain;

import aisafe.airtransportcompany.domain.IATACode;
import aisafe.usermanagement.domain.User;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity and Aggregate Root representing a pilot (US075).
 * A pilot is a system {@link User} (with the PILOT role) that belongs to an air
 * transport company and is certified to operate one or more aircraft models.
 * The company and certified aircraft models are referenced by identity (IATA code
 * and aircraft model id, respectively) to avoid cross-aggregate object references.
 */
@Entity
@Table(name = "T_PILOT")
public class Pilot implements AggregateRoot<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @OneToOne(fetch = FetchType.LAZY, cascade = {})
    @JoinColumn(name = "user_mecanographic_number")
    private User user;

    @Embedded
    @AttributeOverride(name = "code", column = @Column(name = "company_iata_code"))
    private IATACode companyIataCode;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "T_PILOT_CERTIFICATIONS",
            joinColumns = @JoinColumn(name = "pilot_id"))
    @Column(name = "aircraft_model_id")
    private Set<Long> certifiedAircraftModelIds = new HashSet<>();

    private boolean active;

    protected Pilot() {}

    /**
     * Creates a pilot certified for one or more aircraft models.
     *
     * @param user                      the system user backing this pilot (non-null)
     * @param companyIataCode           the IATA code of the company the pilot belongs to (non-null)
     * @param certifiedAircraftModelIds identities of the aircraft models the pilot is certified for
     *                                  (non-null, at least one)
     * @throws IllegalArgumentException if any argument is null or the certification set is empty
     */
    public Pilot(final User user,
                 final IATACode companyIataCode,
                 final Set<Long> certifiedAircraftModelIds) {
        if (user == null)
            throw new IllegalArgumentException("User cannot be null.");
        if (companyIataCode == null)
            throw new IllegalArgumentException("Air Transport Company cannot be null.");
        if (certifiedAircraftModelIds == null || certifiedAircraftModelIds.isEmpty())
            throw new IllegalArgumentException(
                    "A pilot must be certified for at least one aircraft model.");

        final Set<Long> certifications = new HashSet<>(certifiedAircraftModelIds);
        if (certifications.contains(null))
            throw new IllegalArgumentException("Certified aircraft model id cannot be null.");

        this.user = user;
        this.companyIataCode = companyIataCode;
        this.certifiedAircraftModelIds = certifications;
        this.active = true;
    }

    /** @return the system user associated with this pilot */
    public User user() { return user; }

    /** @return the IATA code of the company this pilot belongs to */
    public IATACode companyIataCode() { return companyIataCode; }

    /** @return unmodifiable set of aircraft model identities this pilot is certified for */
    public Set<Long> certifiedAircraftModelIds() {
        return Collections.unmodifiableSet(certifiedAircraftModelIds);
    }

    /**
     * @param aircraftModelId the aircraft model identity to check
     * @return {@code true} if the pilot is certified to operate the given aircraft model
     */
    public boolean isCertifiedFor(final Long aircraftModelId) {
        return certifiedAircraftModelIds.contains(aircraftModelId);
    }

    /** @return {@code true} if this pilot is active (a newly added pilot is active) */
    public boolean isActive() { return active; }

    @Override
    public Long identity() { return id; }

    @Override
    public boolean sameAs(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public boolean equals(final Object o) { return DomainEntities.areEqual(this, o); }

    @Override
    public int hashCode() { return DomainEntities.hashCode(this); }

    @Override
    public String toString() {
        return String.format("Pilot{user='%s', company='%s', certifications=%d}",
                user.identity(), companyIataCode, certifiedAircraftModelIds.size());
    }
}
