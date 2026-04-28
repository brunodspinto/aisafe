/*
 * Copyright (c) 2013-2024 the original author or authors.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package eapli.exemplo.persistence.impl.inmemory;

import java.util.ArrayList;
import java.util.Optional;

import eapli.exemplo.userbackoffice.domain.MecanographicNumber;
import eapli.exemplo.userbackoffice.domain.User;
import eapli.exemplo.userbackoffice.repositories.AiSafeUserRepository;
import eapli.framework.infrastructure.authz.domain.model.Username;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

/**
 * In-memory repository for AISafe client users.
 */
public class InMemoryAiSafeUserRepository extends InMemoryDomainRepository<User, MecanographicNumber>
        implements AiSafeUserRepository {

    static {
        InMemoryInitializer.init();
    }

    @Override
    public Optional<User> findByUsername(final Username name) {
        for (final User user : findAll()) {
            if (user.user() != null && user.user().username().equals(name)) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    @Override
    public Iterable<User> findAllActive() {
        final var activeUsers = new ArrayList<User>();
        for (final User user : findAll()) {
            if (user.user() != null && user.user().isActive()) {
                activeUsers.add(user);
            }
        }
        return activeUsers;
    }
}



