package aisafe.infrastructure.persistence.jpa;

import aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import aisafe.airtransportcompany.repositories.AirTransportCompanyRepository;
import aisafe.infrastructure.persistence.RepositoryFactory;
import aisafe.usermanagement.repositories.UserRepository;
import aisafe.flightplan.repositories.FlightPlanRepository;
import aisafe.weatherdata.repositories.WeatherDataRepository;
import aisafe.airport.repositories.AirportRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.authz.repositories.impl.jpa.JpaAutoTxUserRepository;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;
import java.util.HashMap;

public class JpaRepositoryFactory implements RepositoryFactory {

    private static final String PERSISTENCE_UNIT = "aisafe";

    @Override
    public eapli.framework.infrastructure.authz.domain.repositories.UserRepository systemUsers(
            final TransactionalContext tx) {
        return new JpaAutoTxUserRepository(tx);
    }

    @Override
    public eapli.framework.infrastructure.authz.domain.repositories.UserRepository systemUsers() {
        return new JpaAutoTxUserRepository(PERSISTENCE_UNIT, new HashMap<>());
    }

    @Override
    public UserRepository users(final TransactionalContext tx) {
        return new JpaUserRepository(PERSISTENCE_UNIT);
    }

    @Override
    public UserRepository users() {
        return new JpaUserRepository(PERSISTENCE_UNIT);
    }

    @Override
    public AirTransportCompanyRepository airTransportCompanies() {
        return new JpaAirTransportCompanyRepository(PERSISTENCE_UNIT);
    }

    @Override
    public AirControlAreaRepository airControlAreas() {
        return new JpaAirControlAreaRepository(PERSISTENCE_UNIT);
    }

    @Override
    public FlightPlanRepository flightPlans() {
        return new JpaFlightPlanRepository(PERSISTENCE_UNIT);
    }

    @Override
    public WeatherDataRepository weatherData() {
        return new JpaWeatherDataRepository(PERSISTENCE_UNIT);
    }

    @Override
    public TransactionalContext newTransactionalContext() {
        return JpaAutoTxRepository.buildTransactionalContext(PERSISTENCE_UNIT, new HashMap<>());
    }

    @Override
    public AirportRepository airports() {
        return new JpaAirportRepository(PERSISTENCE_UNIT);
    }
}
