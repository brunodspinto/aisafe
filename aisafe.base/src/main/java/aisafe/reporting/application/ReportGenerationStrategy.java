package aisafe.reporting.application;

import aisafe.aircontrolarea.domain.AirControlArea;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;

import java.time.YearMonth;

/**
 * Strategy contract for report-specific data collection.
 */
public interface ReportGenerationStrategy {

    ReportData generate(YearMonth reportingMonth, AirControlArea area, SystemUser generatedBy);
}
