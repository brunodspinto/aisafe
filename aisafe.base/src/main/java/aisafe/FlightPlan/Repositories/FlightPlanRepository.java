package aisafe.flightplan.repositories;

import aisafe.flightplan.domain.FlightPlan;
import eapli.framework.domain.repositories.DomainRepository;

/**
 * Repository interface for FlightPlan aggregate.
 */
public interface FlightPlanRepository extends DomainRepository<String, FlightPlan> {
}
