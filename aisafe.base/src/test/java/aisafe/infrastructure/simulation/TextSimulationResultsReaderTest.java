package aisafe.infrastructure.simulation;

import aisafe.simulation.domain.SafetyViolation;
import aisafe.simulation.domain.Simulation;
import aisafe.simulation.domain.SimulationId;
import aisafe.simulation.domain.SimulationReport;
import aisafe.simulation.domain.SimulationResults;
import aisafe.simulation.domain.SimulationStatus;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link TextSimulationResultsReader} — parsing the SCOMP/C {@code simulation_report.txt}
 * (Option A). Uses sample report files under {@code src/test/resources/simulation/}, so it does not
 * depend on actually running the simulation.
 */
class TextSimulationResultsReaderTest {

    private final TextSimulationResultsReader reader = new TextSimulationResultsReader();

    private List<String> resource(final String name) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(name)) {
            assertNotNull(in, "Missing test resource: " + name);
            return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines().toList();
        }
    }

    @Test
    void ensureAbortedReportIsParsed() throws IOException {
        final SimulationResults r = reader.parse(resource("/simulation/sample_simulation_report.txt"));
        assertEquals(SimulationStatus.FAILED, r.status());
        assertEquals(3, r.totalFlights());
        assertEquals(3, r.executionStatuses().size());
        // one C event (a pair) -> two domain SafetyViolations
        assertEquals(2, r.safetyViolations().size());
    }

    @Test
    void ensureViolationCarriesTimestampAndPosition() throws IOException {
        final SimulationResults r = reader.parse(resource("/simulation/sample_simulation_report.txt"));
        final SafetyViolation first = r.safetyViolations().get(0);
        assertEquals("FLIGHT_01", first.flightDesignator());
        assertEquals(LocalDateTime.of(2026, 6, 1, 12, 3, 10), first.timestamp());
        assertEquals(41.2000, first.latitude());
        assertEquals(-8.6000, first.longitude());
        assertEquals(9000.0, first.altitude());
        assertEquals("FLIGHT_02", r.safetyViolations().get(1).flightDesignator());
    }

    @Test
    void ensureCleanReportIsParsed() throws IOException {
        final SimulationResults r = reader.parse(resource("/simulation/sample_simulation_report_clean.txt"));
        assertEquals(SimulationStatus.COMPLETED, r.status());
        assertEquals(2, r.totalFlights());
        assertTrue(r.safetyViolations().isEmpty());
        assertEquals(2, r.executionStatuses().size());
        assertEquals("COMPLETED", r.executionStatuses().get(0).status());
    }

    @Test
    void ensureParsedResultsBuildACoherentReport() throws IOException {
        // integration of M1 (domain) + M2 (parser)
        final SimulationResults clean = reader.parse(resource("/simulation/sample_simulation_report_clean.txt"));
        final SimulationReport pass = new Simulation(SimulationId.valueOf("SIM-1")).buildReport(clean);
        assertTrue(pass.passed());
        assertEquals(2, pass.totalFlights());

        final SimulationResults aborted = reader.parse(resource("/simulation/sample_simulation_report.txt"));
        final SimulationReport fail = new Simulation(SimulationId.valueOf("SIM-2")).buildReport(aborted);
        assertFalse(fail.passed());
        assertEquals(2, fail.safetyViolations().size());
    }
}
