package aisafe.app.console.presentation.maker;

import aisafe.maker.application.RegisterMakerController;
import aisafe.maker.domain.Maker;
import eapli.framework.io.util.Console;
import eapli.framework.presentation.console.AbstractUI;

public class RegisterMakerUI extends AbstractUI {

    private final RegisterMakerController controller = new RegisterMakerController();

    @Override
    protected boolean doShow() {
        try {
            final String name = Console.readLine("Maker Name: ");
            final String country = Console.readLine("Country: ");

            final Maker maker = controller.registerMaker(name, country);

            System.out.println("\n Maker successfully registered!");
            System.out.println("  Name    : " + maker.name());
            System.out.println("  Country : " + maker.country());

        } catch (final IllegalArgumentException e) {
            System.out.println("\n Validation Error: " + e.getMessage());
        } catch (final Exception e) {
            System.out.println("\n An error occurred: " + e.getMessage());
        }
        return false;
    }

    @Override
    public String headline() {
        return "Register Maker";
    }
}
