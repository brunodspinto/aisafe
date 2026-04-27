package aisafe.dsl.ast;

import java.util.List;

public record LegAst(
        EndpointAst departure,
        EndpointAst arrival,
        RouteAst route,
        List<SegmentAst> segments,
        FuelAst fuel
) {
}
