package aisafe.infrastructure.persistence.inmemory;

import aisafe.flightplan.domain.FlightPlan;
import aisafe.flightplan.domain.FlightPlanDesignator;
import aisafe.flightplan.domain.FlightPlanStatus;
import aisafe.flightplan.repositories.FlightPlanRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

import java.util.ArrayList;
import java.util.List;

public class InMemoryFlightPlanRepository
        extends InMemoryDomainRepository<FlightPlan, FlightPlanDesignator>
        implements FlightPlanRepository {

    @Override
    public boolean hasFlightPlanAssignedTo(final Long pilotId) {
        for (final FlightPlan fp : findAll()) {
            if (pilotId.equals(fp.assignedPilotId())) return true;
        }
        return false;
    }

    @Override
    public Iterable<FlightPlan> findAllValidated() {
        final List<FlightPlan> result = new ArrayList<>();
        for (final FlightPlan fp : findAll()) {
            if (FlightPlanStatus.VALIDATED.equals(fp.status())) {
                result.add(fp);
            }
        }
        return result;
    }
}
