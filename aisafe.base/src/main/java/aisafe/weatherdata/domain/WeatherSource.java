package aisafe.weatherdata.domain;

import eapli.framework.domain.model.ValueObject;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Value Object representing the origin and format of the weather data provider.
 */
@Embeddable
public class WeatherSource implements ValueObject {

    private static final long serialVersionUID = 1L;

    private String provider;
    private String format;

    /**
     * Protected constructor required by JPA (ORM).
     */
    protected WeatherSource() {
        // for ORM only
    }

    /**
     * Creates a WeatherSource with the given provider and format.
     */
    public WeatherSource(final String provider, final String format) {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("Weather source provider cannot be null or blank.");
        }
        if (format == null || format.isBlank()) {
            throw new IllegalArgumentException("Weather source format cannot be null or blank.");
        }
        this.provider = provider.trim();
        this.format = format.trim();
    }

    public String provider() {
        return provider;
    }

    public String format() {
        return format;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final WeatherSource that = (WeatherSource) o;
        return Objects.equals(provider, that.provider) && Objects.equals(format, that.format);
    }

    @Override
    public int hashCode() {
        return Objects.hash(provider, format);
    }

    @Override
    public String toString() {
        return provider + " (" + format + ")";
    }
}
