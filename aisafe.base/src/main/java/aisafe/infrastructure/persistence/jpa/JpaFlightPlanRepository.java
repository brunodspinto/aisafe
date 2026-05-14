package aisafe.infrastructure.persistence.jpa;

import aisafe.flightplan.domain.FlightPlan;
import aisafe.flightplan.domain.FlightPlanDesignator;
import aisafe.flightplan.repositories.FlightPlanRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

public class JpaFlightPlanRepository
        extends JpaAutoTxRepository<FlightPlan, FlightPlanDesignator, FlightPlanDesignator>
        implements FlightPlanRepository {

    public JpaFlightPlanRepository(final String puName) {
        super(puName, "designator");
    }

    public JpaFlightPlanRepository(final TransactionalContext tx) {
        super(tx, "designator");
    }
}
