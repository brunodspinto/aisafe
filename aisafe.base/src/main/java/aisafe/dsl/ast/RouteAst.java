package aisafe.dsl.ast;

public record RouteAst(String fromAirportCode, String toAirportCode, SourcePosition position) {

    public RouteAst(final String fromAirportCode, final String toAirportCode) {
        this(fromAirportCode, toAirportCode, SourcePosition.unknown());
    }
}
