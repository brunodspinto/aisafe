package aisafe.usermanagement.domain;

import eapli.framework.infrastructure.authz.domain.model.PasswordPolicy;
import eapli.framework.strings.util.StringPredicates;

/**
 * AISafe password policy.
 * A password is valid when it is at least 6 characters long, contains at least one digit,
 * and contains at least one uppercase letter.
 */
public class AiSafePasswordPolicy implements PasswordPolicy {

    /**
     * @param rawPassword the plaintext password to evaluate
     * @return {@code true} if the password satisfies the policy; {@code false} otherwise
     */
    @Override
    public boolean isSatisfiedBy(final String rawPassword) {
        if (StringPredicates.isNullOrEmpty(rawPassword) || rawPassword.length() < 6)
            return false;
        if (!StringPredicates.containsDigit(rawPassword))
            return false;
        return StringPredicates.containsCapital(rawPassword);
    }
}
