package aisafe.aircontrolarea.domain;
import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Embeddable;
import java.util.Objects;



/**
 * Value Object representing the geographical boundaries of an Air Control Area.
 */
@Embeddable
public class GeoBoundary implements ValueObject {

    private static final long serialVersionUID = 1L;

    private double northLatitude;
    private double southLatitude;
    private double eastLongitude;
    private double westLongitude;

    /**
     * Empty/protected constructor required by JPA (ORM).
     * It should not be used by business logic.
     */
    protected GeoBoundary() {
        // for ORM only
    }

    /**
     * Constructor with business rules validation.
     */
    public GeoBoundary(double northLatitude, double southLatitude, double eastLongitude, double westLongitude) {
        validateCoordinates(northLatitude, southLatitude, eastLongitude, westLongitude);

        this.northLatitude = northLatitude;
        this.southLatitude = southLatitude;
        this.eastLongitude = eastLongitude;
        this.westLongitude = westLongitude;
    }

    /**
     * Validates the business rules of the geographical coordinates.
     */
    private void validateCoordinates(double north, double south, double east, double west) {
        if (north < -90 || north > 90) {
            throw new IllegalArgumentException("North latitude must be between -90 and 90 degrees.");
        }
        if (south < -90 || south > 90) {
            throw new IllegalArgumentException("South latitude must be between -90 and 90 degrees.");
        }
        if (east < -180 || east > 180) {
            throw new IllegalArgumentException("East longitude must be between -180 and 180 degrees.");
        }
        if (west < -180 || west > 180) {
            throw new IllegalArgumentException("West longitude must be between -180 and 180 degrees.");
        }
        if (north <= south) {
            throw new IllegalArgumentException("North latitude must be strictly greater than South latitude.");
        }
        // Note: East longitude is not necessarily greater than West longitude due to the antimeridian,
        // so that direct relationship is not validated here.
    }

    // Access methods (Getters)

    public double northLatitude() {
        return northLatitude;
    }

    public double southLatitude() {
        return southLatitude;
    }

    public double eastLongitude() {
        return eastLongitude;
    }

    public double westLongitude() {
        return westLongitude;
    }

    // Since it's a Value Object, equality is based on attributes, not identity

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GeoBoundary that = (GeoBoundary) o;
        return Double.compare(that.northLatitude, northLatitude) == 0 &&
                Double.compare(that.southLatitude, southLatitude) == 0 &&
                Double.compare(that.eastLongitude, eastLongitude) == 0 &&
                Double.compare(that.westLongitude, westLongitude) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(northLatitude, southLatitude, eastLongitude, westLongitude);
    }

    @Override
    public String toString() {
        return String.format("Boundaries [North: %.4f, South: %.4f, East: %.4f, West: %.4f]",
                northLatitude, southLatitude, eastLongitude, westLongitude);
    }
}
