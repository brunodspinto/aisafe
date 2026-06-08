package aisafe.reporting.application;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Common data structure shared by all generated reports.
 */
public final class ReportData {

    private final String reportType;
    private final String areaCode;
    private final String areaName;
    private final YearMonth reportingMonth;
    private final String generatedBy;
    private final List<ReportSection> sections;

    public ReportData(final String reportType,
                      final String areaCode,
                      final String areaName,
                      final YearMonth reportingMonth,
                      final String generatedBy,
                      final List<ReportSection> sections) {
        if (reportType == null || reportType.isBlank()) {
            throw new IllegalArgumentException("Report type cannot be null or blank.");
        }
        if (areaCode == null || areaCode.isBlank()) {
            throw new IllegalArgumentException("Area code cannot be null or blank.");
        }
        if (areaName == null || areaName.isBlank()) {
            throw new IllegalArgumentException("Area name cannot be null or blank.");
        }
        if (reportingMonth == null) {
            throw new IllegalArgumentException("Reporting month cannot be null.");
        }
        if (generatedBy == null || generatedBy.isBlank()) {
            throw new IllegalArgumentException("Generated-by user cannot be null or blank.");
        }
        if (sections == null) {
            throw new IllegalArgumentException("Sections cannot be null.");
        }
        this.reportType = reportType;
        this.areaCode = areaCode;
        this.areaName = areaName;
        this.reportingMonth = reportingMonth;
        this.generatedBy = generatedBy;
        this.sections = Collections.unmodifiableList(new ArrayList<>(sections));
    }

    public String reportType() {
        return reportType;
    }

    public String areaCode() {
        return areaCode;
    }

    public String areaName() {
        return areaName;
    }

    public YearMonth reportingMonth() {
        return reportingMonth;
    }

    public String generatedBy() {
        return generatedBy;
    }

    public List<ReportSection> sections() {
        return sections;
    }
}
