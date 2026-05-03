package aisafe.app.console.presentation;

import aisafe.app.console.presentation.authz.LoginUI;
import eapli.framework.actions.menu.Menu;
import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.presentation.console.ExitWithMessageAction;
import eapli.framework.presentation.console.menu.MenuItemRenderer;
import eapli.framework.presentation.console.menu.MenuRenderer;
import eapli.framework.presentation.console.menu.VerticalMenuRenderer;

public class PreLoginMenu extends AbstractUI {

    private static final int EXIT_OPTION = 0;
    private static final int LOGIN_OPTION = 1;
    private static final int TEAM_OPTION = 2;

    @Override
    protected boolean doShow() {
        final MenuRenderer renderer = new VerticalMenuRenderer(buildMenu(), MenuItemRenderer.DEFAULT);
        return renderer.render();
    }

    @Override
    public String headline() {
        return "Main Menu";
    }

    private Menu buildMenu() {
        final var menu = new Menu();
        menu.addItem(LOGIN_OPTION, "Do Login", () -> {
            if (new LoginUI().show()) {
                new MainMenu().mainLoop();
            }
            return false;
        });
        menu.addItem(TEAM_OPTION, "Know the Development Team", () -> {
            new DevelopmentTeamUI().show();
            return false;
        });
        menu.addItem(EXIT_OPTION, "Cancel", new ExitWithMessageAction("Goodbye!"));
        return menu;
    }
}
