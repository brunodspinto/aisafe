package aisafe.dsl.ast;

public record SegmentAst(
        CoordinateAst from,
        CoordinateAst to,
        double altitudeMeters,
        double widthMeters,
        double windSpeed,
        double windDirection
) {
}
