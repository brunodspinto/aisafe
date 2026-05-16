package aisafe.dsl.parser;

import aisafe.dsl.ast.FlightPlanAst;
import aisafe.dsl.generated.FlightPlanDslLexer;
import aisafe.dsl.generated.FlightPlanDslParser;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class FlightPlanParserFacade {

	public ParseResult parse(final String inputDsl) {
		final List<ParseError> errors = new ArrayList<>();
		final BaseErrorListener errorListener = new BaseErrorListener() {
			@Override
			public void syntaxError(final Recognizer<?, ?> recognizer,
								final Object offendingSymbol,
								final int line,
								final int charPositionInLine,
								final String msg,
								final RecognitionException e) {
				final String symbol = offendingSymbol == null ? "<unknown>" : offendingSymbol.toString();
				errors.add(new ParseError(line, charPositionInLine, msg, symbol));
			}
		};

		final FlightPlanDslLexer lexer = new FlightPlanDslLexer(CharStreams.fromString(inputDsl));
		lexer.removeErrorListeners();
		lexer.addErrorListener(errorListener);

		final FlightPlanDslParser parser = new FlightPlanDslParser(new CommonTokenStream(lexer));
		parser.removeErrorListeners();
		parser.addErrorListener(errorListener);

		final FlightPlanDslParser.FlightPlanContext root = parser.flightPlan();

		// Stage 1: syntactic errors already collected
		if (!errors.isEmpty()) {
			return ParseResult.invalid(errors);
		}

		// Stage 2: parse-tree range validation via Listener
		ParseTreeWalker.DEFAULT.walk(new FlightPlanValidationListener(errors), root);
		if (!errors.isEmpty()) {
			return ParseResult.invalid(errors);
		}

		// Stage 3: AST construction via Visitor
		final FlightPlanAst ast = (FlightPlanAst) new FlightPlanAstBuilderVisitor().visit(root.flight());

		// Stage 4: cross-field semantic validation on AST
		final List<ParseError> semanticErrors = new FlightPlanSemanticValidator().validate(ast);
		if (!semanticErrors.isEmpty()) {
			return ParseResult.invalid(semanticErrors);
		}
		return ParseResult.valid(ast);
	}

	public record ParseError(int line, int column, String message, String offendingSymbol) {
	}

	public static final class ParseResult {
		private final FlightPlanAst ast;
		private final List<ParseError> errors;

		private ParseResult(final FlightPlanAst ast, final List<ParseError> errors) {
			this.ast = ast;
			this.errors = List.copyOf(errors);
		}

		public static ParseResult valid(final FlightPlanAst ast) {
			return new ParseResult(ast, Collections.emptyList());
		}

		public static ParseResult invalid(final List<ParseError> errors) {
			return new ParseResult(null, errors);
		}

		public boolean isValid() {
			return errors.isEmpty() && ast != null;
		}

		public Optional<FlightPlanAst> ast() {
			return Optional.ofNullable(ast);
		}

		public List<ParseError> errors() {
			return errors;
		}
	}
}
