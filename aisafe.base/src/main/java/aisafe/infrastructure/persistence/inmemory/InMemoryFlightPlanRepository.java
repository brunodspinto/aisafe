package aisafe.infrastructure.persistence.inmemory;

import aisafe.flightplan.domain.FlightPlan;
import aisafe.flightplan.domain.FlightPlanDesignator;
import aisafe.flightplan.repositories.FlightPlanRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

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
}
