package aisafe.app.console.presentation.aircraftmodel;

import aisafe.aircraftmodel.application.RemoveEngineFromAircraftModelController;
import aisafe.aircraftmodel.domain.AircraftModel;
import aisafe.enginemodel.domain.EngineModel;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

/**
 * Console UI for the "Remove Engine Model from Aircraft Model" use case (US056).
 * Lets the user pick an aircraft model and de-certify one of its engines.
 */
public class RemoveEngineFromAircraftModelUI extends AbstractUI {

    private final RemoveEngineFromAircraftModelController controller =
            new RemoveEngineFromAircraftModelController();

    @Override
    protected boolean doShow() {
        try {
            System.out.println("\n--- Available Aircraft Models ---");
            final List<AircraftModel> models = new ArrayList<>();
            for (final AircraftModel model : controller.allAircraftModels()) {
                System.out.printf("  [%d] %s (%s) - %s - %d engine(s)%n",
                        models.size() + 1,
                        model.modelName(),
                        model.makerName(),
                        model.aircraftType(),
                        model.certifiedEngines().size());
                models.add(model);
            }
            if (models.isEmpty()) {
                System.out.println("  No aircraft models registered.");
                return false;
            }

            final int modelChoice = Console.readInteger("Select Aircraft Model (number): ");
            if (modelChoice < 1 || modelChoice > models.size()) {
                System.out.println("Invalid selection.");
                return false;
            }
            final AircraftModel selectedModel = models.get(modelChoice - 1);

            final List<EngineModel> engines = new ArrayList<>(selectedModel.certifiedEngines());
            if (engines.isEmpty()) {
                System.out.println("  This aircraft model has no certified engines.");
                return false;
            }

            System.out.println("\n--- Certified Engines for " + selectedModel.modelName() + " ---");
            for (int i = 0; i < engines.size(); i++) {
                final EngineModel e = engines.get(i);
                System.out.printf("  [%d] %s (%s) - %s%n",
                        i + 1, e.name(), e.makerName(), e.engineType());
            }

            final int engineChoice = Console.readInteger("Select Engine Model to remove (number): ");
            if (engineChoice < 1 || engineChoice > engines.size()) {
                System.out.println("Invalid selection.");
                return false;
            }
            final EngineModel selectedEngine = engines.get(engineChoice - 1);

            final AircraftModel updated = controller.removeEngine(selectedModel, selectedEngine);

            System.out.println("\n Engine successfully removed!");
            System.out.println("  Model   : " + updated.modelName());
            System.out.println("  Maker   : " + updated.makerName());
            System.out.println("  Engines : " + updated.certifiedEngines().size() + " certified engine(s)");

        } catch (final IllegalArgumentException e) {
            System.out.println("\n Validation Error: " + e.getMessage());
        } catch (final Exception e) {
            System.out.println("\n An error occurred: " + e.getMessage());
        }
        return false;
    }

    @Override
    public String headline() {
        return "Remove Engine Model from Aircraft Model";
    }
}
