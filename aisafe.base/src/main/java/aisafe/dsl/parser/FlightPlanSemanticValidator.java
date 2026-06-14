package aisafe.dsl.parser;

import aisafe.dsl.ast.CoordinateAst;
import aisafe.dsl.ast.FlightPlanAst;
import aisafe.dsl.ast.LegAst;
import aisafe.dsl.ast.SegmentAst;
import aisafe.dsl.ast.SourcePosition;
import aisafe.dsl.parser.FlightPlanParserFacade.ParseError;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Performs semantic validation on a parsed FlightPlanAst.
 * All errors are collected and returned - execution does not stop at the first error.
 */
public final class FlightPlanSemanticValidator {

    public List<ParseError> validate(final FlightPlanAst plan) {
        final List<ParseError> errors = new ArrayList<>();

        validateLegs(plan, errors);
        validateNoAirportVisitedTwice(plan, errors);

        return errors;
    }

    private void validateLegs(final FlightPlanAst plan, final List<ParseError> errors) {
        final List<LegAst> legs = plan.legs();

        for (int i = 0; i < legs.size(); i++) {
            final LegAst leg = legs.get(i);
            final int legNumber = i + 1;

            if (leg.fuel() != null && leg.fuel().amount() <= 0) {
                errors.add(errorAt(leg.fuel().position(),
                        String.format("Leg %d: fuel quantity must be strictly positive (got %.2f).",
                                legNumber, leg.fuel().amount()),
                        "<fuel>"));
            }

            if (leg.segments().isEmpty()) {
                errors.add(errorAt(leg.position(),
                        String.format("Leg %d: must contain at least one segment.", legNumber),
                        "<segment>"));
            }

            for (int s = 0; s < leg.segments().size(); s++) {
                validateSegment(leg.segments().get(s), legNumber, s + 1, errors);
            }

            validateDateTime(leg.departure().date(), leg.departure().time(),
                    "Leg " + legNumber + " departure", leg.departure().position(), errors);
            validateDateTime(leg.arrival().date(), leg.arrival().time(),
                    "Leg " + legNumber + " arrival", leg.arrival().position(), errors);

            try {
                final LocalDate depDate = LocalDate.parse(leg.departure().date());
                final LocalTime depTime = LocalTime.parse(leg.departure().time());
                final LocalDate arrDate = LocalDate.parse(leg.arrival().date());
                final LocalTime arrTime = LocalTime.parse(leg.arrival().time());

                final boolean depBeforeArr = depDate.isBefore(arrDate)
                        || (depDate.isEqual(arrDate) && depTime.isBefore(arrTime));

                if (!depBeforeArr) {
                    errors.add(errorAt(leg.departure().position(),
                            String.format("Leg %d: departure (%s %s) must be before arrival (%s %s).",
                                    legNumber, leg.departure().date(), leg.departure().time(),
                                    leg.arrival().date(), leg.arrival().time()),
                            "<datetime>"));
                }
            } catch (final DateTimeParseException ignored) {
                // already reported by validateDateTime
            }

            validateSegmentContinuity(leg, legNumber, errors);
        }

        for (int i = 0; i < legs.size() - 1; i++) {
            final LegAst current = legs.get(i);
            final LegAst next = legs.get(i + 1);
            final int legNumber = i + 1;

            final String arrivalAirport = current.route().toAirportCode();
            final String nextDepartureAirport = next.route().fromAirportCode();

            if (!arrivalAirport.equalsIgnoreCase(nextDepartureAirport)) {
                errors.add(errorAt(next.route().position(),
                        String.format("Leg %d arrival airport (%s) must match leg %d departure airport (%s).",
                                legNumber, arrivalAirport, legNumber + 1, nextDepartureAirport),
                        "<airport>"));
            }

            try {
                final LocalDate arrDate = LocalDate.parse(current.arrival().date());
                final LocalTime arrTime = LocalTime.parse(current.arrival().time());
                final LocalDate depDate = LocalDate.parse(next.departure().date());
                final LocalTime depTime = LocalTime.parse(next.departure().time());

                final boolean timeOk = arrDate.isBefore(depDate)
                        || (arrDate.isEqual(depDate) && arrTime.isBefore(depTime));

                if (!timeOk) {
                    errors.add(errorAt(next.departure().position(),
                            String.format("Leg %d arrival time (%s %s) must precede leg %d departure time (%s %s).",
                                    legNumber, current.arrival().date(), current.arrival().time(),
                                    legNumber + 1, next.departure().date(), next.departure().time()),
                            "<datetime>"));
                }
            } catch (final DateTimeParseException ignored) {
                // already reported by validateDateTime
            }
        }
    }

