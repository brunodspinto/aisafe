package aisafe.dsl.parser;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parameterised tests that feed every DSL file under src/test/resources/dsl/
 * through the parser and assert the expected outcome (valid / invalid).
 */
class FlightPlanDslFileTest {

    // ── helpers ──────────────────────────────────────────────────────────────

    private static Stream<Path> dslFilesIn(final String resourceDir)
            throws IOException, URISyntaxException {
        final URI uri = FlightPlanDslFileTest.class.getResource(resourceDir).toURI();
        return Files.list(Paths.get(uri))
                .filter(p -> p.toString().endsWith(".dsl"))
                .sorted();
    }

    private FlightPlanParserFacade.ParseResult parse(final Path file) throws IOException {
        return new FlightPlanParserFacade().parse(Files.readString(file));
    }

    // ── valid files ───────────────────────────────────────────────────────────

    static Stream<Path> validFiles() throws IOException, URISyntaxException {
        return dslFilesIn("/dsl/valid");
    }

    @ParameterizedTest(name = "[valid] {0}")
    @MethodSource("validFiles")
    void ensureValidDslFileIsAccepted(final Path file) throws IOException {
        final FlightPlanParserFacade.ParseResult result = parse(file);
        assertTrue(result.isValid(),
                "Expected VALID but got errors: " + result.errors());
    }

    // ── invalid — lexical ─────────────────────────────────────────────────────

    static Stream<Path> invalidLexicalFiles() throws IOException, URISyntaxException {
        return dslFilesIn("/dsl/invalid/lexical");
    }

    @ParameterizedTest(name = "[invalid/lexical] {0}")
    @MethodSource("invalidLexicalFiles")
    void ensureInvalidLexicalDslFileIsRejected(final Path file) throws IOException {
        final FlightPlanParserFacade.ParseResult result = parse(file);
        assertFalse(result.isValid(),
                "Expected INVALID (lexical) but was accepted with no errors.");
    }

    // ── invalid — syntactic ───────────────────────────────────────────────────

    static Stream<Path> invalidSyntacticFiles() throws IOException, URISyntaxException {
        return dslFilesIn("/dsl/invalid/syntactic");
    }

    @ParameterizedTest(name = "[invalid/syntactic] {0}")
    @MethodSource("invalidSyntacticFiles")
    void ensureInvalidSyntacticDslFileIsRejected(final Path file) throws IOException {
        final FlightPlanParserFacade.ParseResult result = parse(file);
        assertFalse(result.isValid(),
                "Expected INVALID (syntactic) but was accepted with no errors.");
    }

    // ── invalid — semantic ────────────────────────────────────────────────────

    static Stream<Path> invalidSemanticFiles() throws IOException, URISyntaxException {
        return dslFilesIn("/dsl/invalid/semantic");
    }

    @ParameterizedTest(name = "[invalid/semantic] {0}")
    @MethodSource("invalidSemanticFiles")
    void ensureInvalidSemanticDslFileIsRejected(final Path file) throws IOException {
        final FlightPlanParserFacade.ParseResult result = parse(file);
        assertFalse(result.isValid(),
                "Expected INVALID (semantic) but was accepted with no errors.");
    }
}
