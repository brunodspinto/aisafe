package aisafe.app.console.presentation;

import eapli.framework.presentation.console.AbstractUI;

public class DevelopmentTeamUI extends AbstractUI {

    @Override
    protected boolean doShow() {
        System.out.println("  Bruno Pinto     - [removed]");
        System.out.println("  Hugo Pereira    - [removed]");
        System.out.println("  Joana Braga     - [removed]");
        System.out.println("  Jorge Rocha     - [removed]");
        System.out.println("  Marcos Menezes  - [removed]");
        return true;
    }

    @Override
    public String headline() {
        return "Development Team";
    }
}
