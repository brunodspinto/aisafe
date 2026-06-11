package aisafe.app.console.presentation.simulation;

import aisafe.simulation.application.GenerateSimulationReportController;
import aisafe.simulation.application.GenerateSimulationReportController.GeneratedReport;
import eapli.framework.presentation.console.AbstractUI;

import java.io.IOException;

/**
 * Console UI for the "Generate a Simulation Report" use case (US111).
 * Lets a Flight Control Operator generate the FCO report from the latest simulation results.
 */
public class GenerateSimulationReportUI extends AbstractUI {

    private final GenerateSimulationReportController controller = new GenerateSimulationReportController();

    @Override
    protected boolean doShow() {
        try {
            final GeneratedReport result = controller.generate();

            System.out.println("\nSimulation report generated.");
            System.out.println("File    : " + result.file().toAbsolutePath());
            System.out.println("Result  : " + (result.report().passed() ? "PASSED" : "FAILED"));
            System.out.println("Flights : " + result.report().totalFlights()
                    + "  |  Safety violations: " + result.report().safetyViolations().size());
        } catch (final IOException e) {
            System.out.println("\nCould not read the simulation results file: " + e.getMessage());
            System.out.println("Run the flight simulation first, or check 'simulation.report.file'.");
        } catch (final Exception e) {
            System.out.println("\nUnable to generate simulation report: " + e.getMessage());
        }
        return false;
    }

    @Override
    public String headline() {
        return "Generate Simulation Report";
    }
}
