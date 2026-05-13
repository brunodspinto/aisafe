package aisafe.aircraft.domain;

/**
 * Represents the operational status of an {@link Aircraft}.
 */
public enum OperationalStatus {
    /** Aircraft is in service and eligible for operations. */
    ACTIVE,
    /** Aircraft has been permanently retired from service. */
    DECOMMISSIONED
}
