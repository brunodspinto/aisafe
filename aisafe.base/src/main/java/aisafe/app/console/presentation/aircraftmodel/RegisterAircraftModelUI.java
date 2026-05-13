package aisafe.app.console.presentation.aircraftmodel;

import aisafe.aircraftmodel.application.RegisterAircraftModelController;
import aisafe.aircraftmodel.domain.AircraftModel;
import aisafe.aircraftmodel.domain.AircraftType;
import aisafe.enginemodel.domain.EngineModel;
import aisafe.maker.domain.Maker;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

import java.util.ArrayList;
import java.util.List;

/**
 * Console UI for the "Register Aircraft Model" use case (US056).
 * Guides the user through selecting a maker, first engine model, aircraft type,
 * and entering all performance parameters.
 */
public class RegisterAircraftModelUI extends AbstractUI {

    private final RegisterAircraftModelController controller = new RegisterAircraftModelController();

    @Override
    protected boolean doShow() {
        try {
            System.out.println("\n--- Available Makers ---");
            final List<Maker> makers = new ArrayList<>();
            for (final Maker maker : controller.allMakers()) {
                System.out.printf("  [%d] %s (%s)%n", makers.size() + 1, maker.name(), maker.country());
                makers.add(maker);
            }
            if (makers.isEmpty()) {
                System.out.println("  No makers registered. Please register a maker first.");
                return false;
            }

            System.out.println("\n--- Available Engine Models ---");
            final List<EngineModel> engines = new ArrayList<>();
            for (final EngineModel engine : controller.allEngineModels()) {
                System.out.printf("  [%d] %s (%s) - %s%n",
                        engines.size() + 1, engine.name(), engine.makerName(), engine.engineType());
                engines.add(engine);
            }
            if (engines.isEmpty()) {
                System.out.println("  No engine models registered. Please register an engine model first.");
                return false;
            }

            System.out.println();
            final String modelName = Console.readLine("Model Name: ");

            final int makerChoice = Console.readInteger("Select Maker (number): ");
            if (makerChoice < 1 || makerChoice > makers.size()) {
                System.out.println("Invalid selection.");
                return false;
            }
            final String makerName = makers.get(makerChoice - 1).name();

            System.out.println("\nAircraft Type:");
            final AircraftType[] types = controller.aircraftTypes();
            for (int i = 0; i < types.length; i++) {
                System.out.printf("  %d - %s%n", i + 1, types[i]);
            }
            final int typeChoice = Console.readInteger("Choice: ");
            if (typeChoice < 1 || typeChoice > types.length) {
                System.out.println("Invalid selection.");
                return false;
            }
            final String aircraftType = types[typeChoice - 1].name();

            final double emptyWeight = Console.readDouble("Empty Weight (kg): ");
            final double mtow = Console.readDouble("MTOW - Maximum Take-Off Weight (kg): ");
            final double mzfw = Console.readDouble("MZFW - Maximum Zero Fuel Weight (kg): ");
            final double maxFuelCapacity = Console.readDouble("Max Fuel Capacity (kg): ");
            final double serviceCeiling = Console.readDouble("Service Ceiling (m): ");
            final double cruiseSpeed = Console.readDouble("Cruise Speed (m/s): ");
            final double wingSpan = Console.readDouble("Wing Span (m): ");
            final double wingArea = Console.readDouble("Wing Area (m²): ");
            final double dragCoefficient = Console.readDouble("Drag Coefficient (Cd): ");
            final double liftCoefficient = Console.readDouble("Lift Coefficient (Cl): ");

            final int engineChoice = Console.readInteger("Select Engine Model (number): ");
            if (engineChoice < 1 || engineChoice > engines.size()) {
                System.out.println("Invalid selection.");
                return false;
            }
            final EngineModel selectedEngine = engines.get(engineChoice - 1);

            final AircraftModel model = controller.registerAircraftModel(
                    modelName, makerName, aircraftType,
                    emptyWeight, mtow, mzfw, maxFuelCapacity,
                    serviceCeiling, cruiseSpeed, wingSpan, wingArea,
                    dragCoefficient, liftCoefficient, selectedEngine
            );

            System.out.println("\n Aircraft Model successfully registered!");
            System.out.println("  Model    : " + model.modelName());
            System.out.println("  Maker    : " + model.maker().name());
            System.out.println("  Type     : " + model.aircraftType());
            System.out.println("  Engines  : " + model.certifiedEngines().size() + " certified engine(s)");

        } catch (final IllegalArgumentException e) {
            System.out.println("\n Validation Error: " + e.getMessage());
        } catch (final Exception e) {
            System.out.println("\n An error occurred: " + e.getMessage());
        }
        return false;
    }

    @Override
    public String headline() {
        return "Register Aircraft Model";
    }
}
