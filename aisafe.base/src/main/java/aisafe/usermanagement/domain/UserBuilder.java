package aisafe.usermanagement.domain;

import eapli.framework.domain.model.DomainFactory;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import java.time.LocalDate;

/**
 * Builder for constructing {@link User} aggregate instances.
 * Follows the DDD {@link DomainFactory} pattern; call {@link #build()} to produce the final object.
 */
public class UserBuilder implements DomainFactory<User> {

    private SystemUser systemUser;
    private MecanographicNumber mecanographicNumber;
    private String phoneNumber;
    private Email email;
    private String position;
    private SecurityClearance securityClearance;
    private LocalDate skillsAssessmentDate;

    /**
     * @param systemUser the underlying EAPLI system user
     * @return this builder
     */
    public UserBuilder withSystemUser(final SystemUser systemUser) {
        this.systemUser = systemUser;
        return this;
    }

    /**
     * @param mecanographicNumber the mecanographic number value object
     * @return this builder
     */
    public UserBuilder withMecanographicNumber(final MecanographicNumber mecanographicNumber) {
        this.mecanographicNumber = mecanographicNumber;
        return this;
    }

    /**
     * @param mecanographicNumber the raw mecanographic number string
     * @return this builder
     */
    public UserBuilder withMecanographicNumber(final String mecanographicNumber) {
        this.mecanographicNumber = new MecanographicNumber(mecanographicNumber);
        return this;
    }

    /**
     * @param phoneNumber the contact phone number
     * @return this builder
     */
    public UserBuilder withPhoneNumber(final String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }

    /**
     * @param email the contact e-mail address value object
     * @return this builder
     */
    public UserBuilder withEmail(final Email email) {
        this.email = email;
        return this;
    }

    /**
     * @param position the job position or title
     * @return this builder
     */
    public UserBuilder withPosition(final String position) {
        this.position = position;
        return this;
    }

    /**
     * @param securityClearance the security clearance value object
     * @return this builder
     */
    public UserBuilder withSecurityClearance(final SecurityClearance securityClearance) {
        this.securityClearance = securityClearance;
        return this;
    }

    /**
     * @param skillsAssessmentDate date of the most recent skills assessment
     * @return this builder
     */
    public UserBuilder withSkillsAssessmentDate(final LocalDate skillsAssessmentDate) {
        this.skillsAssessmentDate = skillsAssessmentDate;
        return this;
    }

    /**
     * Builds and returns the {@link User} instance from the accumulated state.
     *
     * @return a new {@link User}
     */
    @Override
    public User build() {
        return new User(systemUser, mecanographicNumber, phoneNumber, email,
                position, securityClearance, skillsAssessmentDate);
    }
}
