package aisafe.reporting.application;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.airport.domain.Airport;
import aisafe.airport.repositories.AirportRepository;
import aisafe.flightplan.domain.FlightPlan;
import aisafe.flightplan.domain.FlightPlanStatus;
import aisafe.flightplan.repositories.FlightPlanRepository;
import aisafe.flightroute.domain.FlightRoute;
import aisafe.flightroute.repositories.FlightRouteRepository;
import aisafe.weatherdata.domain.WeatherData;
import aisafe.weatherdata.repositories.WeatherDataRepository;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Report strategy for monthly operational statistics.
 */
public final class MonthlyStatisticsReportStrategy implements ReportGenerationStrategy {

    private final FlightPlanRepository flightPlanRepository;
    private final FlightRouteRepository flightRouteRepository;
    private final AirportRepository airportRepository;
    private final WeatherDataRepository weatherDataRepository;

    public MonthlyStatisticsReportStrategy(final FlightPlanRepository flightPlanRepository,
                                           final FlightRouteRepository flightRouteRepository,
                                           final AirportRepository airportRepository,
                                           final WeatherDataRepository weatherDataRepository) {
        if (flightPlanRepository == null) {
            throw new IllegalArgumentException("Flight plan repository cannot be null.");
        }
        if (flightRouteRepository == null) {
            throw new IllegalArgumentException("Flight route repository cannot be null.");
        }
        if (airportRepository == null) {
            throw new IllegalArgumentException("Airport repository cannot be null.");
        }
        if (weatherDataRepository == null) {
            throw new IllegalArgumentException("Weather data repository cannot be null.");
        }
        this.flightPlanRepository = flightPlanRepository;
        this.flightRouteRepository = flightRouteRepository;
        this.airportRepository = airportRepository;
        this.weatherDataRepository = weatherDataRepository;
    }

    @Override
    public ReportData generate(final YearMonth reportingMonth,
                               final AirControlArea area,
                               final SystemUser generatedBy) {
        if (reportingMonth == null) {
            throw new IllegalArgumentException("Reporting month cannot be null.");
        }
        if (area == null) {
            throw new IllegalArgumentException("Air Control Area cannot be null.");
        }
        if (generatedBy == null) {
            throw new IllegalArgumentException("Generated-by user cannot be null.");
        }

        final FlightPlanStats flightPlanStats = collectFlightPlanStats(reportingMonth, area);
        final WeatherStats weatherStats = collectWeatherStats(reportingMonth, area);

        final List<ReportSection> sections = new ArrayList<>();
        sections.add(buildExecutiveSummarySection(flightPlanStats, weatherStats));
        sections.add(buildFlightPlanSection(flightPlanStats));
        sections.add(buildWeatherSection(weatherStats));
        sections.add(buildGraphicsSection(flightPlanStats));

        return new ReportData(
                "Monthly Statistics Report",
                area.areaCode().toString(),
                area.name(),
                reportingMonth,
                generatedBy.identity().toString(),
                sections
        );
    }

    private FlightPlanStats collectFlightPlanStats(final YearMonth reportingMonth,
                                                   final AirControlArea area) {
        final Map<FlightPlanStatus, Integer> statusCounts = new EnumMap<>(FlightPlanStatus.class);
        final Map<String, Integer> flightTypeCounts = new LinkedHashMap<>();
        int totalPlans = 0;
        int departuresInArea = 0;
        int arrivalsInArea = 0;

        for (final FlightPlan plan : flightPlanRepository.findAll()) {
            if (plan.departureDateTime() == null || plan.routeName() == null) {
                continue;
            }
            if (!YearMonth.from(plan.departureDateTime()).equals(reportingMonth)) {
                continue;
            }

            final FlightRoute route = flightRouteRepository.ofIdentity(plan.routeName()).orElse(null);
            if (route == null) {
                continue;
            }

            final Airport origin = airportRepository.ofIdentity(route.originAirport()).orElse(null);
            final Airport destination = airportRepository.ofIdentity(route.destinationAirport()).orElse(null);
            if (origin == null || destination == null) {
                continue;
            }

            final boolean originInArea = origin.airControlArea() != null
                    && area.identity().equals(origin.airControlArea().identity());
            final boolean destinationInArea = destination.airControlArea() != null
                    && area.identity().equals(destination.airControlArea().identity());

            if (!originInArea && !destinationInArea) {
                continue;
            }

            totalPlans++;
            if (originInArea) {
                departuresInArea++;
            }
            if (destinationInArea) {
                arrivalsInArea++;
            }
            statusCounts.merge(plan.status(), 1, Integer::sum);
            flightTypeCounts.merge(plan.flightType().name(), 1, Integer::sum);
        }

        return new FlightPlanStats(totalPlans, departuresInArea, arrivalsInArea, statusCounts, flightTypeCounts);
    }

