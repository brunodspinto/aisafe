package aisafe.dsl.parser;

import aisafe.dsl.ast.FlightPlanAst;
import aisafe.dsl.ast.LegAst;
import aisafe.dsl.ast.SegmentAst;
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
 * All errors are collected and returned — execution does not stop at the first error.
 */
public final class FlightPlanSemanticValidator {

    public List<ParseError> validate(final FlightPlanAst plan) {
        final List<ParseError> errors = new ArrayList<>();

        validateLegs(plan, errors);
        validateRouteCoherence(plan, errors);
        validateNoAirportVisitedTwice(plan, errors);

        return errors;
    }

    // ── Leg-level rules ──────────────────────────────────────────────────────

    private void validateLegs(final FlightPlanAst plan, final List<ParseError> errors) {
        final List<LegAst> legs = plan.legs();

        for (int i = 0; i < legs.size(); i++) {
            final LegAst leg = legs.get(i);
            final int legNumber = i + 1;

            // Fuel must be strictly positive
            if (leg.fuel().amount() <= 0) {
                errors.add(new ParseError(0, 0,
                        String.format("Leg %d: fuel quantity must be strictly positive (got %.2f).",
                                legNumber, leg.fuel().amount()),
                        "<fuel>"));
            }

            // At least one segment
            if (leg.segments().isEmpty()) {
                errors.add(new ParseError(0, 0,
                        String.format("Leg %d: must contain at least one segment.", legNumber),
                        "<segment>"));
            }

            // Validate each segment
            for (int s = 0; s < leg.segments().size(); s++) {
                validateSegment(leg.segments().get(s), legNumber, s + 1, errors);
            }

            // Validate date/time values
            validateDateTime(leg.departure().date(), leg.departure().time(), "Leg " + legNumber + " departure", errors);
            validateDateTime(leg.arrival().date(), leg.arrival().time(), "Leg " + legNumber + " arrival", errors);
        }

        // Leg sequence coherence: arrival of leg N must match departure of leg N+1
        for (int i = 0; i < legs.size() - 1; i++) {
            final LegAst current = legs.get(i);
            final LegAst next = legs.get(i + 1);
            final int legNumber = i + 1;

            final String arrivalAirport = current.arrival().airportCode();
            final String nextDepartureAirport = next.departure().airportCode();

            if (!arrivalAirport.equalsIgnoreCase(nextDepartureAirport)) {
                errors.add(new ParseError(0, 0,
                        String.format("Leg %d arrival airport (%s) must match leg %d departure airport (%s).",
                                legNumber, arrivalAirport, legNumber + 1, nextDepartureAirport),
                        "<airport>"));
            }

            // Arrival time of leg N must precede departure time of leg N+1
            try {
                final LocalDate arrDate = LocalDate.parse(current.arrival().date());
                final LocalTime arrTime = LocalTime.parse(current.arrival().time());
                final LocalDate depDate = LocalDate.parse(next.departure().date());
                final LocalTime depTime = LocalTime.parse(next.departure().time());

                final boolean timeOk = arrDate.isBefore(depDate)
                        || (arrDate.isEqual(depDate) && arrTime.isBefore(depTime));

                if (!timeOk) {
                    errors.add(new ParseError(0, 0,
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

    // ── Segment-level rules ──────────────────────────────────────────────────

    private void validateSegment(final SegmentAst seg, final int legNumber, final int segNumber,
                                 final List<ParseError> errors) {
        // Start and end coordinates must be different
        if (seg.from().latitude() == seg.to().latitude()
                && seg.from().longitude() == seg.to().longitude()) {
            errors.add(new ParseError(0, 0,
                    String.format("Leg %d, segment %d: start and end coordinates must be different.",
                            legNumber, segNumber),
                    "<coordinate>"));
        }

        // Numeric values must be positive
        if (seg.altitudeMeters() <= 0) {
            errors.add(new ParseError(0, 0,
                    String.format("Leg %d, segment %d: altitude must be positive (got %.2f).",
                            legNumber, segNumber, seg.altitudeMeters()),
                    "<altitude>"));
        }

        if (seg.widthMeters() <= 0) {
            errors.add(new ParseError(0, 0,
                    String.format("Leg %d, segment %d: width must be positive (got %.2f).",
                            legNumber, segNumber, seg.widthMeters()),
                    "<width>"));
        }

        if (seg.windSpeed() < 0) {
            errors.add(new ParseError(0, 0,
                    String.format("Leg %d, segment %d: wind speed cannot be negative (got %.2f).",
                            legNumber, segNumber, seg.windSpeed()),
                    "<windSpeed>"));
        }
    }

    // ── Route coherence ──────────────────────────────────────────────────────

    private void validateRouteCoherence(final FlightPlanAst plan, final List<ParseError> errors) {
        if (plan.legs().isEmpty()) return;

        final LegAst firstLeg = plan.legs().get(0);
        final LegAst lastLeg = plan.legs().get(plan.legs().size() - 1);

        // Route origin must match first leg departure
        final String routeOrigin = firstLeg.route().fromAirportCode();
        final String firstDeparture = firstLeg.departure().airportCode();
        if (!routeOrigin.equalsIgnoreCase(firstDeparture)) {
            errors.add(new ParseError(0, 0,
                    String.format("Route origin (%s) must match first leg departure airport (%s).",
                            routeOrigin, firstDeparture),
                    "<route>"));
        }

        // Route destination must match last leg arrival
        final String routeDestination = lastLeg.route().toAirportCode();
        final String lastArrival = lastLeg.arrival().airportCode();
        if (!routeDestination.equalsIgnoreCase(lastArrival)) {
            errors.add(new ParseError(0, 0,
                    String.format("Route destination (%s) must match last leg arrival airport (%s).",
                            routeDestination, lastArrival),
                    "<route>"));
        }
    }

    // ── No airport visited twice ─────────────────────────────────────────────

    private void validateNoAirportVisitedTwice(final FlightPlanAst plan, final List<ParseError> errors) {
        final Set<String> visited = new HashSet<>();

        for (int i = 0; i < plan.legs().size(); i++) {
            final LegAst leg = plan.legs().get(i);
            final String dep = leg.departure().airportCode().toUpperCase();

            if (!visited.add(dep)) {
                errors.add(new ParseError(0, 0,
                        String.format("Airport %s is visited more than once in the flight plan.", dep),
                        "<airport>"));
            }
        }

        // Also check the final arrival
        if (!plan.legs().isEmpty()) {
            final String lastArrival = plan.legs().get(plan.legs().size() - 1)
                    .arrival().airportCode().toUpperCase();
            if (!visited.add(lastArrival)) {
                errors.add(new ParseError(0, 0,
                        String.format("Airport %s is visited more than once in the flight plan.", lastArrival),
                        "<airport>"));
            }
        }
    }

    // ── Date/time format validation ──────────────────────────────────────────

    private void validateDateTime(final String date, final String time,
                                  final String context, final List<ParseError> errors) {
        try {
            LocalDate.parse(date);
        } catch (final DateTimeParseException e) {
            errors.add(new ParseError(0, 0,
                    String.format("%s: invalid date value '%s'.", context, date),
                    "<date>"));
        }

        try {
            LocalTime.parse(time);
        } catch (final DateTimeParseException e) {
            errors.add(new ParseError(0, 0,
                    String.format("%s: invalid time value '%s'.", context, time),
                    "<time>"));
        }
    }
}