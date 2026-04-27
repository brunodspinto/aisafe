/*
 * Copyright (c) 2013-2024 the original author or authors.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package eapli.exemplo.userbackoffice.application;

import java.util.Optional;

import eapli.exemplo.infrastructure.persistence.PersistenceContext;
import eapli.exemplo.usermanagement.domain.AiSafeRoles;
import eapli.exemplo.userbackoffice.domain.User;
import eapli.exemplo.userbackoffice.domain.MecanographicNumber;
import eapli.exemplo.userbackoffice.repositories.AiSafeUserRepository;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.Username;

/**
 * @author mcn
 */
public class UserService {

	private final AuthorizationService authz = AuthzRegistry.authorizationService();
	private final AiSafeUserRepository repo = PersistenceContext.repositories().aiSafeUsers();

	public Optional<User> findByMecNumber(final String mecNumber) {
		authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ADMIN, AiSafeRoles.BACKOFFICE_OPERATOR);
		return repo.ofIdentity(MecanographicNumber.valueOf(mecNumber));
	}

	public Optional<User> findByUsername(final Username user) {
		authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.ADMIN, AiSafeRoles.BACKOFFICE_OPERATOR);
		return repo.findByUsername(user);
	}
}