    private WeatherStats collectWeatherStats(final YearMonth reportingMonth,
                                             final AirControlArea area) {
        int totalRecords = 0;
        double totalTemperature = 0.0;
        double totalWindSpeed = 0.0;
        double minVisibility = Double.MAX_VALUE;

        for (final WeatherData weatherData : weatherDataRepository.findAll()) {
            if (!area.areaCode().toString().equalsIgnoreCase(weatherData.areaCode())) {
                continue;
            }
            if (!YearMonth.from(weatherData.date()).equals(reportingMonth)) {
                continue;
            }

            totalRecords++;
            totalTemperature += weatherData.temperature();
            totalWindSpeed += weatherData.windSpeed();
            if (weatherData.visibility() < minVisibility) {
                minVisibility = weatherData.visibility();
            }
        }

        final double averageTemperature = totalRecords == 0 ? 0.0 : totalTemperature / totalRecords;
        final double averageWindSpeed = totalRecords == 0 ? 0.0 : totalWindSpeed / totalRecords;
        final double lowestVisibility = totalRecords == 0 ? 0.0 : minVisibility;

        return new WeatherStats(totalRecords, averageTemperature, averageWindSpeed, lowestVisibility);
    }

    private ReportSection buildExecutiveSummarySection(final FlightPlanStats flightPlanStats,
                                                       final WeatherStats weatherStats) {
        final List<String> lines = new ArrayList<>();
        lines.add("Total flight plans in month : " + flightPlanStats.totalPlans);
        lines.add("Departures from area        : " + flightPlanStats.departuresInArea);
        lines.add("Arrivals to area           : " + flightPlanStats.arrivalsInArea);
        lines.add("Weather records in month   : " + weatherStats.totalRecords);
        return new ReportSection("Executive Summary", lines);
    }

    private ReportSection buildFlightPlanSection(final FlightPlanStats stats) {
        final List<String> lines = new ArrayList<>();
        lines.add("Flight plans related to this Air Control Area are counted when their route");
        lines.add("departs from or arrives at an airport inside the operator's area.");
        lines.add("");
        lines.add("By status:");
        for (final FlightPlanStatus status : FlightPlanStatus.values()) {
            lines.add(String.format("  %-10s : %d", status.name(), stats.statusCounts.getOrDefault(status, 0)));
        }
        lines.add("");
        lines.add("By flight type:");
        if (stats.flightTypeCounts.isEmpty()) {
            lines.add("  No flight plans found for the selected month.");
        } else {
            for (final Map.Entry<String, Integer> entry : stats.flightTypeCounts.entrySet()) {
                lines.add(String.format("  %-10s : %d", entry.getKey(), entry.getValue()));
            }
        }
        return new ReportSection("Flight Plan Statistics", lines);
    }

    private ReportSection buildWeatherSection(final WeatherStats stats) {
        final List<String> lines = new ArrayList<>();
        lines.add("Total records        : " + stats.totalRecords);
        lines.add(String.format("Average temperature : %.1f C", stats.averageTemperature));
        lines.add(String.format("Average wind speed  : %.1f", stats.averageWindSpeed));
        lines.add(String.format("Lowest visibility   : %.1f", stats.lowestVisibility));
        return new ReportSection("Weather Summary", lines);
    }

    private ReportSection buildGraphicsSection(final FlightPlanStats stats) {
        final List<String> lines = new ArrayList<>();
        final int max = maxStatusCount(stats.statusCounts);
        lines.add("Flight plans by status:");
        for (final FlightPlanStatus status : FlightPlanStatus.values()) {
            final int count = stats.statusCounts.getOrDefault(status, 0);
            lines.add(String.format("  %-10s | %s (%d)", status.name(), bar(count, max), count));
        }
        return new ReportSection("Simple Graphics", lines);
    }

    private int maxStatusCount(final Map<FlightPlanStatus, Integer> statusCounts) {
        int max = 0;
        for (final Integer count : statusCounts.values()) {
            if (count != null && count > max) {
                max = count;
            }
        }
        return max;
    }

    private String bar(final int count, final int max) {
        if (count <= 0 || max <= 0) {
            return "";
        }
        final int barSize = Math.max(1, (int) Math.round((count * 20.0) / max));
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < barSize; i++) {
            sb.append('#');
        }
        return sb.toString();
    }

    private static final class FlightPlanStats {
        private final int totalPlans;
        private final int departuresInArea;
        private final int arrivalsInArea;
        private final Map<FlightPlanStatus, Integer> statusCounts;
        private final Map<String, Integer> flightTypeCounts;

        private FlightPlanStats(final int totalPlans,
                                final int departuresInArea,
                                final int arrivalsInArea,
                                final Map<FlightPlanStatus, Integer> statusCounts,
                                final Map<String, Integer> flightTypeCounts) {
            this.totalPlans = totalPlans;
            this.departuresInArea = departuresInArea;
            this.arrivalsInArea = arrivalsInArea;
            this.statusCounts = statusCounts;
            this.flightTypeCounts = flightTypeCounts;
        }
    }

    private static final class WeatherStats {
        private final int totalRecords;
        private final double averageTemperature;
        private final double averageWindSpeed;
        private final double lowestVisibility;

        private WeatherStats(final int totalRecords,
                             final double averageTemperature,
                             final double averageWindSpeed,
                             final double lowestVisibility) {
            this.totalRecords = totalRecords;
            this.averageTemperature = averageTemperature;
            this.averageWindSpeed = averageWindSpeed;
            this.lowestVisibility = lowestVisibility;
        }
    }
}
