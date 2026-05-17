package aisafe.enginemodel.domain;

/**
 * Classifies the propulsion technology of an {@link EngineModel}.
 */
public enum EngineType {
    /** High-bypass turbofan — most common on commercial jets. */
    TURBOFAN,
    /** Turboprop — shaft-driven propeller for regional aircraft. */
    TURBOPROP,
    /** Pure turbojet — used on older or supersonic aircraft. */
    TURBOJET,
    /** Ramjet — operates only at high speeds, no moving parts. */
    RAMJET,
    /** Electric motor driving a propeller. */
    ELECTRIC_PROPELLER
}
