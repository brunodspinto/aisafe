package aisafe.dsl.parser;

import aisafe.dsl.ast.CoordinateAst;
import aisafe.dsl.ast.EndpointAst;
import aisafe.dsl.ast.FlightPlanAst;
import aisafe.dsl.ast.FlightType;
import aisafe.dsl.ast.FuelAst;
import aisafe.dsl.ast.LegAst;
import aisafe.dsl.ast.RouteAst;
import aisafe.dsl.ast.SegmentAst;
import aisafe.dsl.generated.FlightPlanDslBaseVisitor;
import aisafe.dsl.generated.FlightPlanDslParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Visitor that traverses the ANTLR4 parse tree and constructs the flight-plan AST.
 * Each visitXxx method returns a typed AST node; callers cast to the expected type.
 */
public final class FlightPlanAstBuilderVisitor extends FlightPlanDslBaseVisitor<Object> {

    @Override
    public FlightPlanAst visitFlight(final FlightPlanDslParser.FlightContext ctx) {
        final List<LegAst> legs = new ArrayList<>();
        for (final FlightPlanDslParser.LegContext legCtx : ctx.leg()) {
            legs.add((LegAst) visitLeg(legCtx));
        }
        return new FlightPlanAst(
                ctx.IDENTIFIER().getText(),
                FlightType.valueOf(ctx.flightType().getText().toUpperCase()),
                List.copyOf(legs)
        );
    }

    @Override
    public LegAst visitLeg(final FlightPlanDslParser.LegContext ctx) {
        final List<SegmentAst> segments = new ArrayList<>();
        for (final FlightPlanDslParser.SegmentContext segCtx : ctx.segment()) {
            segments.add((SegmentAst) visitSegment(segCtx));
        }
        return new LegAst(
                (EndpointAst) visitDeparture(ctx.departure()),
                (EndpointAst) visitArrival(ctx.arrival()),
                (RouteAst) visitRoute(ctx.route()),
                List.copyOf(segments),
                (FuelAst) visitFuel(ctx.fuel())
        );
    }

    private static double convertToMeters(final double value, final String unit) {
        return switch (unit.toUpperCase()) {
            case "FT" -> value * 0.3048;
            case "KM" -> value * 1000.0;
            default   -> value; // M — no conversion
        };
    }

    @Override
    public SegmentAst visitSegment(final FlightPlanDslParser.SegmentContext ctx) {
        final FlightPlanDslParser.AltitudeSlotContext altSlot = ctx.altitudeSlot(0);
        final double altitude = convertToMeters(
                parseSignedNumber(altSlot.altitude().signedNumber()),
                altSlot.altitude().distanceUnit().getText());
        final double width = convertToMeters(
                parseSignedNumber(altSlot.distance().signedNumber()),
                altSlot.distance().distanceUnit().getText());
        final FlightPlanDslParser.WindDeclContext windDecl = ctx.windDecl();
        final double windDirection = parseSignedNumber(windDecl.windDirection().signedNumber());
        final double windSpeed = parseSignedNumber(windDecl.windSpeed().signedNumber());
        return new SegmentAst(
                (CoordinateAst) visitCoordinate(ctx.coordinate(0)),
                (CoordinateAst) visitCoordinate(ctx.coordinate(1)),
                altitude,
                width,
                windSpeed,
                windDirection
        );
    }

    @Override
    public EndpointAst visitDeparture(final FlightPlanDslParser.DepartureContext ctx) {
        return new EndpointAst(
                ctx.dateTime().DATE().getText(),
                ctx.dateTime().TIME().getText()
        );
    }

    @Override
    public EndpointAst visitArrival(final FlightPlanDslParser.ArrivalContext ctx) {
        return new EndpointAst(
                ctx.dateTime().DATE().getText(),
                ctx.dateTime().TIME().getText()
        );
    }

    @Override
    public RouteAst visitRoute(final FlightPlanDslParser.RouteContext ctx) {
        return new RouteAst(ctx.airportCode(0).getText(), ctx.airportCode(1).getText());
    }

    @Override
    public FuelAst visitFuel(final FlightPlanDslParser.FuelContext ctx) {
        return new FuelAst(
                parseSignedNumber(ctx.signedNumber()),
                ctx.fuelUnit().getText().toUpperCase()
        );
    }

    @Override
    public CoordinateAst visitCoordinate(final FlightPlanDslParser.CoordinateContext ctx) {
        return new CoordinateAst(
                parseSignedNumber(ctx.signedNumber(0)),
                parseSignedNumber(ctx.signedNumber(1))
        );
    }

    private double parseSignedNumber(final FlightPlanDslParser.SignedNumberContext ctx) {
        final String sign = ctx.PLUS() != null ? "+" : ctx.MINUS() != null ? "-" : "";
        return Double.parseDouble(sign + ctx.NUMBER().getText());
    }
}
