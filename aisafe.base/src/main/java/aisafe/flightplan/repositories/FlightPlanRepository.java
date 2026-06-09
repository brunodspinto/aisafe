package aisafe.flightplan.repositories;

import aisafe.flightplan.domain.FlightPlan;
import aisafe.flightplan.domain.FlightPlanDesignator;
import eapli.framework.domain.repositories.DomainRepository;

/**
 * Repository interface for FlightPlan aggregate.
 */
public interface FlightPlanRepository extends DomainRepository<FlightPlanDesignator, FlightPlan> {

    /**
     * Returns {@code true} if any flight plan has the given pilot assigned.
     *
     * @param pilotId the pilot identity to check
     * @return {@code true} if at least one flight plan references this pilot
     */
    boolean hasFlightPlanAssignedTo(Long pilotId);

    /**
     * Returns all flight plans currently in {@code VALIDATED} status.
     * Used by US085 to list plans that are eligible for simulation testing.
     *
     * @return an iterable of all VALIDATED flight plans (may be empty, never null)
     */
    Iterable<FlightPlan> findAllValidated();
}
