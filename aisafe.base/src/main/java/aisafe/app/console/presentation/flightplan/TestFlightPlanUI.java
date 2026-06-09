package aisafe.app.console.presentation.flightplan;

import aisafe.flightplan.application.TestFlightPlanController;
import aisafe.flightplan.domain.FlightPlan;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

/**
 * Console UI for the "Test Flight Plan" use case (US085).
 *
 * <p>Lists all VALIDATED DSL-based flight plans and invites the authenticated Pilot to
 * select one for simulation testing. Delegates all business logic to
 * {@link TestFlightPlanController}.</p>
 */
public class TestFlightPlanUI extends AbstractUI {

    private final TestFlightPlanController controller = new TestFlightPlanController();

    @Override
    protected boolean doShow() {
        // Collect available plans
        final List<String> designators = new ArrayList<>();
        for (final FlightPlan fp : controller.validatedDslPlans()) {
            designators.add(fp.designator());
        }

        if (designators.isEmpty()) {
            System.out.println("\n No validated DSL flight plans available for testing.");
            return false;
        }

        System.out.println("\n Available VALIDATED DSL flight plans:");
        for (final String d : designators) {
            System.out.println("   - " + d);
        }

        final String input = Console.readLine("\n Enter designator to test (or leave blank to cancel): ");
        if (input == null || input.isBlank()) {
            System.out.println(" Operation cancelled.");
            return false;
        }

        try {
            System.out.println("\n Testing flight plan " + input.trim().toUpperCase() + "...");
            final FlightPlan tested = controller.testFlightPlan(input.trim());
            System.out.println("\n Flight plan " + tested.designator() + " tested successfully.");
            System.out.println("  Status: " + tested.status());
        } catch (final IllegalArgumentException e) {
            System.out.println("\n Error: " + e.getMessage());
        } catch (final IllegalStateException e) {
            System.out.println("\n " + e.getMessage());
        } catch (final Exception e) {
            System.out.println("\n Error: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Test Flight Plan";
    }
}
