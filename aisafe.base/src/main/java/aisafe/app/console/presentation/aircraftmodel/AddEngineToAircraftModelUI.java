package aisafe.app.console.presentation.aircraftmodel;

import aisafe.aircraftmodel.application.AddEngineToAircraftModelController;
import aisafe.aircraftmodel.domain.AircraftModel;
import aisafe.enginemodel.domain.EngineModel;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

public class AddEngineToAircraftModelUI extends AbstractUI {

    private final AddEngineToAircraftModelController controller =
            new AddEngineToAircraftModelController();

    @Override
    protected boolean doShow() {
        try {
            System.out.println("\n--- Available Aircraft Models ---");
            final List<AircraftModel> models = new ArrayList<>();
            for (final AircraftModel model : controller.allAircraftModels()) {
                System.out.printf("  [%d] %s (%s) - %s - %d engine(s)%n",
                        models.size() + 1,
                        model.modelName(),
                        model.maker().name(),
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

            System.out.println("\n--- Available Engine Models ---");
            final List<EngineModel> engines = new ArrayList<>();
            for (final EngineModel engine : controller.allEngineModels()) {
                System.out.printf("  [%d] %s (%s) - %s%n",
                        engines.size() + 1,
                        engine.name(),
                        engine.makerName(),
                        engine.engineType());
                engines.add(engine);
            }
            if (engines.isEmpty()) {
                System.out.println("  No engine models registered.");
                return false;
            }

            final int engineChoice = Console.readInteger("Select Engine Model to add (number): ");
            if (engineChoice < 1 || engineChoice > engines.size()) {
                System.out.println("Invalid selection.");
                return false;
            }
            final EngineModel selectedEngine = engines.get(engineChoice - 1);

            final AircraftModel updated = controller.addEngine(selectedModel, selectedEngine);

            System.out.println("\n Engine successfully added!");
            System.out.println("  Model   : " + updated.modelName());
            System.out.println("  Maker   : " + updated.maker().name());
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
        return "Add Engine Model to Aircraft Model";
    }
}
