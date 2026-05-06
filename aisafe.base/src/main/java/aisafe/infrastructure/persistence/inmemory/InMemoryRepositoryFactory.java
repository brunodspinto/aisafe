package aisafe.infrastructure.persistence.inmemory;

import aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import aisafe.airtransportcompany.repositories.AirTransportCompanyRepository;
import aisafe.infrastructure.persistence.RepositoryFactory;
import aisafe.weatherdata.repositories.WeatherDataRepository;
import aisafe.usermanagement.domain.AiSafePasswordPolicy;
import aisafe.usermanagement.domain.AiSafeRoles;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;

public class InMemoryRepositoryFactory implements RepositoryFactory {

    @Override
    public eapli.framework.infrastructure.authz.domain.repositories.UserRepository systemUsers(
            final TransactionalContext tx) {
        final var repo = new InMemoryUserRepository();
        final var builder = new SystemUserBuilder(new AiSafePasswordPolicy(), new PlainTextEncoder());
        builder.withUsername("admin").withPassword("Password1")
                .withName("System", "Admin")
                .withEmail("admin@aisafe.com")
                .withRoles(AiSafeRoles.ADMIN);
        repo.save(builder.build());
        return repo;
    }

    @Override
    public eapli.framework.infrastructure.authz.domain.repositories.UserRepository systemUsers() {
        return systemUsers(null);
    }

    @Override
    public aisafe.usermanagement.repositories.UserRepository users(final TransactionalContext tx) {
        return new aisafe.infrastructure.persistence.inmemory.InMemoryAiSafeUserRepository();
    }

    @Override
    public aisafe.usermanagement.repositories.UserRepository users() {
        return users(null);
    }

    @Override
    public AirTransportCompanyRepository airTransportCompanies() {
        return new InMemoryAirTransportCompanyRepository();
    }

    @Override
    public AirControlAreaRepository airControlAreas() {
        return new InMemoryAirControlAreaRepository();
    }

    @Override
    public WeatherDataRepository weatherData() {
        return new InMemoryWeatherDataRepository();
    }

    @Override
    public TransactionalContext newTransactionalContext() {
        return null;
    }
}
