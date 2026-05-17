package aisafe.flightplan.repositories;

import aisafe.flightplan.domain.FlightPlan;
import aisafe.flightplan.domain.FlightPlanDesignator;
import eapli.framework.domain.repositories.DomainRepository;

/**
 * Repository interface for FlightPlan aggregate.
 */
public interface FlightPlanRepository extends DomainRepository<FlightPlanDesignator, FlightPlan> {
}
