package aisafe.app.console.presentation;

import aisafe.app.console.presentation.aircontrolarea.RegisterAirControlAreaUI;
import aisafe.app.console.presentation.airtransportcompany.RegisterAirTransportCompanyUI;
import aisafe.app.console.presentation.authz.AddUserUI;
import aisafe.app.console.presentation.weatherdata.RegisterWeatherDataUI;
import aisafe.app.console.presentation.authz.DisableEnableUserUI;
import aisafe.app.console.presentation.authz.ListUsersUI;
import aisafe.app.console.presentation.authz.LogoutUI;
import aisafe.usermanagement.domain.AiSafeRoles;
import aisafe.app.console.presentation.flightplan.CreateFlightPlanFromFileUI;
import aisafe.app.console.presentation.airport.RegisterAirportUI;
import aisafe.app.console.presentation.enginemodel.RegisterEngineModelUI;
import aisafe.app.console.presentation.maker.RegisterMakerUI;
import aisafe.app.console.presentation.aircraftmodel.RegisterAircraftModelUI;
import aisafe.app.console.presentation.aircraftmodel.AddEngineToAircraftModelUI;
import aisafe.app.console.presentation.collaborator.AddCollaboratorUI;
import aisafe.app.console.presentation.collaborator.ListCollaboratorsByCustomerUI;
import eapli.framework.actions.Actions;
import eapli.framework.actions.menu.Menu;
import eapli.framework.actions.menu.MenuItem;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.presentation.console.AbstractUI;
import eapli.framework.presentation.console.ExitWithMessageAction;
import eapli.framework.presentation.console.menu.MenuItemRenderer;
import eapli.framework.presentation.console.menu.MenuRenderer;
import eapli.framework.presentation.console.menu.VerticalMenuRenderer;

public class MainMenu extends AbstractUI {

    private static final int EXIT_OPTION = 0;
    private static final int MY_ACCOUNT_OPTION = 1;
    private static final int USERS_OPTION = 2;
    private static final int COMPANIES_OPTION = 3;
    private static final int AIR_CONTROL_OPTION = 4;
    private static final int WEATHER_OPTION = 5;
    private static final int FLIGHT_PLAN_OPTION = 6;
    private static final int AIRCRAFT_OPTION = 7;
    private static final int COLLABORATOR_OPTION = 8;
    private static final String SEPARATOR = "--------------";

    private final AuthorizationService authz = AuthzRegistry.authorizationService();

    @Override
    public boolean show() {
        drawFormTitle();
        return doShow();
    }

    @Override
    protected boolean doShow() {
        final MenuRenderer renderer = new VerticalMenuRenderer(buildMainMenu(), MenuItemRenderer.DEFAULT);
        return renderer.render();
    }

    @Override
    public String headline() {
        return authz.session()
                .map(s -> "AISafe [ @" + s.authenticatedUser().identity() + " ]")
                .orElse("AISafe");
    }

    private Menu buildMainMenu() {
        final var menu = new Menu();

        menu.addSubMenu(MY_ACCOUNT_OPTION, buildMyAccountMenu());
        menu.addItem(MenuItem.separator(SEPARATOR));

        if (authz.isAuthenticatedUserAuthorizedTo(AiSafeRoles.ADMIN)) {
            menu.addSubMenu(USERS_OPTION, buildUsersMenu());
            menu.addItem(MenuItem.separator(SEPARATOR));
        }

        if (authz.isAuthenticatedUserAuthorizedTo(AiSafeRoles.BACKOFFICE_OPERATOR)) {
            menu.addSubMenu(COMPANIES_OPTION, buildCompaniesMenu());
            menu.addItem(MenuItem.separator(SEPARATOR));

            menu.addSubMenu(AIR_CONTROL_OPTION, buildAirControlMenu());
            menu.addItem(MenuItem.separator(SEPARATOR));

            menu.addSubMenu(AIRCRAFT_OPTION, buildAircraftMenu());
            menu.addItem(MenuItem.separator(SEPARATOR));

            menu.addSubMenu(COLLABORATOR_OPTION, buildCollaboratorMenu());
            menu.addItem(MenuItem.separator(SEPARATOR));
        }
        if (authz.isAuthenticatedUserAuthorizedTo(AiSafeRoles.PILOT)) {
            menu.addSubMenu(FLIGHT_PLAN_OPTION, buildFlightPlanMenu());
            menu.addItem(MenuItem.separator(SEPARATOR));
        }

        if (authz.isAuthenticatedUserAuthorizedTo(AiSafeRoles.WEATHER_PERSON)) {
            menu.addSubMenu(WEATHER_OPTION, buildWeatherMenu());
            menu.addItem(MenuItem.separator(SEPARATOR));
        }

        menu.addItem(EXIT_OPTION, "Exit", new ExitWithMessageAction("Goodbye!"));
        return menu;
    }

