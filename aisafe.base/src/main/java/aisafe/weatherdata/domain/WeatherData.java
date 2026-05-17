package aisafe.weatherdata.domain;

import aisafe.aircontrolarea.domain.AirControlAreaCode;
import eapli.framework.domain.model.AggregateRoot;
import eapli.framework.domain.model.DomainEntities;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Entity and Aggregate Root representing meteorological conditions
 * recorded for a specific Air Control Area at a point in time.
 *
 * <p>The area is referenced by its code (external reference) rather than
 * an embedded entity, to maintain low coupling with the AirControlArea aggregate.</p>
 */
@Entity
@Table(name = "T_WEATHER_DATA")
public class WeatherData implements AggregateRoot<Long> {

    private static final Set<String> VALID_DIRECTIONS = Set.of(
        "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
        "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"
    );

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "area_code", nullable = false))
    private AirControlAreaCode areaCode;

    @Embedded
    private WeatherSource source;

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(nullable = false)
    private double temperature;

    @Column(nullable = false)
    private double windSpeed;

    @Column(nullable = false)
    private String windDirection;

    @Column(nullable = false)
    private double pressure;

    @Column(nullable = false)
    private double visibility;

    /**
     * Protected constructor required by JPA (ORM).
     */
    protected WeatherData() {
        // for ORM only
    }

    /**
     * Creates a valid WeatherData record linked to an existing Air Control Area.
     */
    public WeatherData(final AirControlAreaCode areaCode, final WeatherSource source, final LocalDateTime date,
                       final double temperature, final double windSpeed, final String windDirection,
                       final double pressure, final double visibility) {
        if (areaCode == null) {
            throw new IllegalArgumentException("Area code cannot be null.");
        }
        if (source == null) {
            throw new IllegalArgumentException("Weather source cannot be null.");
        }
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null.");
        }
        if (windSpeed < 0) {
            throw new IllegalArgumentException("Wind speed cannot be negative.");
        }
        if (windDirection == null || windDirection.isBlank()) {
            throw new IllegalArgumentException("Wind direction cannot be null or blank.");
        }
        if (!VALID_DIRECTIONS.contains(windDirection.trim().toUpperCase())) {
            throw new IllegalArgumentException("Wind direction must be a valid compass direction (e.g. N, NE, SW).");
        }
        if (visibility < 0) {
            throw new IllegalArgumentException("Visibility cannot be negative.");
        }
        this.areaCode = areaCode;
        this.source = source;
        this.date = date;
        this.temperature = temperature;
        this.windSpeed = windSpeed;
        this.windDirection = windDirection.trim().toUpperCase();
        this.pressure = pressure;
        this.visibility = visibility;
    }

    public String areaCode() {
        return areaCode.toString();
    }

    public WeatherSource source() {
        return source;
    }

    public LocalDateTime date() {
        return date;
    }

    public double temperature() {
        return temperature;
    }

    public double windSpeed() {
        return windSpeed;
    }

    public String windDirection() {
        return windDirection;
    }

    public double pressure() {
        return pressure;
    }

    public double visibility() {
        return visibility;
    }

    @Override
    public Long identity() {
        return id;
    }

    @Override
    public boolean sameAs(final Object other) {
        return DomainEntities.areEqual(this, other);
    }

    @Override
    public boolean equals(final Object o) {
        return DomainEntities.areEqual(this, o);
    }

    @Override
    public int hashCode() {
        return DomainEntities.hashCode(this);
    }

    @Override
    public String toString() {
        return String.format("WeatherData [area=%s, date=%s, temp=%.1f, wind=%.1f %s, pressure=%.1f, visibility=%.1f]",
                areaCode, date, temperature, windSpeed, windDirection, pressure, visibility);
    }
}
