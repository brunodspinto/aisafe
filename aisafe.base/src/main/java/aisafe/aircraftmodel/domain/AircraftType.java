package aisafe.aircraftmodel.domain;

/**
 * Classifies an {@link AircraftModel} by its primary purpose.
 */
public enum AircraftType {
    /** Designed primarily to carry passengers. */
    PASSENGER,
    /** Designed primarily to carry cargo/freight. */
    CARGO,
    /** Configured for both passengers and cargo. */
    MIXED
}
