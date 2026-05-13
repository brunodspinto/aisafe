package aisafe.flightplan.domain;

/**
 * Lifecycle status of a {@link FlightPlan}.
 */
public enum FlightPlanStatus {
    /** Newly created from a DSL file; not yet validated. */
    DRAFT,
    /** Has passed all validation checks (US080, US081). */
    VALIDATED,
    /** Has been successfully tested / simulated (US085). */
    TESTED
}