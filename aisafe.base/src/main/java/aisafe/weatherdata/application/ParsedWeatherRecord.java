package aisafe.weatherdata.application;

import java.time.LocalDateTime;

/**
 * Plain data carrier holding the fields of one successfully parsed weather record.
 *
 * <p>This is a transport object used between the parser and the controller.
 * It carries raw field values before domain object construction and validation.</p>
 */
public class ParsedWeatherRecord {

    private final String areaCode;
    private final String provider;
    private final String format;
    private final LocalDateTime date;
    private final double temperature;
    private final double windSpeed;
    private final String windDirection;
    private final double pressure;
    private final double visibility;

    public ParsedWeatherRecord(final String areaCode, final String provider, final String format,
                               final LocalDateTime date, final double temperature,
                               final double windSpeed, final String windDirection,
                               final double pressure, final double visibility) {
        this.areaCode = areaCode;
        this.provider = provider;
        this.format = format;
        this.date = date;
        this.temperature = temperature;
        this.windSpeed = windSpeed;
        this.windDirection = windDirection;
        this.pressure = pressure;
        this.visibility = visibility;
    }

    public String areaCode() { return areaCode; }
    public String provider() { return provider; }
    public String format() { return format; }
    public LocalDateTime date() { return date; }
    public double temperature() { return temperature; }
    public double windSpeed() { return windSpeed; }
    public String windDirection() { return windDirection; }
    public double pressure() { return pressure; }
    public double visibility() { return visibility; }
}
