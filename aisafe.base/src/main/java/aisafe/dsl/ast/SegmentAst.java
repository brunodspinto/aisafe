package aisafe.dsl.ast;

public record SegmentAst(
        CoordinateAst from,
        CoordinateAst to,
        double altitudeMeters,
        double widthMeters,
        double windSpeed,
        double windDirection,
        SourcePosition position
) {

    public SegmentAst(final CoordinateAst from, final CoordinateAst to, final double altitudeMeters,
                      final double widthMeters, final double windSpeed, final double windDirection) {
        this(from, to, altitudeMeters, widthMeters, windSpeed, windDirection, SourcePosition.unknown());
    }
}
