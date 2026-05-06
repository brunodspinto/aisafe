package aisafe.infrastructure.persistence;

import aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import aisafe.airtransportcompany.repositories.AirTransportCompanyRepository;
import aisafe.usermanagement.repositories.UserRepository;
import aisafe.weatherdata.repositories.WeatherDataRepository;
import eapli.framework.domain.repositories.TransactionalContext;

public interface RepositoryFactory {

    TransactionalContext newTransactionalContext();

    eapli.framework.infrastructure.authz.domain.repositories.UserRepository systemUsers(TransactionalContext tx);

    eapli.framework.infrastructure.authz.domain.repositories.UserRepository systemUsers();

    UserRepository users(TransactionalContext tx);

    UserRepository users();

    AirTransportCompanyRepository airTransportCompanies();

    AirControlAreaRepository airControlAreas();

    WeatherDataRepository weatherData();
}
