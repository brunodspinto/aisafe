package aisafe.aircraftmodel.repositories;

import aisafe.aircraftmodel.domain.AircraftModel;
import aisafe.maker.domain.Maker;
import eapli.framework.domain.repositories.DomainRepository;

import java.util.Optional;

public interface AircraftModelRepository extends DomainRepository<Long, AircraftModel> {

    Optional<AircraftModel> findByModelNameAndMaker(String modelName, Maker maker);
}
