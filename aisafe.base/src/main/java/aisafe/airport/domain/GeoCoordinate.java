package aisafe.airport.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Embeddable;

import java.util.Objects;

/**
 * Value Object representing a geographic point (latitude + longitude).
 */
@Embeddable
public class GeoCoordinate implements ValueObject {

    private double latitude;
    private double longitude;

    public GeoCoordinate(final double latitude, final double longitude) {
        if (latitude < -90 || latitude > 90)
            throw new IllegalArgumentException("Latitude must be between -90 and 90.");
        if (longitude < -180 || longitude > 180)
            throw new IllegalArgumentException("Longitude must be between -180 and 180.");
        this.latitude = latitude;
        this.longitude = longitude;
    }

    protected GeoCoordinate() {}

    public double latitude() { return latitude; }
    public double longitude() { return longitude; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof GeoCoordinate)) return false;
        final GeoCoordinate that = (GeoCoordinate) o;
        return Double.compare(that.latitude, latitude) == 0
                && Double.compare(that.longitude, longitude) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude);
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.US, "(%.4f, %.4f)", latitude, longitude);
    }
}
