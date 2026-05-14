package aisafe.infrastructure.persistence.jpa;

import aisafe.aircraft.repositories.AircraftRepository;
import aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import aisafe.airtransportcompany.repositories.AirTransportCompanyRepository;
import aisafe.enginemodel.repositories.EngineModelRepository;
import aisafe.infrastructure.persistence.RepositoryFactory;
import aisafe.usermanagement.repositories.UserRepository;
import aisafe.flightplan.repositories.FlightPlanRepository;
import aisafe.weatherdata.repositories.WeatherDataRepository;
import aisafe.airport.repositories.AirportRepository;
import aisafe.maker.repositories.MakerRepository;
import aisafe.aircraftmodel.repositories.AircraftModelRepository;
import aisafe.collaborator.repositories.CollaboratorRepository;
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
        return new JpaUserRepository(tx);
    }

    @Override
    public UserRepository users() {
        return new JpaUserRepository(PERSISTENCE_UNIT);
    }

    @Override
    public AirTransportCompanyRepository airTransportCompanies(final TransactionalContext tx) {
        return new JpaAirTransportCompanyRepository(tx);
    }

    @Override
    public AirTransportCompanyRepository airTransportCompanies() {
        return new JpaAirTransportCompanyRepository(PERSISTENCE_UNIT);
    }

    @Override
    public AirControlAreaRepository airControlAreas(final TransactionalContext tx) {
        return new JpaAirControlAreaRepository(tx);
    }

    @Override
    public AirControlAreaRepository airControlAreas() {
        return new JpaAirControlAreaRepository(PERSISTENCE_UNIT);
    }

    @Override
    public FlightPlanRepository flightPlans(final TransactionalContext tx) {
        return new JpaFlightPlanRepository(tx);
    }

    @Override
    public FlightPlanRepository flightPlans() {
        return new JpaFlightPlanRepository(PERSISTENCE_UNIT);
    }

    @Override
    public WeatherDataRepository weatherData(final TransactionalContext tx) {
        return new JpaWeatherDataRepository(tx);
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
    public AirportRepository airports(final TransactionalContext tx) {
        return new JpaAirportRepository(tx);
    }

    @Override
    public AirportRepository airports() {
        return new JpaAirportRepository(PERSISTENCE_UNIT);
    }

    @Override
    public EngineModelRepository engineModels(final TransactionalContext tx) {
        return new JpaEngineModelRepository(tx);
    }

    @Override
    public EngineModelRepository engineModels() {
        return new JpaEngineModelRepository(PERSISTENCE_UNIT);
    }

    @Override
    public MakerRepository makers(final TransactionalContext tx) {
        return new JpaMakerRepository(tx);
    }

    @Override
    public MakerRepository makers() {
        return new JpaMakerRepository(PERSISTENCE_UNIT);
    }

    @Override
    public AircraftModelRepository aircraftModels(final TransactionalContext tx) {
        return new JpaAircraftModelRepository(tx);
    }

    @Override
    public AircraftModelRepository aircraftModels() {
        return new JpaAircraftModelRepository(PERSISTENCE_UNIT);
    }

    @Override
    public CollaboratorRepository collaborators(final TransactionalContext tx) {
        return new JpaCollaboratorRepository(tx);
    }

    @Override
    public CollaboratorRepository collaborators() {
        return new JpaCollaboratorRepository(PERSISTENCE_UNIT);
    }

    @Override
    public AircraftRepository aircraft(final TransactionalContext tx) {
        return new JpaAircraftRepository(tx);
    }

    @Override
    public AircraftRepository aircraft() {
        return new JpaAircraftRepository(PERSISTENCE_UNIT);
    }
}
