package aisafe.usermanagement.domain;

import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;

/**
 * Aggregate root representing an AISafe system user.
 * Wraps an EAPLI {@link eapli.framework.infrastructure.authz.domain.model.SystemUser} and adds
 * AISafe-specific attributes such as phone number, position, and security clearance.
 */
@Entity
@Table(name = "T_AISAFE_USER")
public class User implements AggregateRoot<MecanographicNumber> {

    private static final long serialVersionUID = 1L;

    @Version
    private Long version;

    @EmbeddedId
    private MecanographicNumber mecanographicNumber;

    @OneToOne()
    private SystemUser systemUser;

    private String phoneNumber;

    private String position;

    @Embedded
    private SecurityClearance securityClearance;

    private LocalDate skillsAssessmentDate;

    @Embedded
    private Email email;

    /**
     * Creates a new AISafe user.
     *
     * @param systemUser           the underlying EAPLI system user (must not be null)
     * @param mecanographicNumber  the unique mecanographic identifier (must not be null)
     * @param phoneNumber          contact phone number
     * @param email                contact e-mail address
     * @param position             job position or title
     * @param securityClearance    security clearance level and expiration
     * @param skillsAssessmentDate date of the most recent skills assessment
     * @throws IllegalArgumentException if {@code systemUser} or {@code mecanographicNumber} is null
     */
    public User(final SystemUser systemUser,
                final MecanographicNumber mecanographicNumber,
                final String phoneNumber,
                final Email email,
                final String position,
                final SecurityClearance securityClearance,
                final LocalDate skillsAssessmentDate) {
        if (mecanographicNumber == null || systemUser == null)
            throw new IllegalArgumentException("SystemUser and MecanographicNumber are required");
        this.systemUser = systemUser;
        this.mecanographicNumber = mecanographicNumber;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.position = position;
        this.securityClearance = securityClearance;
        this.skillsAssessmentDate = skillsAssessmentDate;
    }

    /** For JPA. */
    protected User() {
        // for ORM
    }

    /** @return the underlying EAPLI system user */
    public SystemUser systemUser() { return systemUser; }
    /** @return the contact phone number */
    public String phoneNumber() { return phoneNumber; }
    /** @return the contact e-mail address */
    public Email email() { return email; }
    /** @return the job position or title */
    public String position() { return position; }
    /** @return the current security clearance */
    public SecurityClearance securityClearance() { return securityClearance; }
    /** @return the date of the most recent skills assessment */
    public LocalDate skillsAssessmentDate() { return skillsAssessmentDate; }

    @Override
    public boolean equals(final Object o) { return DomainEntities.areEqual(this, o); }

    @Override
    public int hashCode() { return DomainEntities.hashCode(this); }

    @Override
    public boolean sameAs(final Object other) { return DomainEntities.areEqual(this, other); }

    @Override
    public MecanographicNumber identity() { return mecanographicNumber; }

    /**
     * Updates the user's contact details.
     *
     * @param email       new e-mail address (must not be null)
     * @param phoneNumber new phone number (must not be blank)
     * @throws IllegalArgumentException if either argument is null or blank
     */
    public void updateContact(final Email email, final String phoneNumber) {
        if (email == null)
            throw new IllegalArgumentException("Email cannot be null.");
        if (phoneNumber == null || phoneNumber.isBlank())
            throw new IllegalArgumentException("Phone number cannot be null or empty.");
        this.email = email;
        this.phoneNumber = phoneNumber;
    }
}
