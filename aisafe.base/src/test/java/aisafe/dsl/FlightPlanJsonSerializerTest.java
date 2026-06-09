package aisafe.dsl;

import aisafe.dsl.ast.CoordinateAst;
import aisafe.dsl.ast.FlightPlanAst;
import aisafe.dsl.ast.FlightType;
import aisafe.dsl.ast.LegAst;
import aisafe.dsl.ast.SegmentAst;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FlightPlanJsonSerializer}.
 * Verifies that the serialiser produces a JSON file in the format expected by the
 * {@code flight_tester} C binary.
 */
class FlightPlanJsonSerializerTest {

    // ---- helpers ----

    private static FlightPlanAst singleLegAst(final String identifier) {
        final SegmentAst seg = new SegmentAst(
                new CoordinateAst(41.15, -8.61),
                new CoordinateAst(38.72, -9.14),
                10000.0, 2000.0, 12.0, 180.0);
        final LegAst leg = new LegAst(null, null, null, List.of(seg), null);
        return new FlightPlanAst(identifier, FlightType.REGULAR, List.of(leg));
    }

    private static FlightPlanAst twoLegAst(final String identifier) {
        final SegmentAst seg1 = new SegmentAst(
                new CoordinateAst(41.15, -8.61),
                new CoordinateAst(38.72, -9.14),
                10000.0, 2000.0, 12.0, 180.0);
        final SegmentAst seg2 = new SegmentAst(
                new CoordinateAst(38.72, -9.14),
                new CoordinateAst(40.41, -3.68),
                9000.0, 2000.0, 10.0, 200.0);
        final LegAst leg1 = new LegAst(null, null, null, List.of(seg1), null);
        final LegAst leg2 = new LegAst(null, null, null, List.of(seg2), null);
        return new FlightPlanAst(identifier, FlightType.REGULAR, List.of(leg1, leg2));
    }

    // ---- tests ----

    @Test
    void ensureSingleLegPlanProducesNonEmptyFile() throws Exception {
        final FlightPlanAst ast = singleLegAst("TP001");
        final Path tmp = new FlightPlanJsonSerializer().toTempFile(ast);
        try {
            assertTrue(Files.size(tmp) > 0, "temp file must not be empty");
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void ensureOutputIsValidJsonArray() throws Exception {
        final FlightPlanAst ast = singleLegAst("TP001");
        final Path tmp = new FlightPlanJsonSerializer().toTempFile(ast);
        try {
            final String json = Files.readString(tmp).trim();
            assertTrue(json.startsWith("["), "root must be a JSON array");
            assertTrue(json.endsWith("]"), "root must be a JSON array");
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void ensureIdentifierAndFlightTypeArePresent() throws Exception {
        final FlightPlanAst ast = singleLegAst("TP001");
        final Path tmp = new FlightPlanJsonSerializer().toTempFile(ast);
        try {
            final String json = Files.readString(tmp);
            assertTrue(json.contains("\"TP001\""), "identifier must appear in JSON");
            assertTrue(json.contains("\"REGULAR\"") || json.contains("\"CHARTER\""),
                    "flight_type must appear in JSON");
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void ensureSegmentCoordinatesArePresent() throws Exception {
        final FlightPlanAst ast = singleLegAst("TP001");
        final Path tmp = new FlightPlanJsonSerializer().toTempFile(ast);
        try {
            final String json = Files.readString(tmp);
            assertTrue(json.contains("start_coord"), "start_coord must appear in JSON");
            assertTrue(json.contains("end_coord"), "end_coord must appear in JSON");
            assertTrue(json.contains("altitude_m"), "altitude_m must appear in JSON");
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void ensureMultiLegPlanSerializesAllLegs() throws Exception {
        final FlightPlanAst ast = twoLegAst("TP002");
        final Path tmp = new FlightPlanJsonSerializer().toTempFile(ast);
        try {
            final String json = Files.readString(tmp);
            // Both legs contribute segments; start_coord should appear at least twice
            int count = 0;
            int idx = 0;
            while ((idx = json.indexOf("start_coord", idx)) >= 0) {
                count++;
                idx++;
            }
            assertTrue(count >= 2, "multi-leg plan must serialise all segments");
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void ensureTempFileCanBeDeletedByCallerAfterUse() throws Exception {
        final FlightPlanAst ast = singleLegAst("TP003");
        final Path tmp = new FlightPlanJsonSerializer().toTempFile(ast);
        assertTrue(Files.exists(tmp), "temp file must exist after creation");
        Files.delete(tmp);
        assertFalse(Files.exists(tmp), "temp file must be deletable by caller");
    }
}
