package aisafe.flightplan.application;

import aisafe.dsl.ast.FlightPlanAst;
import aisafe.dsl.parser.FlightPlanParserFacade;
import aisafe.dsl.parser.FlightPlanParserFacade.ParseError;
import aisafe.dsl.parser.FlightPlanParserFacade.ParseResult;
import aisafe.flightplan.domain.FlightPlan;
import aisafe.flightplan.domain.FlightPlanDesignator;
import aisafe.flightplan.repositories.FlightPlanRepository;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Controller for US081 - Create a flight plan from a DSL file.
 * Reads the file, validates it (lexical, syntactic and semantic),
 * and if valid creates a FlightPlan in DRAFT status.
 */
@UseCaseController
public class CreateFlightPlanFromFileController {

    private final AuthorizationService authz = AuthzRegistry.authorizationService();
    private final FlightPlanRepository repository =
            PersistenceContext.repositories().flightPlans();
    private final FlightPlanParserFacade parser = new FlightPlanParserFacade();

    /**
     * Creates a flight plan from a DSL file path.
     *
     * @param filePath path to the DSL file
     * @return the created FlightPlan in DRAFT status
     * @throws IOException              if the file cannot be read
     * @throws IllegalArgumentException if the DSL content is invalid
     * @throws IllegalStateException    if a flight plan with the same designator already exists
     */
    public FlightPlan createFromFile(final String filePath) throws IOException {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.PILOT);

        final String dslContent = readFile(filePath);
        final ParseResult result = parser.parse(dslContent);

        if (!result.isValid()) {
            throw new IllegalArgumentException(buildErrorMessage(result.errors()));
        }

        final FlightPlanAst ast = result.ast()
                .orElseThrow(() -> new IllegalStateException(
                        "Parse succeeded but produced no AST — this is a parser bug."));

        if (repository.ofIdentity(FlightPlanDesignator.valueOf(ast.identifier())).isPresent()) {
            throw new IllegalStateException(
                    "A flight plan with designator '" + ast.identifier() + "' already exists.");
        }

        final FlightPlan flightPlan = FlightPlan.fromDsl(ast, dslContent);
        return repository.save(flightPlan);
    }

    private String readFile(final String filePath) throws IOException {
        return Files.readString(Path.of(filePath));
    }

    private String buildErrorMessage(final List<ParseError> errors) {
        final StringBuilder sb = new StringBuilder("Flight plan file is invalid:\n");
        for (final ParseError error : errors) {
            sb.append(String.format("  [line %d, col %d] %s (near '%s')%n",
                    error.line(), error.column(), error.message(), error.offendingSymbol()));
        }
        return sb.toString();
    }
}