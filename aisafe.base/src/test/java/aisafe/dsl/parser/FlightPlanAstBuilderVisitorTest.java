package aisafe.dsl.parser;

import aisafe.dsl.ast.SegmentAst;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightPlanAstBuilderVisitorTest {

    private static final String DSL_TEMPLATE =
            "FLIGHT %s TYPE REGULAR {\n" +
            "  LEG {\n" +
            "    DEPARTURE: 2026-06-01 10:00;\n" +
            "    ARRIVAL: 2026-06-01 10:45;\n" +
            "    ROUTE: OPO -> LIS;\n" +
            "    SEGMENT {\n" +
            "      START: (+41.15, -8.61);\n" +
            "      END: (+38.72, -9.14);\n" +
            "      ALTITUDE: %s WIDTH: %s;\n" +
            "      WIND: (180, 12 M/S);\n" +
            "    }\n" +
            "    FUEL: 5300 KG;\n" +
            "  }\n" +
            "}\n";

    private SegmentAst parseFirstSegment(final String id,
                                         final String altitude,
                                         final String width) {
        final String dsl = String.format(DSL_TEMPLATE, id, altitude, width);
        final FlightPlanParserFacade.ParseResult result =
                new FlightPlanParserFacade().parse(dsl);
        assertTrue(result.isValid(), "Expected valid: " + result.errors());
        return result.ast().orElseThrow().legs().get(0).segments().get(0);
    }

    @Test
    void ensureFtAltitudeIsConvertedToMeters() {
        final SegmentAst seg = parseFirstSegment("TP001", "35000 FT", "1000 M");
        assertEquals(35000 * 0.3048, seg.altitudeMeters(), 0.01);
    }

    @Test
    void ensureKmWidthIsConvertedToMeters() {
        final SegmentAst seg = parseFirstSegment("TP002", "10000 M", "5 KM");
        assertEquals(5000.0, seg.widthMeters(), 0.01);
    }

    @Test
    void ensureFtWidthIsConvertedToMeters() {
        final SegmentAst seg = parseFirstSegment("TP003", "10000 M", "3000 FT");
        assertEquals(3000 * 0.3048, seg.widthMeters(), 0.01);
    }

    @Test
    void ensureMeterValuesAreUnchanged() {
        final SegmentAst seg = parseFirstSegment("TP004", "10000 M", "2000 M");
        assertEquals(10000.0, seg.altitudeMeters(), 0.01);
        assertEquals(2000.0, seg.widthMeters(), 0.01);
    }

    @Test
    void ensureKmAltitudeIsConvertedToMeters() {
        final SegmentAst seg = parseFirstSegment("TP005", "10 KM", "2000 M");
        assertEquals(10000.0, seg.altitudeMeters(), 0.01);
    }
}
