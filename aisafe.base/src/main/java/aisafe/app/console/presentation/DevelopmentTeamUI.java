package aisafe.app.console.presentation;

import eapli.framework.presentation.console.AbstractUI;

public class DevelopmentTeamUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        System.out.println("  Bruno Pinto");
        System.out.println("  Hugo Pereira");
        System.out.println("  Joana Braga");
        System.out.println("  Jorge Rocha");
        System.out.println("  Marcos Menezes");
        return true;
    }

    @Override
    public String headline() {
        return "Development Team";
    }
}
