package aisafe.flightplan.application;

import aisafe.dsl.FlightPlanJsonSerializer;
import aisafe.dsl.ast.FlightPlanAst;
import aisafe.dsl.parser.FlightPlanParserFacade;
import aisafe.flightplan.domain.FlightPlan;
import aisafe.flightplan.domain.FlightPlanDesignator;
import aisafe.flightplan.domain.FlightPlanStatus;
import aisafe.flightplan.repositories.FlightPlanRepository;
import aisafe.infrastructure.application.AppSettings;
import aisafe.infrastructure.persistence.PersistenceContext;
import aisafe.usermanagement.domain.AiSafeRoles;
import eapli.framework.application.UseCaseController;
import eapli.framework.infrastructure.authz.application.AuthorizationService;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Application controller for US085 — Test/Validate a Flight Plan.
 *
 * <p>Orchestrates: authorisation check → load plan → guard checks → DSL re-parse →
 * JSON serialisation → C binary invocation → result processing → status update.</p>
 *
 * <p>The actual simulation logic is fully delegated to the {@code flight_tester} C binary
 * (AC085.7). This controller never implements flight simulation logic.</p>
 */
@UseCaseController
public class TestFlightPlanController {

    private static final int TIMEOUT_SECONDS = 30;

    /**
     * Grace period for the stdout reader thread to drain after the process exits. The process has
     * already terminated by this point, so EOF is imminent; a generous bound (vs. a tight 1s)
     * avoids abandoning the daemon thread with partial output on slow/loaded machines.
     */
    private static final long READER_JOIN_TIMEOUT_MS = 5000L;

    private final AuthorizationService    authz;
    private final FlightPlanRepository    repository;
    private final FlightPlanParserFacade  parser;
    private final String                  binaryPath;
    private final FlightTesterRunner      runner;

    /** Runtime constructor — pulls from infrastructure registries. */
    public TestFlightPlanController() {
        this.authz      = AuthzRegistry.authorizationService();
        this.repository = PersistenceContext.repositories().flightPlans();
        this.parser     = new FlightPlanParserFacade();
        this.binaryPath = new AppSettings().flightTesterBinary();
        this.runner     = this::runBinary;
    }

    /**
     * Testing constructor for guard-condition tests — accepts only a repository.
     * Parser and runner are null; tests that exercise only the guard checks before
     * the binary invocation use this constructor. Package-private.
     */
    TestFlightPlanController(final FlightPlanRepository repository) {
        this.authz      = null;
        this.repository = repository;
        this.parser     = null;
        this.binaryPath = null;
        this.runner     = null;
    }

    /**
     * Testing constructor for execution-path tests — accepts a repository and a
     * {@link FlightTesterRunner} stub. Wires a real {@link FlightPlanParserFacade}
     * (pure Java, no infrastructure required). Package-private.
     */
    TestFlightPlanController(final FlightPlanRepository repository,
                             final FlightTesterRunner runner) {
        this.authz      = null;
        this.repository = repository;
        this.parser     = new FlightPlanParserFacade();
        this.binaryPath = null;
        this.runner     = runner;
    }

