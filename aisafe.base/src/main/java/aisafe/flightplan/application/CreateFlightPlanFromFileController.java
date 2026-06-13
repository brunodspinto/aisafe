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
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Controller for US121 - Create a flight plan from a DSL file.
 * Reads the file, validates it (lexical, syntactic and semantic),
 * and if valid creates a FlightPlan in DRAFT status.
 */
@UseCaseController
public class CreateFlightPlanFromFileController {

    private final FlightPlanRepository repository;
    private final FlightPlanParserFacade parser;
    private final Runnable authorizationGuard;

    public CreateFlightPlanFromFileController() {
        this(PersistenceContext.repositories().flightPlans(),
                new FlightPlanParserFacade(),
                null);
    }

    CreateFlightPlanFromFileController(final FlightPlanRepository repository,
                                       final FlightPlanParserFacade parser,
                                       final Runnable authorizationGuard) {
        if (repository == null) {
            throw new IllegalArgumentException("Flight Plan repository cannot be null.");
        }
        if (parser == null) {
            throw new IllegalArgumentException("Flight Plan parser cannot be null.");
        }
        this.repository = repository;
        this.parser = parser;
        this.authorizationGuard = authorizationGuard != null
                ? authorizationGuard
                : this::ensureAuthenticatedPilot;
    }

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
        authorizationGuard.run();

        final Path path = validateFilePath(filePath);
        final String dslContent = readFile(path);
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

    private void ensureAuthenticatedPilot() {
        AuthzRegistry.authorizationService().ensureAuthenticatedUserHasAnyOf(AiSafeRoles.PILOT);
    }

    private Path validateFilePath(final String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("Flight plan file path cannot be null or blank.");
        }
        final Path path = Path.of(filePath.trim());
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Flight plan file does not exist or is not a regular file: " + path);
        }
        final String lowerName = path.getFileName().toString().toLowerCase();
        if (!lowerName.endsWith(".dsl") && !lowerName.endsWith(".fpdsl")) {
            throw new IllegalArgumentException("Flight plan file must use .dsl or .fpdsl extension.");
        }
        return path;
    }

    private String readFile(final Path path) throws IOException {
        return Files.readString(path);
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
