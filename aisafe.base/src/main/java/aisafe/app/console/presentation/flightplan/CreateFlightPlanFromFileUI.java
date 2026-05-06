package aisafe.app.console.presentation.flightplan;

import aisafe.flightplan.application.CreateFlightPlanFromFileController;
import aisafe.flightplan.domain.FlightPlan;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class CreateFlightPlanFromFileUI extends AbstractUI {

    private final CreateFlightPlanFromFileController controller =
            new CreateFlightPlanFromFileController();

    @Override
    protected boolean doShow() {
        try {
            final String filePath = Console.readLine("DSL file path (e.g., /home/user/flightplan.dsl): ");

            final FlightPlan flightPlan = controller.createFromFile(filePath);

            System.out.println("\n Flight plan successfully created!");
            System.out.println("  Designator : " + flightPlan.designator());
            System.out.println("  Type       : " + flightPlan.flightType());
            System.out.println("  Status     : " + flightPlan.status());

        } catch (final IllegalArgumentException e) {
            System.out.println("\n Validation Error:\n" + e.getMessage());
        } catch (final IllegalStateException e) {
            System.out.println("\n Error: " + e.getMessage());
        } catch (final Exception e) {
            System.out.println("\n An error occurred: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Create Flight Plan from DSL File";
    }
}
EOF
