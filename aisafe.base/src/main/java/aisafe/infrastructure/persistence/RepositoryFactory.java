package aisafe.infrastructure.persistence;

import aisafe.aircraft.repositories.AircraftRepository;
import aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import aisafe.airtransportcompany.repositories.AirTransportCompanyRepository;
import aisafe.enginemodel.repositories.EngineModelRepository;
import aisafe.usermanagement.repositories.UserRepository;
import aisafe.flightplan.repositories.FlightPlanRepository;
import aisafe.weatherdata.repositories.WeatherDataRepository;
import aisafe.airport.repositories.AirportRepository;
import aisafe.maker.repositories.MakerRepository;
import aisafe.aircraftmodel.repositories.AircraftModelRepository;
import aisafe.collaborator.repositories.CollaboratorRepository;
import aisafe.pilot.repositories.PilotRepository;
import aisafe.flightroute.repositories.FlightRepository;
import aisafe.flightroute.repositories.FlightRouteRepository;
import eapli.framework.domain.repositories.TransactionalContext;

public interface RepositoryFactory {

    TransactionalContext newTransactionalContext();

    eapli.framework.infrastructure.authz.domain.repositories.UserRepository systemUsers(TransactionalContext tx);

    eapli.framework.infrastructure.authz.domain.repositories.UserRepository systemUsers();

    UserRepository users(TransactionalContext tx);

    UserRepository users();

    AirTransportCompanyRepository airTransportCompanies(TransactionalContext tx);

    AirTransportCompanyRepository airTransportCompanies();

    AirControlAreaRepository airControlAreas(TransactionalContext tx);

    AirControlAreaRepository airControlAreas();

    FlightPlanRepository flightPlans(TransactionalContext tx);

    FlightPlanRepository flightPlans();

    WeatherDataRepository weatherData(TransactionalContext tx);

    WeatherDataRepository weatherData();

    AirportRepository airports(TransactionalContext tx);

    AirportRepository airports();

    EngineModelRepository engineModels(TransactionalContext tx);

    EngineModelRepository engineModels();

    MakerRepository makers(TransactionalContext tx);

    MakerRepository makers();

    AircraftModelRepository aircraftModels(TransactionalContext tx);

    AircraftModelRepository aircraftModels();

    CollaboratorRepository collaborators(TransactionalContext tx);

    CollaboratorRepository collaborators();

    AircraftRepository aircraft(TransactionalContext tx);

    AircraftRepository aircraft();

    PilotRepository pilots(TransactionalContext tx);

    PilotRepository pilots();

    FlightRouteRepository flightRoutes(TransactionalContext tx);

    FlightRouteRepository flightRoutes();

    FlightRepository flights(TransactionalContext tx);

    FlightRepository flights();
}