    private Menu buildMyAccountMenu() {
        final var menu = new Menu("My Account >");
        menu.addItem(1, "Logout", new LogoutUI()::show);
        menu.addItem(EXIT_OPTION, "Return", Actions.SUCCESS);
        return menu;
    }

    private Menu buildUsersMenu() {
        final var menu = new Menu("Users >");
        menu.addItem(1, "Add User", new AddUserUI()::show);
        menu.addItem(2, "List Users", new ListUsersUI()::show);
        menu.addItem(3, "Disable/Enable User", new DisableEnableUserUI()::show);
        menu.addItem(EXIT_OPTION, "Return", Actions.SUCCESS);
        return menu;
    }

    private Menu buildCompaniesMenu() {
        final var menu = new Menu("Companies >");
        menu.addItem(1, "Register Air Transport Company", new RegisterAirTransportCompanyUI()::show);
        menu.addItem(EXIT_OPTION, "Return", Actions.SUCCESS);
        return menu;
    }

    private Menu buildAirControlMenu() {
        final var menu = new Menu("Air Control >");
        menu.addItem(1, "Register Air Control Area", new RegisterAirControlAreaUI()::show);
        menu.addItem(2, "Register Airport", new RegisterAirportUI()::show);
        menu.addItem(EXIT_OPTION, "Return", Actions.SUCCESS);
        return menu;
    }

    private Menu buildWeatherMenu() {
        final var menu = new Menu("Weather >");
        menu.addItem(1, "Register Weather Data", new RegisterWeatherDataUI()::show);
        menu.addItem(EXIT_OPTION, "Return", Actions.SUCCESS);
        return menu;
    }

    private Menu buildFlightPlanMenu() {
        final var menu = new Menu("Flight Plans >");
        menu.addItem(1, "Create Flight Plan from DSL File", new CreateFlightPlanFromFileUI()::show);
        menu.addItem(EXIT_OPTION, "Return", Actions.SUCCESS);
        return menu;
    }

    private Menu buildAircraftMenu() {
        final var menu = new Menu("Aircraft >");
        menu.addItem(1, "Register Engine Model", new RegisterEngineModelUI()::show);
        menu.addItem(2, "Register Maker", new RegisterMakerUI()::show);
        menu.addItem(3, "Register Aircraft Model", new RegisterAircraftModelUI()::show);
        menu.addItem(4, "Add Engine to Aircraft Model", new AddEngineToAircraftModelUI()::show);
        menu.addItem(EXIT_OPTION, "Return", Actions.SUCCESS);
        return menu;
    }

    private Menu buildCollaboratorMenu() {
        final var menu = new Menu("Collaborators >");
        menu.addItem(1, "Add Customer's Collaborator", new AddCollaboratorUI()::show);
        menu.addItem(2, "List Customer's Collaborators", new ListCollaboratorsByCustomerUI()::show);
        menu.addItem(EXIT_OPTION, "Return", Actions.SUCCESS);
        return menu;
    }
}
