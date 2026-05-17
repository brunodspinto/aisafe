package aisafe.dsl.parser;

import aisafe.dsl.ast.*;
import aisafe.dsl.parser.FlightPlanParserFacade.ParseError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link FlightPlanSemanticValidator}.
 * Verifies semantic validation rules applied to parsed flight-plan ASTs.
 */
class FlightPlanSemanticValidatorTest {

    private FlightPlanSemanticValidator validator;

    @BeforeEach
    void setUp() {
        validator = new FlightPlanSemanticValidator();
    }

    private SegmentAst validSegment() {
        return new SegmentAst(
                new CoordinateAst(38.7, -9.1),
                new CoordinateAst(41.1, -8.6),
                10000.0, 50.0, 10.0, 270.0
        );
    }

    private LegAst validLeg(final String depAirport, final String arrAirport) {
        return new LegAst(
                new EndpointAst(depAirport, "2026-06-01", "08:00"),
                new EndpointAst(arrAirport, "2026-06-01", "10:00"),
                new RouteAst(depAirport, arrAirport),
                List.of(validSegment()),
                new FuelAst(5000.0, "KG")
        );
    }

    private FlightPlanAst validPlan() {
        return new FlightPlanAst("TP1234", FlightType.REGULAR, List.of(validLeg("LIS", "OPO")));
    }

    @Test
    void ensureValidPlanProducesNoErrors() {
        assertTrue(validator.validate(validPlan()).isEmpty());
    }

