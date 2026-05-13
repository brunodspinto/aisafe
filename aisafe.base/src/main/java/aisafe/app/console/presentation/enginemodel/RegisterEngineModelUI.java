package aisafe.app.console.presentation.enginemodel;

import aisafe.enginemodel.application.RegisterEngineModelController;
import aisafe.enginemodel.domain.EngineType;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

/**
 * Console UI for the "Register Engine Model" use case (US056).
 * Collects engine model details and delegates to {@link RegisterEngineModelController}.
 */
public class RegisterEngineModelUI extends AbstractUI {

    private final RegisterEngineModelController controller = new RegisterEngineModelController();

    @Override
    protected boolean doShow() {
        try {
            final String name = Console.readLine("Model Name: ");
            final String makerName = Console.readLine("Maker Name: ");

            System.out.println("\n--- Engine Types ---");
            final EngineType[] types = controller.engineTypes();
            for (int i = 0; i < types.length; i++) {
                System.out.printf("  [%d] %s%n", i + 1, types[i]);
            }
            EngineType engineType = null;
            while (engineType == null) {
                final int typeIndex = Console.readInteger("Select Engine Type (number): ");
                if (typeIndex < 1 || typeIndex > types.length) {
                    System.out.println("Invalid selection. Choose a number between 1 and " + types.length + ".");
                } else {
                    engineType = types[typeIndex - 1];
                }
            }

            final double thrust = Console.readDouble("Thrust (kN): ");
            final double tsfc = Console.readDouble("TSFC (kg/(kN·h)): ");

            controller.registerEngineModel(name, makerName, engineType, thrust, tsfc);

            System.out.println("\nEngine model successfully registered!");
        } catch (final IllegalArgumentException e) {
            System.out.println("\nValidation Error: " + e.getMessage());
        } catch (final Exception e) {
            System.out.println("\nAn error occurred while registering the engine model: " + e.getMessage());
        }

        return false;
    }

    @Override
    public String headline() {
        return "Register Engine Model";
    }
}
