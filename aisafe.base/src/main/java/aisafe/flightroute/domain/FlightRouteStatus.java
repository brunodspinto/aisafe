package aisafe.flightroute.domain;

/**
 * Operational status of a {@link FlightRoute}.
 */
public enum FlightRouteStatus {
    /** The route is available for flight plan creation. */
    ACTIVE,
    /** The route has been deactivated and is no longer available. */
    INACTIVE
}