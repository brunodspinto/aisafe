package aisafe.infrastructure.persistence.inmemory;

import aisafe.aircraft.repositories.AircraftRepository;
import aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import aisafe.airtransportcompany.repositories.AirTransportCompanyRepository;
import aisafe.enginemodel.repositories.EngineModelRepository;
import aisafe.infrastructure.persistence.RepositoryFactory;
import aisafe.weatherdata.repositories.WeatherDataRepository;
import aisafe.usermanagement.domain.AiSafePasswordPolicy;
import aisafe.usermanagement.domain.AiSafeRoles;
import aisafe.flightplan.repositories.FlightPlanRepository;
import aisafe.airport.repositories.AirportRepository;
import aisafe.maker.repositories.MakerRepository;
import aisafe.aircraftmodel.repositories.AircraftModelRepository;
import aisafe.collaborator.repositories.CollaboratorRepository;
import aisafe.pilot.repositories.PilotRepository;
import aisafe.flightroute.repositories.FlightRouteRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryTransactionalContext;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;

/**
 * In-memory factory used for tests and the {@code --inmemory} runtime profile.
 * Each repository is cached so that data persists across multiple calls to
 * {@code PersistenceContext.repositories().xxx()} within the same JVM run.
 */
public class InMemoryRepositoryFactory implements RepositoryFactory {

    private eapli.framework.infrastructure.authz.domain.repositories.UserRepository systemUsersRepo;
    private aisafe.usermanagement.repositories.UserRepository usersRepo;
    private AirTransportCompanyRepository airTransportCompaniesRepo;
    private AirControlAreaRepository airControlAreasRepo;
    private FlightPlanRepository flightPlansRepo;
    private WeatherDataRepository weatherDataRepo;
    private AirportRepository airportsRepo;
    private EngineModelRepository engineModelsRepo;
    private MakerRepository makersRepo;
    private AircraftModelRepository aircraftModelsRepo;
    private CollaboratorRepository collaboratorsRepo;
    private AircraftRepository aircraftRepo;
    private PilotRepository pilotsRepo;
    private FlightRouteRepository flightRoutesRepo;

    @Override
    public synchronized eapli.framework.infrastructure.authz.domain.repositories.UserRepository systemUsers(
            final TransactionalContext tx) {
        if (systemUsersRepo == null) {
            final var repo = new InMemoryUserRepository();
            final var builder = new SystemUserBuilder(new AiSafePasswordPolicy(), new PlainTextEncoder());
            builder.withUsername("admin").withPassword("Password1")
                    .withName("System", "Admin")
                    .withEmail("admin@aisafe.com")
                    .withRoles(AiSafeRoles.ADMIN);
            repo.save(builder.build());
            systemUsersRepo = repo;
        }
        return systemUsersRepo;
    }

    @Override
    public eapli.framework.infrastructure.authz.domain.repositories.UserRepository systemUsers() {
        return systemUsers(null);
    }

    @Override
    public synchronized aisafe.usermanagement.repositories.UserRepository users(final TransactionalContext tx) {
        if (usersRepo == null) usersRepo = new InMemoryAiSafeUserRepository();
        return usersRepo;
    }

    @Override
    public aisafe.usermanagement.repositories.UserRepository users() {
        return users(null);
    }

    @Override
    public synchronized AirTransportCompanyRepository airTransportCompanies(final TransactionalContext tx) {
        if (airTransportCompaniesRepo == null) airTransportCompaniesRepo = new InMemoryAirTransportCompanyRepository();
        return airTransportCompaniesRepo;
    }

    @Override
    public AirTransportCompanyRepository airTransportCompanies() {
        return airTransportCompanies(null);
    }

    @Override
    public synchronized AirControlAreaRepository airControlAreas(final TransactionalContext tx) {
        if (airControlAreasRepo == null) airControlAreasRepo = new InMemoryAirControlAreaRepository();
        return airControlAreasRepo;
    }

    @Override
    public AirControlAreaRepository airControlAreas() {
        return airControlAreas(null);
    }

    @Override
    public synchronized FlightPlanRepository flightPlans(final TransactionalContext tx) {
        if (flightPlansRepo == null) flightPlansRepo = new InMemoryFlightPlanRepository();
        return flightPlansRepo;
    }

    @Override
    public FlightPlanRepository flightPlans() {
        return flightPlans(null);
    }

    @Override
    public synchronized WeatherDataRepository weatherData(final TransactionalContext tx) {
        if (weatherDataRepo == null) weatherDataRepo = new InMemoryWeatherDataRepository();
        return weatherDataRepo;
    }

    @Override
    public WeatherDataRepository weatherData() {
        return weatherData(null);
    }

    @Override
    public TransactionalContext newTransactionalContext() {
        return new InMemoryTransactionalContext();
    }

    @Override
    public synchronized AirportRepository airports(final TransactionalContext tx) {
        if (airportsRepo == null) airportsRepo = new InMemoryAirportRepository();
        return airportsRepo;
    }

    @Override
    public AirportRepository airports() {
        return airports(null);
    }

    @Override
    public synchronized EngineModelRepository engineModels(final TransactionalContext tx) {
        if (engineModelsRepo == null) engineModelsRepo = new InMemoryEngineModelRepository();
        return engineModelsRepo;
    }

    @Override
    public EngineModelRepository engineModels() {
        return engineModels(null);
    }

    @Override
    public synchronized MakerRepository makers(final TransactionalContext tx) {
        if (makersRepo == null) makersRepo = new InMemoryMakerRepository();
        return makersRepo;
    }

    @Override
    public MakerRepository makers() {
        return makers(null);
    }

    @Override
    public synchronized AircraftModelRepository aircraftModels(final TransactionalContext tx) {
        if (aircraftModelsRepo == null) aircraftModelsRepo = new InMemoryAircraftModelRepository();
        return aircraftModelsRepo;
    }

    @Override
    public AircraftModelRepository aircraftModels() {
        return aircraftModels(null);
    }

    @Override
    public synchronized CollaboratorRepository collaborators(final TransactionalContext tx) {
        if (collaboratorsRepo == null) collaboratorsRepo = new InMemoryCollaboratorRepository();
        return collaboratorsRepo;
    }

    @Override
    public CollaboratorRepository collaborators() {
        return collaborators(null);
    }

    @Override
    public synchronized AircraftRepository aircraft(final TransactionalContext tx) {
        if (aircraftRepo == null) aircraftRepo = new InMemoryAircraftRepository();
        return aircraftRepo;
    }

    @Override
    public AircraftRepository aircraft() {
        return aircraft(null);
    }

    @Override
    public synchronized PilotRepository pilots(final TransactionalContext tx) {
        if (pilotsRepo == null) pilotsRepo = new InMemoryPilotRepository();
        return pilotsRepo;
    }

    @Override
    public PilotRepository pilots() {
        return pilots(null);
    }

    @Override
    public synchronized FlightRouteRepository flightRoutes(final TransactionalContext tx) {
        if (flightRoutesRepo == null) flightRoutesRepo = new InMemoryFlightRouteRepository();
        return flightRoutesRepo;
    }

    @Override
    public FlightRouteRepository flightRoutes() {
        return flightRoutes(null);
    }
}