    /**
     * Returns all VALIDATED, DSL-based flight plans eligible for simulation testing.
     * Form-based plans (those with {@code dslContent == null}) are excluded.
     *
     * @return iterable of testable flight plans (never null, may be empty)
     */
    public Iterable<FlightPlan> validatedDslPlans() {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.PILOT);
        return listValidatedDslPlans();
    }

    /** Package-private overload used by tests — bypasses auth. */
    List<FlightPlan> listValidatedDslPlans() {
        final List<FlightPlan> result = new ArrayList<>();
        for (final FlightPlan fp : repository.findAllValidated()) {
            if (fp.dslContent() != null) {
                result.add(fp);
            }
        }
        return result;
    }

    /**
     * Runs the C-based simulation test for the given flight plan designator.
     *
     * <p>On PASS: the plan transitions to {@link FlightPlanStatus#TESTED} and is persisted.</p>
     * <p>On FAIL: an {@link IllegalStateException} is thrown with the reason from the C binary;
     * the plan status remains {@link FlightPlanStatus#VALIDATED}.</p>
     *
     * @param designator the flight plan designator string (case-insensitive)
     * @return the updated {@link FlightPlan} in {@code TESTED} status
     * @throws IllegalArgumentException if no plan with the given designator exists
     * @throws IllegalStateException    if the plan is not in VALIDATED status, has no DSL content,
     *                                  or if the C binary reports a FAIL result
     * @throws RuntimeException         if the C binary is not found, times out, or fails to start
     * @throws IOException              if reading or writing the temp file fails
     */
    public FlightPlan testFlightPlan(final String designator) throws IOException {
        authz.ensureAuthenticatedUserHasAnyOf(AiSafeRoles.PILOT);
        return executeTestFlightPlan(designator);
    }

    /**
     * Implements the test-flight-plan business logic without an authorisation check.
     * Package-private so unit tests can exercise the guard conditions directly, without
     * needing a live auth context or a C binary on the test classpath.
     */
    FlightPlan executeTestFlightPlan(final String designator) throws IOException {
        final FlightPlan plan = repository
                .ofIdentity(FlightPlanDesignator.valueOf(designator.trim().toUpperCase()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Flight plan not found: " + designator));

        if (plan.status() != FlightPlanStatus.VALIDATED) {
            throw new IllegalStateException(
                    "Cannot test a plan that is not in VALIDATED status. Current status: "
                    + plan.status());
        }
        if (plan.dslContent() == null) {
            throw new IllegalStateException(
                    "Flight plan has no DSL content to test. Only DSL-based plans can be tested.");
        }

        // Re-parse the DSL to get the AST
        final FlightPlanParserFacade.ParseResult parseResult = parser.parse(plan.dslContent());
        if (!parseResult.isValid()) {
            throw new IllegalStateException(
                    "DSL content is no longer valid — cannot test: " + parseResult.errors());
        }
        final FlightPlanAst ast = parseResult.ast()
                .orElseThrow(() -> new IllegalStateException(
                        "Parse succeeded but produced no AST — this is a parser bug."));

        Path tempFile = null;
        try {
            tempFile = new FlightPlanJsonSerializer().toTempFile(ast);
            final String output = runner.run(tempFile).trim();
            final String status = extractJsonValue(output, "status");

            if ("PASS".equals(status)) {
                plan.markTested();
                return repository.save(plan);
            } else {
                final String reason = extractJsonValue(output, "reason");
                throw new IllegalStateException(
                        "Simulation failed: " + (reason != null ? reason : output));
            }

        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (final IOException ignored) {
                    // best-effort cleanup
                }
            }
        }
    }

    /**
     * Invokes the {@code flight_tester} C binary with the given JSON input file and
     * returns its stdout. Used by the runtime {@link FlightTesterRunner} lambda.
     */
    private String runBinary(final Path jsonFile) throws IOException {
        final ProcessBuilder pb = new ProcessBuilder(binaryPath, jsonFile.toString());
        pb.redirectErrorStream(true);

        final Process process;
        try {
            process = pb.start();
        } catch (final IOException e) {
            throw new RuntimeException(
                    "Flight tester binary not found. Check flight.tester.binary in "
                    + "application.properties. Path: " + binaryPath, e);
        }

        // Read stdout in a background thread to prevent pipe-buffer deadlock
        final StringBuilder outputBuilder = new StringBuilder();
        final Thread outputReader = new Thread(() -> {
            try (final BufferedReader reader =
                         new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputBuilder.append(line);
                }
            } catch (final IOException ignored) {
                // stream closed on process exit
            }
        });
        outputReader.setDaemon(true);
        outputReader.start();

        final boolean finished;
        try {
            finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new RuntimeException("Flight tester interrupted while waiting.", e);
        }
        if (!finished) {
            // SIGTERM first — gives the C binary a chance to unlink named IPC objects.
            process.destroy();
            boolean terminated;
            try {
                terminated = process.waitFor(5, TimeUnit.SECONDS);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                terminated = false;
            }
            if (!terminated) {
                process.destroyForcibly(); // SIGKILL — last resort
            }
            throw new RuntimeException(
                    "Flight tester timed out after " + TIMEOUT_SECONDS + " seconds.");
        }

        try {
            outputReader.join(READER_JOIN_TIMEOUT_MS);
        } catch (final InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        return outputBuilder.toString();
    }

    /**
     * Extracts a string value from a flat JSON object string.
     * Only handles string values (quoted) — sufficient for the C binary's output format.
     */
    private String extractJsonValue(final String json, final String key) {
        final String searchKey = "\"" + key + "\"";
        final int keyIdx = json.indexOf(searchKey);
        if (keyIdx < 0) return null;
        final int colonIdx = json.indexOf(':', keyIdx + searchKey.length());
        if (colonIdx < 0) return null;
        final String rest = json.substring(colonIdx + 1).trim();
        if (rest.startsWith("\"")) {
            final int endIdx = rest.indexOf('"', 1);
            return endIdx < 0 ? null : rest.substring(1, endIdx);
        }
        return null;
    }
}
