package aisafe.reporting.application;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.aircontrolarea.domain.AirControlAreaCode;
import aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import aisafe.collaborator.repositories.CollaboratorRepository;
import aisafe.flightplan.repositories.FlightPlanRepository;
import aisafe.flightroute.repositories.FlightRouteRepository;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.airport.repositories.AirportRepository;
import aisafe.usermanagement.domain.AiSafeRoles;
import aisafe.weatherdata.repositories.WeatherDataRepository;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;

import java.nio.file.Path;
import java.time.YearMonth;

/**
 * Application controller for US112.
 */
@UseCaseController
public class GenerateMonthlyReportController {

    private final AuthorizationService authz;
    private final CollaboratorRepository collaboratorRepository;
    private final AirControlAreaRepository airControlAreaRepository;
    private final ReportGenerationStrategy reportStrategy;
    private final OperationalReportFormatter reportFormatter;
    private final ReportWriter reportWriter;

    public GenerateMonthlyReportController() {
        this(
                AuthzRegistry.authorizationService(),
                PersistenceContext.repositories().collaborators(),
                PersistenceContext.repositories().airControlAreas(),
                buildStrategy(
                        PersistenceContext.repositories().flightPlans(),
                        PersistenceContext.repositories().flightRoutes(),
                        PersistenceContext.repositories().airports(),
                        PersistenceContext.repositories().weatherData()
                ),
                new OperationalReportFormatter(),
                new ReportWriter()
        );
    }

    GenerateMonthlyReportController(final AuthorizationService authz,
                                    final CollaboratorRepository collaboratorRepository,
                                    final AirControlAreaRepository airControlAreaRepository,
                                    final ReportGenerationStrategy reportStrategy,
                                    final OperationalReportFormatter reportFormatter,
                                    final ReportWriter reportWriter) {
        if (authz == null) {
            throw new IllegalArgumentException("Authorization service cannot be null.");
        }
        if (collaboratorRepository == null) {
            throw new IllegalArgumentException("Collaborator repository cannot be null.");
        }
        if (airControlAreaRepository == null) {
            throw new IllegalArgumentException("Air Control Area repository cannot be null.");
        }
        if (reportStrategy == null) {
            throw new IllegalArgumentException("Report strategy cannot be null.");
        }
        if (reportFormatter == null) {
            throw new IllegalArgumentException("Report formatter cannot be null.");
        }
        if (reportWriter == null) {
            throw new IllegalArgumentException("Report writer cannot be null.");
        }
        this.authz = authz;
        this.collaboratorRepository = collaboratorRepository;
        this.airControlAreaRepository = airControlAreaRepository;
        this.reportStrategy = reportStrategy;
        this.reportFormatter = reportFormatter;
        this.reportWriter = reportWriter;
    }

    public Path generateMonthlyReport(final int year, final int month) {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.FLIGHT_CONTROL_OPERATOR);

        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12.");
        }

        final YearMonth reportingMonth = YearMonth.of(year, month);
        final SystemUser currentUser = authz.session()
                .orElseThrow(() -> new IllegalStateException("No active session."))
                .authenticatedUser();
        final AirControlArea area = resolveOperatorArea(currentUser);

        final ReportData reportData = reportStrategy.generate(reportingMonth, area, currentUser);
        final String content = reportFormatter.format(reportData);
        final String fileName = String.format("monthly_report_%s_%04d_%02d.txt",
                area.areaCode(), year, month);
        return reportWriter.write(fileName, content);
    }

    private AirControlArea resolveOperatorArea(final SystemUser currentUser) {
        return collaboratorRepository.findBySystemUser(currentUser)
                .map(c -> {
                    if (!c.isAreaCollaborator()) {
                        throw new IllegalStateException(
                                "Authenticated user is not assigned to an Air Control Area.");
                    }
                    return airControlAreaRepository.ofIdentity(AirControlAreaCode.valueOf(c.areaCode().toString()))
                            .orElseThrow(() -> new IllegalStateException(
                                    "Air Control Area not found for code: " + c.areaCode()));
                })
                .orElseThrow(() -> new IllegalStateException("No collaborator found for the authenticated user."));
    }

    private static ReportGenerationStrategy buildStrategy(final FlightPlanRepository flightPlanRepository,
                                                          final FlightRouteRepository flightRouteRepository,
                                                          final AirportRepository airportRepository,
                                                          final WeatherDataRepository weatherDataRepository) {
        return new MonthlyStatisticsReportStrategy(
                flightPlanRepository,
                flightRouteRepository,
                airportRepository,
                weatherDataRepository
        );
    }
}
