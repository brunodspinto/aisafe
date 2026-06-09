package aisafe.app.console.presentation;

import aisafe.app.console.presentation.aircontrolarea.RegisterAirControlAreaUI;
import aisafe.app.console.presentation.airtransportcompany.RegisterAirTransportCompanyUI;
import aisafe.app.console.presentation.authz.AddUserUI;
import aisafe.app.console.presentation.weatherdata.ConsultWeatherDataUI;
import aisafe.app.console.presentation.weatherdata.ImportBulkWeatherDataUI;
import aisafe.app.console.presentation.weatherdata.RegisterWeatherDataUI;
import aisafe.app.console.presentation.authz.DisableEnableUserUI;
import aisafe.app.console.presentation.authz.ListUsersUI;
import aisafe.app.console.presentation.authz.LogoutUI;
import aisafe.usermanagement.domain.AiSafeRoles;
import aisafe.app.console.presentation.flightplan.CreateFlightPlanFromFileUI;
import aisafe.app.console.presentation.flightplan.CreateFlightPlanUI;
import aisafe.app.console.presentation.flightplan.TestFlightPlanUI;
import aisafe.app.console.presentation.airport.RegisterAirportUI;
import aisafe.app.console.presentation.enginemodel.RegisterEngineModelUI;
import aisafe.app.console.presentation.maker.RegisterMakerUI;
import aisafe.app.console.presentation.aircraftmodel.RegisterAircraftModelUI;
import aisafe.app.console.presentation.aircraftmodel.AddEngineToAircraftModelUI;
import aisafe.app.console.presentation.aircraftmodel.RemoveEngineFromAircraftModelUI;
import aisafe.app.console.presentation.aircraft.AddAircraftUI;
import aisafe.app.console.presentation.collaborator.AddCollaboratorUI;
import aisafe.app.console.presentation.collaborator.DisableCollaboratorUI;
import aisafe.app.console.presentation.collaborator.ListCollaboratorsByCustomerUI;
import aisafe.app.console.presentation.collaborator.EditCollaboratorUI;
import aisafe.app.console.presentation.aircraft.DecommissionAircraftUI;
import aisafe.app.console.presentation.aircraft.ListFleetUI;
import aisafe.app.console.presentation.pilot.AddPilotUI;
import aisafe.app.console.presentation.pilot.ListPilotRosterUI;
import aisafe.app.console.presentation.pilot.RemovePilotUI;
import aisafe.app.console.presentation.flightroute.CreateFlightRouteUI;
import aisafe.app.console.presentation.flightroute.DeactivateFlightRouteUI;
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
        int option = 1;

        menu.addSubMenu(option++, buildMyAccountMenu());
        menu.addItem(MenuItem.separator(SEPARATOR));

        if (authz.isAuthenticatedUserAuthorizedTo(AiSafeRoles.ADMIN)) {
            menu.addSubMenu(option++, buildUsersMenu());
            menu.addItem(MenuItem.separator(SEPARATOR));
        }

        if (authz.isAuthenticatedUserAuthorizedTo(AiSafeRoles.BACKOFFICE_OPERATOR)) {
            menu.addSubMenu(option++, buildCompaniesMenu());
            menu.addItem(MenuItem.separator(SEPARATOR));

            menu.addSubMenu(option++, buildAirControlMenu());
            menu.addItem(MenuItem.separator(SEPARATOR));

            menu.addSubMenu(option++, buildAircraftMenu());
            menu.addItem(MenuItem.separator(SEPARATOR));

            menu.addSubMenu(option++, buildCollaboratorMenu());
            menu.addItem(MenuItem.separator(SEPARATOR));
        }
        if (authz.isAuthenticatedUserAuthorizedTo(AiSafeRoles.ATCC)) {
            menu.addSubMenu(option++, buildFleetMenu());
            menu.addItem(MenuItem.separator(SEPARATOR));

            menu.addSubMenu(option++, buildPilotMenu());
            menu.addItem(MenuItem.separator(SEPARATOR));

            menu.addSubMenu(option++, buildFlightRouteMenu());
            menu.addItem(MenuItem.separator(SEPARATOR));
        }

        if (authz.isAuthenticatedUserAuthorizedTo(AiSafeRoles.PILOT)) {
            menu.addSubMenu(option++, buildFlightPlanMenu());
            menu.addItem(MenuItem.separator(SEPARATOR));
        }

        if (canConsultWeatherData()) {
            menu.addSubMenu(option++, buildWeatherMenu());
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
        int option = 1;
        menu.addItem(option++, "Consult Weather Data", new ConsultWeatherDataUI()::show);
        if (authz.isAuthenticatedUserAuthorizedTo(AiSafeRoles.WEATHER_PERSON)) {
            menu.addItem(option++, "Register Weather Data", new RegisterWeatherDataUI()::show);
            menu.addItem(option++, "Import Bulk Weather Data", new ImportBulkWeatherDataUI()::show);
        }
        menu.addItem(EXIT_OPTION, "Return", Actions.SUCCESS);
        return menu;
    }

    private boolean canConsultWeatherData() {
        return authz.isAuthenticatedUserAuthorizedTo(AiSafeRoles.WEATHER_PERSON)
                || authz.isAuthenticatedUserAuthorizedTo(AiSafeRoles.PILOT)
                || authz.isAuthenticatedUserAuthorizedTo(AiSafeRoles.FLIGHT_CONTROL_OPERATOR);
    }

    private Menu buildFlightPlanMenu() {
        final var menu = new Menu("Flight Plans >");
        menu.addItem(1, "Create Flight Plan", new CreateFlightPlanUI()::show);
        menu.addItem(2, "Create Flight Plan from DSL File", new CreateFlightPlanFromFileUI()::show);
        menu.addItem(3, "Test Flight Plan", new TestFlightPlanUI()::show);
        menu.addItem(EXIT_OPTION, "Return", Actions.SUCCESS);
        return menu;
    }

    private Menu buildAircraftMenu() {
        final var menu = new Menu("Aircraft >");
        menu.addItem(1, "Register Engine Model", new RegisterEngineModelUI()::show);
        menu.addItem(2, "Register Maker", new RegisterMakerUI()::show);
        menu.addItem(3, "Register Aircraft Model", new RegisterAircraftModelUI()::show);
        menu.addItem(4, "Add Engine to Aircraft Model", new AddEngineToAircraftModelUI()::show);
        menu.addItem(5, "Remove Engine from Aircraft Model", new RemoveEngineFromAircraftModelUI()::show);
        menu.addItem(EXIT_OPTION, "Return", Actions.SUCCESS);
        return menu;
    }

    private Menu buildCollaboratorMenu() {
        final var menu = new Menu("Collaborators >");
        menu.addItem(1, "Add Customer's Collaborator", new AddCollaboratorUI()::show);
        menu.addItem(2, "List Customer's Collaborators", new ListCollaboratorsByCustomerUI()::show);
        menu.addItem(3, "Edit Customer's Collaborator", new EditCollaboratorUI()::show);
        menu.addItem(4, "Disable Customer's Collaborator", new DisableCollaboratorUI()::show);
        menu.addItem(EXIT_OPTION, "Return", Actions.SUCCESS);
        return menu;
    }

    private Menu buildFleetMenu() {
        final var menu = new Menu("Fleet Management >");
        menu.addItem(1, "Add Aircraft to Fleet", new AddAircraftUI()::show);
        menu.addItem(2, "Decommission Aircraft", new DecommissionAircraftUI()::show);
        menu.addItem(3, "List Fleet", new ListFleetUI()::show);
        menu.addItem(EXIT_OPTION, "Return", Actions.SUCCESS);
        return menu;
    }

    private Menu buildPilotMenu() {
        final var menu = new Menu("Pilots >");
        menu.addItem(1, "Add Pilot", new AddPilotUI()::show);
        menu.addItem(2, "List Pilot Roster", new ListPilotRosterUI()::show);
        menu.addItem(3, "Remove Pilot", new RemovePilotUI()::show);
        menu.addItem(EXIT_OPTION, "Return", Actions.SUCCESS);
        return menu;
    }

    private Menu buildFlightRouteMenu() {
        final var menu = new Menu("Flight Routes >");
        menu.addItem(1, "Create Flight Route", new CreateFlightRouteUI()::show);
        menu.addItem(2, "Deactivate Flight Route", new DeactivateFlightRouteUI()::show);
        menu.addItem(EXIT_OPTION, "Return", Actions.SUCCESS);
        return menu;
    }
}
