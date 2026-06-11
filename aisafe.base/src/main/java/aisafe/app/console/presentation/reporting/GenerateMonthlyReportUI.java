package aisafe.app.console.presentation.reporting;

import aisafe.reporting.application.GenerateMonthlyReportController;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.nio.file.Path;
import java.time.YearMonth;

/**
 * Console UI for US112.
 */
public class GenerateMonthlyReportUI extends AbstractUI {

    private final GenerateMonthlyReportController controller = new GenerateMonthlyReportController();

    @Override
    protected boolean doShow() {
        try {
            final int year = Console.readInteger("Year (e.g. 2026): ");
            final int month = Console.readInteger("Month (1-12): ");

            final YearMonth ignored = YearMonth.of(year, month);
            final Path generatedFile = controller.generateMonthlyReport(year, month);

            System.out.println("\nMonthly report generated successfully.");
            System.out.println("File: " + generatedFile.toAbsolutePath());
        } catch (final IllegalArgumentException e) {
            System.out.println("\nValidation Error: " + e.getMessage());
        } catch (final Exception e) {
            System.out.println("\nUnable to generate monthly report: " + e.getMessage());
        }
        return false;
    }

    @Override
    public String headline() {
        return "Generate Monthly Report";
    }
}
