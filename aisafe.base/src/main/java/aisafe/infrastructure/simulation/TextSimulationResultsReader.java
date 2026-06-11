package aisafe.infrastructure.simulation;

import aisafe.simulation.application.SimulationResultsReader;
import aisafe.simulation.domain.FlightExecutionStatus;
import aisafe.simulation.domain.SafetyViolation;
import aisafe.simulation.domain.SimulationResults;
import aisafe.simulation.domain.SimulationStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adapter that parses the SCOMP/C simulation output ({@code simulation_report.txt}, US109) into a
 * {@link SimulationResults}. It is the only class that knows the text format, so a format change
 * affects nothing else (Adapter / Protected Variations).
 *
 * <p>Each C violation event involves a pair of aircraft and carries one position per aircraft;
 * this parser emits <b>one {@link SafetyViolation} per aircraft</b> (two per event), each with its
 * own position and the shared timestamp, matching the single-aircraft Domain Model V10 value object.
 */
public final class TextSimulationResultsReader implements SimulationResultsReader {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Pattern EVENT = Pattern.compile(
            "^\\[\\d+\\]\\s+(\\S+)\\s+<->\\s+(\\S+)\\s+\\|\\s+timestamp="
                    + "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\s+\\|\\s+horizontal=([\\d.]+)m"
                    + "\\s+\\|\\s+vertical=([\\d.]+)m$");

    private static final Pattern POSITION = Pattern.compile(
            "^([^:]+):\\s+lat=(-?[\\d.]+)\\s+\\|\\s+lon=(-?[\\d.]+)\\s+\\|\\s+alt=(-?[\\d.]+)m"
                    + "\\s+\\|\\s+speed=(-?[\\d.]+)kt\\s+\\|\\s+heading=(-?[\\d.]+)"
                    + "\\s+\\|\\s+vertical_rate=(-?[\\d.]+)m/s$");

    private enum Section { HEADER, VIOLATIONS, EXEC }

    @Override
    public SimulationResults read(final Path source) throws IOException {
        return parse(Files.readAllLines(source, StandardCharsets.UTF_8));
    }

    /**
     * Parses the already-read lines of a simulation report into a {@link SimulationResults}.
     * Exposed (instead of only {@link #read}) so it can be unit-tested without a real file.
     *
     * @param lines the lines of the {@code simulation_report.txt}
     * @return the parsed results
     */
    public SimulationResults parse(final List<String> lines) {
        SimulationStatus status = SimulationStatus.COMPLETED;
        int totalFlights = 0;
        final List<SafetyViolation> violations = new ArrayList<>();
        final List<FlightExecutionStatus> statuses = new ArrayList<>();

        Section section = Section.HEADER;
        String eventFlightA = null;
        String eventFlightB = null;
        LocalDateTime eventTimestamp = null;
        double eventHorizontal = 0;
        double eventVertical = 0;
        String currentDesignator = null;

        for (final String raw : lines) {
            final String line = raw.strip();
            if (line.isEmpty()) {
                continue;
            }
            if (line.equals("SAFETY VIOLATION EVENTS")) {
                section = Section.VIOLATIONS;
                continue;
            }
            if (line.equals("FLIGHT EXECUTION STATUSES")) {
                section = Section.EXEC;
                continue;
            }
            if (line.startsWith("=================== END OF REPORT")) {
                break;
            }

            switch (section) {
                case HEADER -> {
                    if (line.startsWith("Simulation End Status:")) {
                        status = line.contains("ABORTED")
                                ? SimulationStatus.FAILED : SimulationStatus.COMPLETED;
                    } else if (line.startsWith("Total Flights:")) {
                        totalFlights = parseIntAfterColon(line);
                    }
                }
                case VIOLATIONS -> {
                    final Matcher event = EVENT.matcher(line);
                    final Matcher position = POSITION.matcher(line);
                    if (event.matches()) {
                        eventFlightA = event.group(1);
                        eventFlightB = event.group(2);
                        eventTimestamp = LocalDateTime.parse(event.group(3), TIMESTAMP);
                        eventHorizontal = Double.parseDouble(event.group(4));
                        eventVertical = Double.parseDouble(event.group(5));
                    } else if (position.matches() && eventTimestamp != null) {
                        final String flight = position.group(1).strip();
                        final String other = flight.equals(eventFlightA) ? eventFlightB : eventFlightA;
                        final String description = String.format(
                                "proximity with %s (H=%.2fm, V=%.2fm)", other, eventHorizontal, eventVertical);
                        violations.add(new SafetyViolation(
                                description, flight, eventTimestamp,
                                Double.parseDouble(position.group(2)),
                                Double.parseDouble(position.group(3)),
                                Double.parseDouble(position.group(4)),
                                Double.parseDouble(position.group(5)),
                                Double.parseDouble(position.group(6))));
                    }
                }
                case EXEC -> {
                    if (line.startsWith("Aircraft Identifier:")) {
                        currentDesignator = afterColon(line);
                    } else if (line.startsWith("Execution Status:") && currentDesignator != null) {
                        statuses.add(new FlightExecutionStatus(currentDesignator, afterColon(line)));
                        currentDesignator = null;
                    }
                }
            }
        }
        return new SimulationResults(status, totalFlights, statuses, violations);
    }

    private static String afterColon(final String line) {
        return line.substring(line.indexOf(':') + 1).strip();
    }

    private static int parseIntAfterColon(final String line) {
        try {
            return Integer.parseInt(afterColon(line));
        } catch (final NumberFormatException e) {
            return 0;
        }
    }
}