    @Test
    void ensureNegativeFuelProducesError() {
        final LegAst leg = new LegAst(
                new EndpointAst("LIS", "2026-06-01", "08:00"),
                new EndpointAst("OPO", "2026-06-01", "10:00"),
                new RouteAst("LIS", "OPO"),
                List.of(validSegment()),
                new FuelAst(-100.0, "KG")
        );
        final List<ParseError> errors = validator.validate(
                new FlightPlanAst("TP1234", FlightType.REGULAR, List.of(leg)));
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.message().contains("fuel")));
    }
        
    @Test
    void ensureSegmentWithSameStartAndEndProducesError() {
        final SegmentAst badSegment = new SegmentAst(
                new CoordinateAst(38.7, -9.1),
                new CoordinateAst(38.7, -9.1),
                10000.0, 50.0, 10.0, 270.0
        );
        final LegAst leg = new LegAst(
                new EndpointAst("LIS", "2026-06-01", "08:00"),
                new EndpointAst("OPO", "2026-06-01", "10:00"),
                new RouteAst("LIS", "OPO"),
                List.of(badSegment),
                new FuelAst(5000.0, "KG")
        );
        final List<ParseError> errors = validator.validate(
                new FlightPlanAst("TP1234", FlightType.REGULAR, List.of(leg)));
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.message().contains("coordinates")));
    }

    @Test
    void ensureLegArrivalMustMatchNextLegDeparture() {
        final LegAst leg1 = validLeg("LIS", "OPO");
        final LegAst leg2 = validLeg("FAO", "MAD");
        final List<ParseError> errors = validator.validate(
                new FlightPlanAst("TP1234", FlightType.REGULAR, List.of(leg1, leg2)));
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.message().contains("arrival airport")));
    }

    @Test
    void ensureLegArrivalTimeMustPrecedeNextLegDepartureTime() {
        final LegAst leg1 = new LegAst(
                new EndpointAst("LIS", "2026-06-01", "08:00"),
                new EndpointAst("OPO", "2026-06-01", "10:00"),
                new RouteAst("LIS", "OPO"),
                List.of(validSegment()),
                new FuelAst(5000.0, "KG")
        );
        final LegAst leg2 = new LegAst(
                new EndpointAst("OPO", "2026-06-01", "09:00"),
                new EndpointAst("MAD", "2026-06-01", "11:00"),
                new RouteAst("OPO", "MAD"),
                List.of(validSegment()),
                new FuelAst(5000.0, "KG")
        );
        final List<ParseError> errors = validator.validate(
                new FlightPlanAst("TP1234", FlightType.REGULAR, List.of(leg1, leg2)));
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.message().contains("precede")));
    }

    @Test
    void ensureRouteOriginMustMatchFirstLegDeparture() {
        final LegAst leg = new LegAst(
                new EndpointAst("LIS", "2026-06-01", "08:00"),
                new EndpointAst("OPO", "2026-06-01", "10:00"),
                new RouteAst("FAO", "OPO"),
                List.of(validSegment()),
                new FuelAst(5000.0, "KG")
        );
        final List<ParseError> errors = validator.validate(
                new FlightPlanAst("TP1234", FlightType.REGULAR, List.of(leg)));
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.message().contains("Route origin")));
    }

    @Test
    void ensureRouteDestinationMustMatchLastLegArrival() {
        final LegAst leg = new LegAst(
                new EndpointAst("LIS", "2026-06-01", "08:00"),
                new EndpointAst("OPO", "2026-06-01", "10:00"),
                new RouteAst("LIS", "MAD"),
                List.of(validSegment()),
                new FuelAst(5000.0, "KG")
        );
        final List<ParseError> errors = validator.validate(
                new FlightPlanAst("TP1234", FlightType.REGULAR, List.of(leg)));
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.message().contains("Route destination")));
    }

    @Test
    void ensureAirportCannotBeVisitedTwice() {
        final LegAst leg1 = validLeg("LIS", "OPO");
        final LegAst leg2 = new LegAst(
                new EndpointAst("OPO", "2026-06-01", "11:00"),
                new EndpointAst("LIS", "2026-06-01", "13:00"),
                new RouteAst("OPO", "LIS"),
                List.of(validSegment()),
                new FuelAst(5000.0, "KG")
        );
        final List<ParseError> errors = validator.validate(
                new FlightPlanAst("TP1234", FlightType.REGULAR, List.of(leg1, leg2)));
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.message().contains("visited more than once")));
    }

    @Test
    void ensureAllErrorsAreCollectedInSingleExecution() {
        final SegmentAst badSegment = new SegmentAst(
                new CoordinateAst(38.7, -9.1),
                new CoordinateAst(38.7, -9.1),
                -100.0, -50.0, 10.0, 270.0
        );
        final LegAst leg = new LegAst(
                new EndpointAst("LIS", "2026-06-01", "08:00"),
                new EndpointAst("OPO", "2026-06-01", "10:00"),
                new RouteAst("LIS", "OPO"),
                List.of(badSegment),
                new FuelAst(-100.0, "KG")
        );
        final List<ParseError> errors = validator.validate(
                new FlightPlanAst("TP1234", FlightType.REGULAR, List.of(leg)));
        assertTrue(errors.size() >= 3);
    }

    @Test
    void ensureZeroAltitudeProducesError() {
        final SegmentAst badSegment = new SegmentAst(
                new CoordinateAst(38.7, -9.1),
                new CoordinateAst(41.1, -8.6),
                0.0, 50.0, 10.0, 270.0
        );
        final LegAst leg = new LegAst(
                new EndpointAst("LIS", "2026-06-01", "08:00"),
                new EndpointAst("OPO", "2026-06-01", "10:00"),
                new RouteAst("LIS", "OPO"),
                List.of(badSegment),
                new FuelAst(5000.0, "KG")
        );
        final List<ParseError> errors = validator.validate(
                new FlightPlanAst("TP1234", FlightType.REGULAR, List.of(leg)));
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.message().contains("altitude")));
    }

    @Test
    void ensureZeroWidthProducesError() {
        final SegmentAst badSegment = new SegmentAst(
                new CoordinateAst(38.7, -9.1),
                new CoordinateAst(41.1, -8.6),
                10000.0, 0.0, 10.0, 270.0
        );
        final LegAst leg = new LegAst(
                new EndpointAst("LIS", "2026-06-01", "08:00"),
                new EndpointAst("OPO", "2026-06-01", "10:00"),
                new RouteAst("LIS", "OPO"),
                List.of(badSegment),
                new FuelAst(5000.0, "KG")
        );
        final List<ParseError> errors = validator.validate(
                new FlightPlanAst("TP1234", FlightType.REGULAR, List.of(leg)));
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.message().contains("width")));
    }

    @Test
    void ensureNegativeWindSpeedProducesError() {
        final SegmentAst badSegment = new SegmentAst(
                new CoordinateAst(38.7, -9.1),
                new CoordinateAst(41.1, -8.6),
                10000.0, 50.0, -5.0, 270.0
        );
        final LegAst leg = new LegAst(
                new EndpointAst("LIS", "2026-06-01", "08:00"),
                new EndpointAst("OPO", "2026-06-01", "10:00"),
                new RouteAst("LIS", "OPO"),
                List.of(badSegment),
                new FuelAst(5000.0, "KG")
        );
        final List<ParseError> errors = validator.validate(
                new FlightPlanAst("TP1234", FlightType.REGULAR, List.of(leg)));
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.message().contains("wind speed")));
    }

    @Test
    void ensureInvalidDateProducesError() {
        final LegAst leg = new LegAst(
                new EndpointAst("LIS", "2026-13-01", "08:00"),
                new EndpointAst("OPO", "2026-06-01", "10:00"),
                new RouteAst("LIS", "OPO"),
                List.of(validSegment()),
                new FuelAst(5000.0, "KG")
        );
        final List<ParseError> errors = validator.validate(
                new FlightPlanAst("TP1234", FlightType.REGULAR, List.of(leg)));
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.message().contains("date")));
    }

    @Test
    void ensureLegWithNoSegmentsProducesError() {
        final LegAst leg = new LegAst(
                new EndpointAst("LIS", "2026-06-01", "08:00"),
                new EndpointAst("OPO", "2026-06-01", "10:00"),
                new RouteAst("LIS", "OPO"),
                List.of(),
                new FuelAst(5000.0, "KG")
        );
        final List<ParseError> errors = validator.validate(
                new FlightPlanAst("TP1234", FlightType.REGULAR, List.of(leg)));
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.message().contains("segment")));
    }
}