package aisafe.simulation.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Simulation#buildReport(SimulationResults)} and {@link SimulationReport}.
 * The validation result mirrors the simulation: PASS iff COMPLETED and no safety violations.
 */
class SimulationReportTest {

    private static Simulation simulation() {
        return new Simulation(SimulationId.valueOf("SIM-1"));
    }

    private static SimulationResults results(final SimulationStatus status,
                                             final int flights, final int violations) {
        final List<FlightExecutionStatus> statuses = new ArrayList<>();
        for (int i = 1; i <= flights; i++) {
            statuses.add(new FlightExecutionStatus("FLIGHT_" + i,
                    status == SimulationStatus.COMPLETED ? "COMPLETED" : "STOPPED"));
        }
        final List<SafetyViolation> vios = new ArrayList<>();
        for (int i = 0; i < violations; i++) {
            vios.add(new SafetyViolation("proximity", "FLIGHT_" + (i + 1),
                    LocalDateTime.of(2026, 6, 1, 12, 0, i), 41.2, -8.6, 9000, 250, 42.5));
        }
        return new SimulationResults(status, flights, statuses, vios);
    }

    @Test
    void ensureReportCountsTotalFlights() {
        assertEquals(3, simulation().buildReport(results(SimulationStatus.COMPLETED, 3, 0)).totalFlights());
    }

    @Test
    void ensureReportHasOneExecutionStatusPerFlight() {
        assertEquals(3, simulation().buildReport(results(SimulationStatus.COMPLETED, 3, 0))
                .executionStatuses().size());
    }

    @Test
    void ensureReportPassesWhenCompletedAndNoViolations() {
        assertTrue(simulation().buildReport(results(SimulationStatus.COMPLETED, 3, 0)).passed());
    }

    @Test
    void ensureReportFailsWhenViolationsExist() {
        final SimulationReport report = simulation().buildReport(results(SimulationStatus.COMPLETED, 3, 2));
        assertFalse(report.passed());
        assertEquals(2, report.safetyViolations().size());
    }

    @Test
    void ensureReportFailsWhenSimulationAborted() {
        assertFalse(simulation().buildReport(results(SimulationStatus.FAILED, 3, 0)).passed());
    }

    @Test
    void ensureBuildReportSetsSimulationStatusFromResults() {
        final Simulation sim = simulation();
        sim.buildReport(results(SimulationStatus.FAILED, 2, 1));
        assertEquals(SimulationStatus.FAILED, sim.status());
    }

    @Test
    void ensureBuildReportWithNullResultsThrows() {
        assertThrows(IllegalArgumentException.class, () -> simulation().buildReport(null));
    }
}