    private void validateSegment(final SegmentAst seg, final int legNumber, final int segNumber,
                                 final List<ParseError> errors) {
        if (seg.from().latitude() == seg.to().latitude()
                && seg.from().longitude() == seg.to().longitude()) {
            errors.add(errorAt(seg.position(),
                    String.format("Leg %d, segment %d: start and end coordinates must be different.",
                            legNumber, segNumber),
                    "<coordinate>"));
        }

        if (seg.altitudeMeters() <= 0) {
            errors.add(errorAt(seg.position(),
                    String.format("Leg %d, segment %d: altitude must be positive (got %.2f).",
                            legNumber, segNumber, seg.altitudeMeters()),
                    "<altitude>"));
        }

        if (seg.widthMeters() <= 0) {
            errors.add(errorAt(seg.position(),
                    String.format("Leg %d, segment %d: width must be positive (got %.2f).",
                            legNumber, segNumber, seg.widthMeters()),
                    "<width>"));
        }

        if (seg.windSpeed() < 0) {
            errors.add(errorAt(seg.position(),
                    String.format("Leg %d, segment %d: wind speed cannot be negative (got %.2f).",
                            legNumber, segNumber, seg.windSpeed()),
                    "<windSpeed>"));
        }
    }

    private void validateSegmentContinuity(final LegAst leg, final int legNumber,
                                           final List<ParseError> errors) {
        final List<SegmentAst> segs = leg.segments();
        for (int i = 0; i < segs.size() - 1; i++) {
            final CoordinateAst end = segs.get(i).to();
            final CoordinateAst start = segs.get(i + 1).from();
            if (end.latitude() != start.latitude() || end.longitude() != start.longitude()) {
                errors.add(errorAt(segs.get(i + 1).position(),
                        String.format("Leg %d: end of segment %d (%.4f, %.4f) does not match start of segment %d (%.4f, %.4f).",
                                legNumber, i + 1, end.latitude(), end.longitude(),
                                i + 2, start.latitude(), start.longitude()),
                        "<coordinate>"));
            }
        }
    }

    private void validateNoAirportVisitedTwice(final FlightPlanAst plan, final List<ParseError> errors) {
        final Set<String> visited = new HashSet<>();

        for (final LegAst leg : plan.legs()) {
            final String dep = leg.route().fromAirportCode().toUpperCase();

            if (!visited.add(dep)) {
                errors.add(errorAt(leg.route().position(),
                        String.format("Airport %s is visited more than once in the flight plan.", dep),
                        "<airport>"));
            }
        }

        if (!plan.legs().isEmpty()) {
            final LegAst lastLeg = plan.legs().get(plan.legs().size() - 1);
            final String lastArrival = lastLeg.route().toAirportCode().toUpperCase();
            if (!visited.add(lastArrival)) {
                errors.add(errorAt(lastLeg.arrival().position(),
                        String.format("Airport %s is visited more than once in the flight plan.", lastArrival),
                        "<airport>"));
            }
        }
    }

    private void validateDateTime(final String date, final String time,
                                  final String context, final SourcePosition position,
                                  final List<ParseError> errors) {
        try {
            LocalDate.parse(date);
        } catch (final DateTimeParseException e) {
            errors.add(errorAt(position,
                    String.format("%s: invalid date value '%s'.", context, date),
                    "<date>"));
        }

        try {
            LocalTime.parse(time);
        } catch (final DateTimeParseException e) {
            errors.add(errorAt(position,
                    String.format("%s: invalid time value '%s'.", context, time),
                    "<time>"));
        }
    }

    private ParseError errorAt(final SourcePosition position, final String message, final String symbol) {
        final SourcePosition safePosition = position == null ? SourcePosition.unknown() : position;
        return new ParseError(safePosition.line(), safePosition.column(), message, symbol);
    }
}
