package aisafe.infrastructure.persistence.jpa;

import aisafe.flightplan.domain.FlightPlan;
import aisafe.flightplan.domain.FlightPlanDesignator;
import aisafe.flightplan.domain.FlightPlanStatus;
import aisafe.flightplan.repositories.FlightPlanRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

import java.util.HashMap;
import java.util.Map;

public class JpaFlightPlanRepository
        extends JpaAutoTxRepository<FlightPlan, FlightPlanDesignator, FlightPlanDesignator>
        implements FlightPlanRepository {

    public JpaFlightPlanRepository(final String puName) {
        super(puName, "designator");
    }

    public JpaFlightPlanRepository(final TransactionalContext tx) {
        super(tx, "designator");
    }

    @Override
    public boolean hasFlightPlanAssignedTo(final Long pilotId) {
        final Map<String, Object> params = new HashMap<>();
        params.put("pilotId", pilotId);
        return matchOne("e.assignedPilotId = :pilotId", params).isPresent();
    }

    @Override
    public Iterable<FlightPlan> findAllValidated() {
        final Map<String, Object> params = new HashMap<>();
        params.put("status", FlightPlanStatus.VALIDATED);
        return match("e.status = :status", params);
    }
}
