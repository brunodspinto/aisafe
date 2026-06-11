package aisafe.simulation.domain;

import eapli.framework.domain.model.ValueObject;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Value object describing one safety violation detected during a simulation, including its
 * timestamp and the position / velocity vector of the involved aircraft. Supports AC111.3.
 */
public final class SafetyViolation implements ValueObject {

    private final String description;
    private final String flightDesignator;
    private final LocalDateTime timestamp;
    private final double latitude;
    private final double longitude;
    private final double altitude;
    private final double speed;
    private final double heading;

    /**
     * @param description      a short description (e.g. {@code "proximity with FLIGHT_02"})
     * @param flightDesignator the flight involved (non-blank)
     * @param timestamp        when the violation occurred (non-null)
     * @param latitude         latitude (decimal degrees)
     * @param longitude        longitude (decimal degrees)
     * @param altitude         altitude (metres)
     * @param speed            speed (knots)
     * @param heading          heading (degrees)
     * @throws IllegalArgumentException if {@code flightDesignator} is blank or {@code timestamp} is null
     */
    public SafetyViolation(final String description, final String flightDesignator,
                           final LocalDateTime timestamp, final double latitude,
                           final double longitude, final double altitude,
                           final double speed, final double heading) {
        if (flightDesignator == null || flightDesignator.isBlank()) {
            throw new IllegalArgumentException("Flight designator cannot be null or blank.");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Violation timestamp cannot be null.");
        }
        this.description = description == null ? "" : description.trim();
        this.flightDesignator = flightDesignator.trim();
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.speed = speed;
        this.heading = heading;
    }

    public String description() { return description; }

    public String flightDesignator() { return flightDesignator; }

    public LocalDateTime timestamp() { return timestamp; }

    public double latitude() { return latitude; }

    public double longitude() { return longitude; }

    public double altitude() { return altitude; }

    public double speed() { return speed; }

    public double heading() { return heading; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof SafetyViolation other)) return false;
        return Double.compare(latitude, other.latitude) == 0
                && Double.compare(longitude, other.longitude) == 0
                && Double.compare(altitude, other.altitude) == 0
                && Double.compare(speed, other.speed) == 0
                && Double.compare(heading, other.heading) == 0
                && description.equals(other.description)
                && flightDesignator.equals(other.flightDesignator)
                && timestamp.equals(other.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, flightDesignator, timestamp,
                latitude, longitude, altitude, speed, heading);
    }

    @Override
    public String toString() {
        return String.format("%s | %s | lat=%.4f lon=%.4f alt=%.0fm (%s)",
                timestamp, flightDesignator, latitude, longitude, altitude, description);
    }
}
