package aisafe.dsl.parser;

import aisafe.dsl.ast.CoordinateAst;
import aisafe.dsl.ast.EndpointAst;
import aisafe.dsl.ast.FlightPlanAst;
import aisafe.dsl.ast.FlightType;
import aisafe.dsl.ast.FuelAst;
import aisafe.dsl.ast.LegAst;
import aisafe.dsl.ast.RouteAst;
import aisafe.dsl.ast.SegmentAst;
import aisafe.dsl.generated.FlightPlanDslLexer;
import aisafe.dsl.generated.FlightPlanDslParser;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

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

		if (!errors.isEmpty()) {
			return ParseResult.invalid(errors);
		}

		try {
			return ParseResult.valid(toFlightPlan(root.flight().get(0)));
		} catch (final RuntimeException ex) {
			return ParseResult.invalid(List.of(new ParseError(
					0,
					0,
					"Failed to build internal representation: " + ex.getMessage(),
					"<mapper>"
			)));
		}
	}

	private FlightPlanAst toFlightPlan(final FlightPlanDslParser.FlightContext ctx) {
		final List<LegAst> legs = new ArrayList<>();
		for (final FlightPlanDslParser.LegContext legContext : ctx.leg()) {
			legs.add(toLeg(legContext));
		}

		return new FlightPlanAst(
				ctx.IDENTIFIER().getText(),
				FlightType.valueOf(ctx.flightType().getText().toUpperCase()),
				List.copyOf(legs)
		);
	}

	private LegAst toLeg(final FlightPlanDslParser.LegContext ctx) {
		final List<SegmentAst> segments = new ArrayList<>();
		for (final FlightPlanDslParser.SegmentContext segmentContext : ctx.segment()) {
			segments.add(toSegment(segmentContext));
		}

		return new LegAst(
				toEndpoint(ctx.departure().airportCode().getText(), ctx.departure().dateTime().DATE().getText(), ctx.departure().dateTime().TIME().getText()),
				toEndpoint(ctx.arrival().airportCode().getText(), ctx.arrival().dateTime().DATE().getText(), ctx.arrival().dateTime().TIME().getText()),
				new RouteAst(ctx.route().airportCode(0).getText(), ctx.route().airportCode(1).getText()),
				List.copyOf(segments),
				new FuelAst(parseSignedNumber(ctx.fuel().signedNumber()), ctx.fuel().fuelUnit().getText().toUpperCase())
		);
	}

	private SegmentAst toSegment(final FlightPlanDslParser.SegmentContext ctx) {
		// Get first altitude slot
		final FlightPlanDslParser.AltitudeSlotContext altSlot = ctx.altitudeSlot(0);
		final double altitude = parseSignedNumber(altSlot.altitude().signedNumber());
		final double width = parseSignedNumber(altSlot.distance().signedNumber());
		
		// Get wind declaration
		final FlightPlanDslParser.WindDeclContext windDecl = ctx.windDecl();
		final double windDirection = parseSignedNumber(windDecl.windDirection().signedNumber());
		final double windSpeed = parseSignedNumber(windDecl.windSpeed().signedNumber());
		
		return new SegmentAst(
				toCoordinate(ctx.coordinate(0)),
				toCoordinate(ctx.coordinate(1)),
				altitude,
				width,
				windSpeed,
				windDirection
		);
	}

	private EndpointAst toEndpoint(final String airportCode, final String date, final String time) {
		return new EndpointAst(airportCode, date, time);
	}

	private CoordinateAst toCoordinate(final FlightPlanDslParser.CoordinateContext ctx) {
		return new CoordinateAst(parseSignedNumber(ctx.signedNumber(0)), parseSignedNumber(ctx.signedNumber(1)));
	}

	private double parseSignedNumber(final FlightPlanDslParser.SignedNumberContext ctx) {
		final String sign = ctx.PLUS() != null ? "+" : ctx.MINUS() != null ? "-" : "";
		return parseNumber(sign + ctx.NUMBER().getText());
	}

	private double parseNumber(final String value) {
		return Double.parseDouble(value);
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



