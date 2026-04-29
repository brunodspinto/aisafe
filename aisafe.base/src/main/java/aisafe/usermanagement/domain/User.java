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

    protected User() {
        // for ORM
    }

    public SystemUser systemUser() { return systemUser; }
    public String phoneNumber() { return phoneNumber; }
    public Email email() { return email; }
    public String position() { return position; }
    public SecurityClearance securityClearance() { return securityClearance; }
    public LocalDate skillsAssessmentDate() { return skillsAssessmentDate; }

    @Override
    public boolean equals(final Object o) { return DomainEntities.areEqual(this, o); }

    @Override
    public int hashCode() { return DomainEntities.hashCode(this); }

    @Override
    public boolean sameAs(final Object other) { return DomainEntities.areEqual(this, other); }

    @Override
    public MecanographicNumber identity() { return mecanographicNumber; }
}
