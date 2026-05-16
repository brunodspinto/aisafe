package aisafe.dsl.parser;

import aisafe.dsl.generated.FlightPlanDslBaseListener;
import aisafe.dsl.generated.FlightPlanDslParser;
import aisafe.dsl.parser.FlightPlanParserFacade.ParseError;

import java.util.List;

/**
 * Listener that performs range validation on the ANTLR4 parse tree.
 * Errors are accumulated into the shared list provided at construction time.
 */
public final class FlightPlanValidationListener extends FlightPlanDslBaseListener {

    private final List<ParseError> errors;

    public FlightPlanValidationListener(final List<ParseError> errors) {
        this.errors = errors;
    }

    @Override
    public void exitWindDecl(final FlightPlanDslParser.WindDeclContext ctx) {
        final double dir = parseSignedNumber(ctx.windDirection().signedNumber());
        if (dir < 0 || dir > 359) {
            errors.add(new ParseError(
                    ctx.getStart().getLine(),
                    ctx.getStart().getCharPositionInLine(),
                    String.format("Wind direction must be in range 0-359 (got %.0f).", dir),
                    ctx.windDirection().getText()
            ));
        }
    }

    @Override
    public void exitCoordinate(final FlightPlanDslParser.CoordinateContext ctx) {
        final double lat = parseSignedNumber(ctx.signedNumber(0));
        final double lon = parseSignedNumber(ctx.signedNumber(1));
        if (lat < -90 || lat > 90) {
            errors.add(new ParseError(
                    ctx.getStart().getLine(),
                    ctx.getStart().getCharPositionInLine(),
                    String.format("Latitude must be in range -90..90 (got %.4f).", lat),
                    ctx.signedNumber(0).getText()
            ));
        }
        if (lon < -180 || lon > 180) {
            errors.add(new ParseError(
                    ctx.getStart().getLine(),
                    ctx.getStart().getCharPositionInLine(),
                    String.format("Longitude must be in range -180..180 (got %.4f).", lon),
                    ctx.signedNumber(1).getText()
            ));
        }
    }

    private double parseSignedNumber(final FlightPlanDslParser.SignedNumberContext ctx) {
        final String sign = ctx.PLUS() != null ? "+" : ctx.MINUS() != null ? "-" : "";
        return Double.parseDouble(sign + ctx.NUMBER().getText());
    }
}
