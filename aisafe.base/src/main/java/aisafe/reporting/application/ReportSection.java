package aisafe.reporting.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple immutable section of a generated report.
 */
public final class ReportSection {

    private final String title;
    private final List<String> lines;

    public ReportSection(final String title, final List<String> lines) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Section title cannot be null or blank.");
        }
        if (lines == null) {
            throw new IllegalArgumentException("Section lines cannot be null.");
        }
        this.title = title;
        this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
    }

    public String title() {
        return title;
    }

    public List<String> lines() {
        return lines;
    }
}
