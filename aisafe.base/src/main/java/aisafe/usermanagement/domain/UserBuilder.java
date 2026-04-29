package aisafe.usermanagement.domain;

import eapli.framework.domain.model.DomainFactory;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import java.time.LocalDate;

public class UserBuilder implements DomainFactory<User> {

    private SystemUser systemUser;
    private MecanographicNumber mecanographicNumber;
    private String phoneNumber;
    private Email email;
    private String position;
    private SecurityClearance securityClearance;
    private LocalDate skillsAssessmentDate;

    public UserBuilder withSystemUser(final SystemUser systemUser) {
        this.systemUser = systemUser;
        return this;
    }

    public UserBuilder withMecanographicNumber(final MecanographicNumber mecanographicNumber) {
        this.mecanographicNumber = mecanographicNumber;
        return this;
    }

    public UserBuilder withMecanographicNumber(final String mecanographicNumber) {
        this.mecanographicNumber = new MecanographicNumber(mecanographicNumber);
        return this;
    }

    public UserBuilder withPhoneNumber(final String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }

    public UserBuilder withEmail(final Email email) {
        this.email = email;
        return this;
    }

    public UserBuilder withPosition(final String position) {
        this.position = position;
        return this;
    }

    public UserBuilder withSecurityClearance(final SecurityClearance securityClearance) {
        this.securityClearance = securityClearance;
        return this;
    }

    public UserBuilder withSkillsAssessmentDate(final LocalDate skillsAssessmentDate) {
        this.skillsAssessmentDate = skillsAssessmentDate;
        return this;
    }

    @Override
    public User build() {
        return new User(systemUser, mecanographicNumber, phoneNumber, email,
                position, securityClearance, skillsAssessmentDate);
    }
}
