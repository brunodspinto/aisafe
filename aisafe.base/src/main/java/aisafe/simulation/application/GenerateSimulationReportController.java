package aisafe.simulation.application;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.aircontrolarea.domain.AirControlAreaCode;
import aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import aisafe.collaborator.repositories.CollaboratorRepository;
import aisafe.infrastructure.application.AppSettings;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.infrastructure.simulation.TextSimulationResultsReader;
import aisafe.reporting.application.OperationalReportFormatter;
import aisafe.reporting.application.ReportData;
import aisafe.reporting.application.ReportSection;
import aisafe.reporting.application.ReportWriter;
import aisafe.simulation.domain.FlightExecutionStatus;
import aisafe.simulation.domain.SafetyViolation;
import aisafe.simulation.domain.Simulation;
import aisafe.simulation.domain.SimulationId;
import aisafe.simulation.domain.SimulationReport;
import aisafe.simulation.domain.SimulationResults;
import aisafe.usermanagement.domain.AiSafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;

import java.io.IOException;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Application-layer controller for the "Generate a Simulation Report" use case (US111).
 * Requires an authenticated Flight Control Operator (FCO).
 *
 * <p>It reads the simulation results from a configured file (the {@code simulation_report.txt}
 * produced by the SCOMP/C simulation), asks the {@link Simulation} aggregate to build the
 * {@link SimulationReport}, and writes the FCO report file by reusing the US112 reporting framework
 * ({@link ReportData} / {@link OperationalReportFormatter} / {@link ReportWriter}).
 */
@UseCaseController
public class GenerateSimulationReportController {

    private final AuthorizationService authz;
    private final CollaboratorRepository collaboratorRepository;
    private final AirControlAreaRepository airControlAreaRepository;
    private final SimulationResultsReader resultsReader;
    private final OperationalReportFormatter reportFormatter;
    private final ReportWriter reportWriter;
    private final Path reportSource;

    /** Composition root — wires the concrete adapters and the configured input path. */
    public GenerateSimulationReportController() {
        this(
                AuthzRegistry.authorizationService(),
                PersistenceContext.repositories().collaborators(),
                PersistenceContext.repositories().airControlAreas(),
                new TextSimulationResultsReader(),
                new OperationalReportFormatter(),
                new ReportWriter(),
                Path.of(new AppSettings().simulationReportFile()));
    }

    GenerateSimulationReportController(final AuthorizationService authz,
                                       final CollaboratorRepository collaboratorRepository,
                                       final AirControlAreaRepository airControlAreaRepository,
                                       final SimulationResultsReader resultsReader,
                                       final OperationalReportFormatter reportFormatter,
                                       final ReportWriter reportWriter,
                                       final Path reportSource) {
        if (authz == null || collaboratorRepository == null || airControlAreaRepository == null
                || resultsReader == null || reportFormatter == null || reportWriter == null
                || reportSource == null) {
            throw new IllegalArgumentException("Controller dependencies cannot be null.");
        }
        this.authz = authz;
        this.collaboratorRepository = collaboratorRepository;
        this.airControlAreaRepository = airControlAreaRepository;
        this.resultsReader = resultsReader;
        this.reportFormatter = reportFormatter;
        this.reportWriter = reportWriter;
        this.reportSource = reportSource;
    }

    /**
     * Reads the simulation results, builds the report and writes it to a file.
     *
     * @return the generated report together with the path of the written file
     * @throws IOException if the simulation results file cannot be read
     */
    public GeneratedReport generate() throws IOException {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.FLIGHT_CONTROL_OPERATOR);

        final SystemUser fco = authz.session()
                .orElseThrow(() -> new IllegalStateException("No active session."))
                .authenticatedUser();
        final AirControlArea area = resolveOperatorArea(fco);

        final SimulationResults results = resultsReader.read(reportSource);
        final SimulationReport report =
                new Simulation(SimulationId.valueOf("SIM-" + System.currentTimeMillis()))
                        .buildReport(results);

        final ReportData data = toReportData(report, area, fco);
        final String content = reportFormatter.format(data);
        final String fileName = String.format("simulation_report_%s_%d.txt",
                area.areaCode(), System.currentTimeMillis());
        final Path file = reportWriter.write(fileName, content);

        return new GeneratedReport(file, report);
    }

    private ReportData toReportData(final SimulationReport report, final AirControlArea area,
                                    final SystemUser fco) {
        final List<ReportSection> sections = new ArrayList<>();

        sections.add(new ReportSection("Executive Summary", List.of(
                "Total flights      : " + report.totalFlights(),
                "Safety violations  : " + report.safetyViolations().size(),
                "Validation result  : " + (report.passed() ? "PASSED" : "FAILED"))));

        final List<String> exec = new ArrayList<>();
        if (report.executionStatuses().isEmpty()) {
            exec.add("No flights recorded.");
        } else {
            for (final FlightExecutionStatus s : report.executionStatuses()) {
                exec.add(s.flightDesignator() + " : " + s.status());
            }
        }
        sections.add(new ReportSection("Flight Execution Statuses", exec));

        final List<String> violations = new ArrayList<>();
        if (report.safetyViolations().isEmpty()) {
            violations.add("No safety violations recorded.");
        } else {
            for (final SafetyViolation v : report.safetyViolations()) {
                violations.add(String.format("%s | %s | lat=%.4f lon=%.4f alt=%.0fm | %s",
                        v.timestamp(), v.flightDesignator(),
                        v.latitude(), v.longitude(), v.altitude(), v.description()));
            }
        }
        sections.add(new ReportSection("Safety Violations", violations));

        return new ReportData("Simulation Report",
                area.areaCode().toString(), area.name(),
                YearMonth.now(), fco.identity().toString(), sections);
    }

    private AirControlArea resolveOperatorArea(final SystemUser fco) {
        return collaboratorRepository.findBySystemUser(fco)
                .map(c -> {
                    if (!c.isAreaCollaborator()) {
                        throw new IllegalStateException(
                                "Authenticated user is not assigned to an Air Control Area.");
                    }
                    return airControlAreaRepository.ofIdentity(
                                    AirControlAreaCode.valueOf(c.areaCode().toString()))
                            .orElseThrow(() -> new IllegalStateException(
                                    "Air Control Area not found for code: " + c.areaCode()));
                })
                .orElseThrow(() -> new IllegalStateException(
                        "No collaborator found for the authenticated user."));
    }

    /** The generated report and the path of the file it was written to. */
    public record GeneratedReport(Path file, SimulationReport report) {
    }
}
