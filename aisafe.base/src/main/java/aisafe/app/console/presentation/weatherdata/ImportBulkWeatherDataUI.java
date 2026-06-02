package aisafe.app.console.presentation.weatherdata;

import aisafe.weatherdata.application.ImportBulkWeatherDataController;
import aisafe.weatherdata.application.ImportResult;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

/**
 * Console UI for the "Import Bulk Weather Data" use case (US042).
 *
 * <p>Prompts the Weather Person for a file path, delegates to
 * {@link ImportBulkWeatherDataController}, and prints the import summary.</p>
 */
public class ImportBulkWeatherDataUI extends AbstractUI {

    private final ImportBulkWeatherDataController controller =
            new ImportBulkWeatherDataController();

    @Override
    protected boolean doShow() {
        final String filePath = Console.readLine("File path (CSV): ").trim();

        try {
            final ImportResult result = controller.importWeatherData(filePath);

            System.out.printf("%nImport complete: %d record(s) saved.%n", result.saved());

            if (!result.failures().isEmpty()) {
                System.out.printf("%d record(s) rejected:%n", result.failures().size());
                for (final String reason : result.failures()) {
                    System.out.println("  - " + reason);
                }
            }

        } catch (final Exception e) {
            System.out.println("\nImport failed: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Import Bulk Weather Data";
    }
}
