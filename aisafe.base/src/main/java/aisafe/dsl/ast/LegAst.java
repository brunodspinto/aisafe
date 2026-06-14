package aisafe.dsl.ast;

import java.util.List;

public record LegAst(
        EndpointAst departure,
        EndpointAst arrival,
        RouteAst route,
        List<SegmentAst> segments,
        FuelAst fuel,
        SourcePosition position
) {

    public LegAst(final EndpointAst departure, final EndpointAst arrival, final RouteAst route,
                  final List<SegmentAst> segments, final FuelAst fuel) {
        this(departure, arrival, route, segments, fuel, SourcePosition.unknown());
    }
}
