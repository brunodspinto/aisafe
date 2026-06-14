package aisafe.dsl.ast;

public record CoordinateAst(double latitude, double longitude, SourcePosition position) {

    public CoordinateAst(final double latitude, final double longitude) {
        this(latitude, longitude, SourcePosition.unknown());
    }
}
