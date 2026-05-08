package aisafe.infrastructure.persistence;

import aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import aisafe.airtransportcompany.repositories.AirTransportCompanyRepository;
import aisafe.enginemodel.repositories.EngineModelRepository;
import aisafe.usermanagement.repositories.UserRepository;
import aisafe.flightplan.repositories.FlightPlanRepository;
import aisafe.weatherdata.repositories.WeatherDataRepository;
import aisafe.airport.repositories.AirportRepository;
import aisafe.maker.repositories.MakerRepository;
import aisafe.aircraftmodel.repositories.AircraftModelRepository;
import eapli.framework.domain.repositories.TransactionalContext;

public interface RepositoryFactory {

    TransactionalContext newTransactionalContext();

    eapli.framework.infrastructure.authz.domain.repositories.UserRepository systemUsers(TransactionalContext tx);

    eapli.framework.infrastructure.authz.domain.repositories.UserRepository systemUsers();

    UserRepository users(TransactionalContext tx);

    UserRepository users();

    AirTransportCompanyRepository airTransportCompanies();

    AirControlAreaRepository airControlAreas();

    FlightPlanRepository flightPlans();

    WeatherDataRepository weatherData();

    AirportRepository airports();

    EngineModelRepository engineModels();

    MakerRepository makers();

    AircraftModelRepository aircraftModels();
}
